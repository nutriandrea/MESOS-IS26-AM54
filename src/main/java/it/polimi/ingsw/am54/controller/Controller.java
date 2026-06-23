package it.polimi.ingsw.am54.controller;

import it.polimi.ingsw.am54.database.DatabaseConfig;
import it.polimi.ingsw.am54.database.DatabaseManager;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import it.polimi.ingsw.am54.network.server.Server;
import it.polimi.ingsw.am54.network.server.VirtualView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Core server-side controller managing a single game instance.
 *
 * Responsibilities:
 * - Intercepts requests from clients (via Socket or RMI handlers).
 * - Forwards actions to the Game model in a thread-safe (synchronized) manner.
 * - Manages the player lifecycle (joins, disconnections, reconnections).
 * - Handles game persistence, state saving, and automated endgame scoring.
 */
public class Controller {
    private final Game game;
    private Server server;
    private final it.polimi.ingsw.am54.network.server.PersistenceManager persistenceManager;
    private volatile boolean scoresSavedToDatabase = false;

    // --- FINAL RESULT SNAPSHOT ---
    // Captured the instant the game ends so a player who reconnects AFTER the game
    // is over can still be shown the winner board. We cannot recompute it on
    // reconnect: a default win has a winner that determineWinners() would not pick,
    // and winners may have already left the game (removePlayerEntirely).
    private volatile List<String> finalWinners = null;
    private volatile Map<String, Integer> finalScores = null;
    private volatile int finalNumPlayers = 0;

    /** Maps a player's nickname to their respective VirtualView (Game Observer). */
    private final Map<String, VirtualView> virtualViews = new ConcurrentHashMap<>();

    /** Tracks nicknames of players who have temporarily lost connection. */
    private final Set<String> disconnectedPlayers = ConcurrentHashMap.newKeySet();

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    /** Database manager for saving match results. */
    private final DatabaseManager dbManager;

    /** Flag set to true when the game is suspended due to insufficient active players. */
    private volatile boolean suspended = false;

    /** Timeout after which the sole active player wins by default (in seconds). */
    private static final long SOLO_TIMEOUT_SECONDS = 60;

    /** Scheduler for the suspension timeout. */
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    /** Reference to the active timeout task (to allow cancellation upon resumption). */
    private ScheduledFuture<?> timeoutFuture;

    public Controller(Game game) {
        this.game = game;
        this.dbManager = new it.polimi.ingsw.am54.database.DatabaseManager(new it.polimi.ingsw.am54.database.DatabaseConfig());
        this.dbManager.init();
        this.persistenceManager = new it.polimi.ingsw.am54.network.server.PersistenceManager("backup.json");

        if (game.getPlayers() != null && !game.getPlayers().isEmpty()) {
            for (it.polimi.ingsw.am54.model.Player p : game.getPlayers()) {
                disconnectedPlayers.add(p.getNickname());
            }
        }
    }

    /**
     * Sets the reference to the Server instance.
     * Required to allow the Controller to call saveState() on the PersistenceManager.
     *
     * @param server The Server instance.
     */
    public void setServer(Server server) {
        this.server = server;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    /**
     * Serializes the current game state to a local JSON file.
     * Invoked automatically after any state-altering game action.
     */
    /**
     * Persiste lo stato corrente della partita su file. A partita conclusa cancella il
     * backup (così un riavvio non recupera una partita finita), altrimenti la salva.
     */
    private void saveState() {
        if (game.getCurrentState().isGameOver()) {
            persistenceManager.clearBackup();
            return;
        }
        persistenceManager.saveGame(game);
    }

    @FunctionalInterface
    private interface GameAction {
        void execute() throws Exception;
    }

    // --- CONNECTION LIFECYCLE (JOIN / RECONNECT) ---

    /**
     * Gestisce l'ingresso di un client. Distingue tre casi: riconnessione di un
     * giocatore caduto (delegata a {@link #handleReconnect}), nickname duplicato
     * (rifiutato con errore) e ingresso standard (crea la VirtualView, la registra come
     * observer del modello e inoltra il join al Game).
     *
     * @param nickname   il nickname del giocatore
     * @param connection la connessione (Socket o RMI) del client
     */
    public void handleJoinGame(String nickname, ClientConnection connection) {
        synchronized (game.getPlayers()) {
            // Case 1: Player is recovering from a temporary crash.
            if (disconnectedPlayers.contains(nickname)) {
                handleReconnect(nickname, connection);
                return;
            }

            // Case 2: Rejected entry due to duplicate nickname.
            if (virtualViews.containsKey(nickname)) {
                VirtualView rejectedView = new VirtualView(connection, nickname);
                rejectedView.showError("Nickname already in use! Please choose another.");
                return;
            }

            // Case 3: Standard join procedure.
            VirtualView playerView = new VirtualView(connection, nickname);
            virtualViews.put(nickname, playerView);
            game.addObserver(playerView);

            // Broadcast current lobby state to the new player.
            for (Player p : game.getPlayers()) {
                playerView.onPlayerJoined(game, p);
                if (p.getTotemColors() != null) {
                    playerView.onTotemColorChosen(game, p, p.getTotemColors());
                }
            }

            try {
                game.joinGame(nickname);
            } catch (Exception e) {
                virtualViews.remove(nickname);
                game.removeObserver(playerView);
                playerView.showError(e.getMessage());
                return;
            }
        }
        connection.startHeartbeat();
    }

    /**
     * Ripristina un giocatore che si riconnette entro la finestra utile: lo toglie dai
     * disconnessi, sostituisce la sua VirtualView con quella della nuova connessione, gli
     * reinvia lo snapshot completo dello stato (e il turno corrente), notifica gli altri
     * del rientro e, se la partita era sospesa con ora ≥2 attivi, la fa riprendere. Se la
     * partita è già finita, gli replica il risultato finale.
     *
     * @param nickname   il giocatore che si riconnette
     * @param connection la nuova connessione del client
     */
    public void handleReconnect(String nickname, ClientConnection connection) {
        synchronized (game.getPlayers()) {
            disconnectedPlayers.remove(nickname);

            VirtualView oldView = virtualViews.get(nickname);
            if (oldView != null) {
                game.removeObserver(oldView);
            }
            VirtualView newView = new VirtualView(connection, nickname);
            virtualViews.put(nickname, newView);
            game.addObserver(newView);
            LOGGER.info("Player " + nickname + " has reconnected.");

            notifyAllClientsStateChanged();

            // CRITICAL FIX: We must explicitly inform the reconnected player whose turn it is.
            // Otherwise, a freshly restarted client will have 'null' as the current player and deadlock.
            if (game.getCurrentPlayer() != null) {
                newView.onTurnChanged(game, game.getCurrentPlayer());
            }

            // If the game is ALREADY over, replay the final result to the reconnecting
            // player: onStateChanged carries the EndGame state but not the winners, and
            // notifyEndGame was a one-shot they missed while disconnected.
            if (game.getCurrentState().isGameOver() && finalWinners != null) {
                connection.notifyEndGame(finalWinners, finalScores, finalNumPlayers);
            }
        }

        connection.startHeartbeat();
        broadcastPlayerReconnected(nickname);

        if (suspended && activePlayerCount() >= 2) {
            resumeGame();
        }
    }

    /**
     * Dopo un ripristino da crash, riallinea il giocatore di turno chiedendo allo stato
     * corrente chi deve agire ({@code recoverActivePlayer}) e logga la fase recuperata.
     */
    public void validateGameStateAfterRecovery() {
        synchronized (game.getPlayers()) {
            // Restore the turn based on the current phase.
            it.polimi.ingsw.am54.model.Player recovered = game.getCurrentState().recoverActivePlayer(game);
            if (recovered != null) {
                game.setCurrentPlayer(recovered);
            }

            // Console diagnostic logging for recovery tracking.
            it.polimi.ingsw.am54.model.Player current = game.getCurrentPlayer();
            if (current != null) {
                System.out.println("[RECOVERY] Active phase: " + game.getCurrentState().getClass().getSimpleName());
                System.out.println("[RECOVERY] Turn dynamically assigned to: " + current.getNickname());
            }
        }
    }

    // --- GAME COMMAND EXECUTORS ---

    /**
     * Avvia la partita su richiesta dell'host e salva lo stato.
     *
     * @param nickname il giocatore che avvia la partita
     */
    public void handleSetupGame(String nickname) {
        executeCommand(nickname, () -> {
            game.setupGame();
            saveState();
        });
    }

    /**
     * Assegna il colore del totem scelto da un giocatore e salva lo stato.
     *
     * @param nickname il giocatore che sceglie
     * @param color    il colore scelto
     */
    public void handleChooseTotemColor(String nickname, String color) {
        executeCommand(nickname, () -> {
            game.chooseTotemColor(nickname, color);
            saveState();
        });
    }

    /**
     * Piazza il totem di un giocatore su una tessera offerta e salva lo stato.
     *
     * @param nickname il giocatore che piazza
     * @param position l'indice della tessera offerta
     */
    public void handlePlaceTotem(String nickname, int position) {
        executeCommand(nickname, () -> {
            game.placeTotem(nickname, position);
            saveState();
        });
    }

    /**
     * Esegue la pesca di una carta, risolve le eventuali fasi automatiche (eventi,
     * fine partita) e salva lo stato.
     *
     * @param nickname il giocatore che pesca
     * @param row      la fila ("upper" o "lower")
     * @param index    l'indice della carta
     */
    public void handlePickCard(String nickname, String row, int index) {
        executeCommand(nickname, () -> {
            game.pickCard(nickname, row, index);
            checkAndResolveAutomaticPhases();
            saveState();
        });
    }

    /**
     * Termina il turno di un giocatore, risolve le eventuali fasi automatiche e salva lo stato.
     *
     * @param nickname il giocatore che termina il turno
     */
    public void handleEndTurn(String nickname) {
        executeCommand(nickname, () -> {
            game.endTurn(nickname);
            checkAndResolveAutomaticPhases();
            saveState();
        });
    }

    // --- DISCONNECTION & SUSPENSION MANAGEMENT ---

    /**
     * Handles the definitive removal of a client. During the setup phase, this frees
     * up the slot. During active gameplay, it translates into a temporary suspension.
     */
    public void removeClient(String nickname) {
        VirtualView view = virtualViews.remove(nickname);

        // Game already finished: the final standings are frozen. A player closing the
        // winner board must NOT be erased from the other players' final ranking, and
        // there is nothing left to suspend. Just detach their view so the server stops
        // pushing to a dead connection; leave the model (players, scores) untouched.
        // The backup is cleared only once the very last viewer has left.
        if (game.getCurrentState().isGameOver()) {
            if (view != null) game.removeObserver(view);
            disconnectedPlayers.remove(nickname);
            if (virtualViews.isEmpty()) {
                persistenceManager.clearBackup();
            }
            return;
        }

        if (game.getCurrentState().isRemovePermanent()) {
            LOGGER.info("Player " + nickname + " abandoned the lobby permanently. Freeing slot and Totem.");

            game.removePlayerEntirely(nickname);
            disconnectedPlayers.remove(nickname);

            if (view != null) game.removeObserver(view);

            if (game.getPlayers().isEmpty()) {
                persistenceManager.clearBackup();
            } else {
                notifyAllClientsStateChanged();
            }
        } else {
            // --- TEMPORARY DISCONNECTION LOGIC (In-game crash) ---
            disconnectedPlayers.add(nickname);
            if (view != null) {
                synchronized (game.getPlayers()) {
                    game.removeObserver(view);
                }
            }
            LOGGER.info("Player " + nickname + " connection dropped during active gameplay.");
            broadcastPlayerDisconnected(nickname);
            autoHandleDisconnection(nickname);
        }
    }

    private void notifyAllClientsStateChanged() {
        for (VirtualView view : virtualViews.values()) {
            view.onStateChanged(game, game.getCurrentState());
        }
    }

    public int getPlayerCount() {
        return game.getPlayersNumber();
    }

    // --- PRIVATE UTILITIES ---

    private void broadcastPlayerReconnected(String reconnectedNickname) {
        for (VirtualView vv : virtualViews.values()) {
            vv.showPlayerReconnected(reconnectedNickname);
        }
    }

    private void broadcastPlayerDisconnected(String disconnectedNickname) {
        for (VirtualView vv : virtualViews.values()) {
            vv.showPlayerDisconnected(disconnectedNickname);
        }
    }

    // --- GAME SUSPENSION LOGIC ---

    /**
     * Returns the current number of actively connected players.
     */
    private int activePlayerCount() {
        return game.getPlayersNumber() - disconnectedPlayers.size();
    }

    private void broadcastGameSuspended() {
        for (VirtualView vv : virtualViews.values()) {
            vv.showGameSuspended();
        }
    }

    private void broadcastGameResumed() {
        for (VirtualView vv : virtualViews.values()) {
            vv.showGameResumed();
        }
    }

    /**
     * Automatically skips the disconnected player's turn if applicable,
     * then evaluates if the game needs to be suspended (1 active player left).
     */
    /**
     * Automatically skips the disconnected player's turn if applicable,
     * then evaluates if the game needs to be suspended (1 active player left).
     */
    private void autoHandleDisconnection(String nickname) {
        synchronized (game.getPlayers()) {
            game.setActivePlayerCount(activePlayerCount());

            // Apply safety blocks (e.g., animations or critical event resolutions)
            if (game.getCurrentState().isDisconnectBlocked()) return;

            // CRITICAL FIX: We must process the skipTurn FIRST, even if only 1 player remains.
            // This ensures that when the game eventually resumes, the turn correctly belongs to Player B.
            try {
                Player p = game.getPlayerByNickname(nickname);
                if (game.getCurrentPlayer() != null && game.getCurrentPlayer().equals(p)) {
                    game.getCurrentState().skipTurn(game, nickname);
                    game.getCurrentState().handleDisconnection(game, p, true);
                    checkAndResolveAutomaticPhases();
                } else {
                    game.getCurrentState().handleDisconnection(game, p, false);
                }
            } catch (Exception e) {
                LOGGER.warning("Turn skip failed for " + nickname + ": " + e.getMessage());
            }

            // ONLY AFTER ensuring the turn has passed, we check if the game should be suspended.
            checkSuspension();
        }
    }

    /**
     * Evaluates whether the game should be paused (if 1 player remains)
     * or completely purged from memory and disk (if 0 players remain).
     */
    private void checkSuspension() {
        // --- EMPTY ROOM CHECK ---
        if (activePlayerCount() == 0) {
            LOGGER.warning("[SERVER] Room is entirely empty. Aborting match and purging state...");

            // 1. Physically delete the save file.
            persistenceManager.clearBackup();

            // 2. Stop the default win timer if it was running.
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
            suspended = false;

            // 3. Clear memory and return the server to the Lobby state.
            game.getPlayers().clear();
            disconnectedPlayers.clear();
            virtualViews.clear();
            game.setState(new it.polimi.ingsw.am54.model.states.SetUpGameState());

            // 4. Extreme cleanup: Reset the current player and board for the next match.
            game.setCurrentPlayer(null);
            if (game.getBoard() != null) {
                game.getBoard().getTurnOrderTile().clear();
                game.getBoard().getOfferingTrack().clear();
                try {
                    game.getBoard().getCharacterUpperRow().clear();
                    game.getBoard().getCharacterLowerRow().clear();
                    game.getBoard().getEventUpperRow().clear();
                    game.getBoard().getEventLowerRow().clear();
                    game.getBoard().getBuildingCardsUpperRow().clear();
                    game.getBoard().getBuildingCardsLowerRow().clear();
                } catch (Exception ignored) {}
            }
            return;
        }

        // --- SOLO PLAYER CHECK (Default Win Logic) ---
        if (suspended) return;
        if (activePlayerCount() == 1) {
            suspendGame();
        }
    }

    /**
     * Pauses gameplay and initializes a countdown. If the countdown expires
     * before another player reconnects, the sole remaining player wins by default.
     */
    private void suspendGame() {
        if (suspended) return;
        suspended = true;
        LOGGER.warning("Only 1 active player remains: game suspended for " + SOLO_TIMEOUT_SECONDS + " seconds.");
        broadcastGameSuspended();

        timeoutFuture = timeoutScheduler.schedule(() -> {
            synchronized (game.getPlayers()) {
                if (!suspended) return;
                Player winner = findSoloActivePlayer();

                if (winner != null) {
                    LOGGER.info("Suspension timeout expired: " + winner.getNickname() + " wins by default!");

                    // 1. Set the model to the EndGame state.
                    game.setState(new it.polimi.ingsw.am54.model.states.EndGameState());

                    // 2. Special resolution for a default win.
                    try {
                        // Snapshot BEFORE saving/notifying, while all players are still present,
                        // so a late reconnection can be shown the winner board.
                        captureFinalResult(Collections.singletonList(winner));

                        if (!scoresSavedToDatabase) {
                            scoresSavedToDatabase = true;
                            int finalScore = game.calculatePlayerScore(winner);
                            int numPlayers = game.getPlayers().size();
                            dbManager.saveMatchResult(winner.getNickname(), finalScore, numPlayers);
                            LOGGER.info("[DATABASE] Default win score saved successfully.");
                        }

                        // Let the Model notify the end of the game.
                        game.notifyEndGame(Collections.singletonList(winner));

                        // The game is over: drop the on-disk backup so a restart does not
                        // "recover" a finished match back into mid-game.
                        persistenceManager.clearBackup();

                    } catch (Exception e) {
                        LOGGER.severe("Error during default win resolution: " + e.getMessage());
                    }
                }
                suspended = false;
            }
        }, SOLO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Resumes the game once a second player reconnects.
     */
    private void resumeGame() {
        if (!suspended) return;
        suspended = false;
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
        LOGGER.info("Game resumed: multiple active players detected.");
        broadcastGameResumed();
    }

    private Player findSoloActivePlayer() {
        return game.getPlayers().stream()
                .filter(p -> !disconnectedPlayers.contains(p.getNickname()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gestisce la richiesta di classifica: recupera dal database le partite con quel
     * numero di giocatori e la posizione del richiedente, poi invia il risultato al client.
     *
     * @param connection la connessione del client richiedente
     * @param nickname   il nickname del richiedente (per calcolarne la posizione)
     * @param numPlayers il filtro sul numero di giocatori
     */
    public void handleGetLeaderboard(ClientConnection connection, String nickname, int numPlayers) {
        // Spec: leaderboard of matches with THIS many players + the requesting
        // player's own rank within that same leaderboard.
        List<it.polimi.ingsw.am54.network.messages.LeaderboardEntry> entries = dbManager.getLeaderboard(numPlayers);
        int playerPosition = dbManager.getPlayerPosition(nickname, numPlayers);
        LOGGER.info("Leaderboard request (" + numPlayers + " players): " + entries.size()
                + " entries; " + nickname + " at position " + playerPosition);
        connection.sendLeaderboardUpdate(new LeaderboardUpdate(playerPosition, entries, numPlayers));
    }

    /**
     * Congela il risultato finale (nickname dei vincitori e punteggio di ogni giocatore)
     * nel momento in cui la partita finisce, così da poterlo ritrasmettere a chi si
     * riconnette più tardi.
     *
     * @param winners la lista dei vincitori
     */
    private void captureFinalResult(List<Player> winners) {
        List<String> names = new ArrayList<>();
        if (winners != null) {
            for (Player w : winners) names.add(w.getNickname());
        }
        Map<String, Integer> scores = new HashMap<>();
        for (Player p : game.getPlayers()) {
            scores.put(p.getNickname(), game.calculatePlayerScore(p));
        }
        this.finalWinners = names;
        this.finalScores = scores;
        this.finalNumPlayers = game.getPlayers().size();
    }

    /**
     * Esegue un'azione di gioco serializzandola sul monitor {@code game.getPlayers()}
     * (così non si sovrappone a join/disconnessioni di altri thread). Se la partita è
     * sospesa rifiuta l'azione; eventuali eccezioni del modello vengono trasformate in
     * un messaggio di errore inviato solo al giocatore.
     *
     * @param nickname il giocatore che esegue l'azione
     * @param action   l'azione di gioco da eseguire
     */
    private void executeCommand(String nickname, GameAction action) {
        if (suspended) {
            VirtualView view = virtualViews.get(nickname);
            if (view != null) {
                view.showError("Game suspended: awaiting player reconnections...");
            }
            return;
        }
        // Serialize on the same monitor used by join/reconnect/disconnect handlers so
        // a game action can never run concurrently with a (dis)connection mutating the
        // same model from another client thread.
        synchronized (game.getPlayers()) {
            game.setActivePlayerCount(activePlayerCount());
            try {
                action.execute();
            } catch (Exception e) {
                VirtualView view = virtualViews.get(nickname);
                if (view != null) {
                    view.showError(e.getMessage());
                }
            }
        }
    }

    /**
     * Checks if the current state requires automated server-side resolution
     * (e.g., Event phase processing) and triggers it.
     */
    private void checkAndResolveAutomaticPhases() throws Exception {
        if (game.getCurrentState().isAutoResolvable() && !game.getCurrentState().isGameOver()) {
            game.getCurrentState().resolvePhase(game);
            saveState();
        }

        if (game.getCurrentState().isGameOver() && !scoresSavedToDatabase) {
            scoresSavedToDatabase = true;
            int numPlayers = game.getPlayers().size();

            // EndGameState is BOTH auto-resolvable and game-over, so the guard above
            // deliberately skips it. Resolve it here, exactly once (the flag protects us
            // from re-entry): this plays out the final events, computes each player's
            // end-of-game building aura and, crucially, fires notifyEndGame(...) so every
            // connected client receives the REAL final scores and the correct winner(s).
            // Without this the clients never got an EndGameUpdate and could only sort the
            // raw round-10 aura — showing the wrong winner and the wrong points.
            if (game.getCurrentState().isAutoResolvable()) {
                game.getCurrentState().resolvePhase(game);
                saveState();
            }

            // Snapshot the result NOW, while every player is still present, so a late
            // reconnection can be shown the winner board.
            captureFinalResult(game.determineWinners());

            new Thread(() -> {
                try {
                    for (Player p : game.getPlayers()) {
                        int finalScore = game.calculatePlayerScore(p);
                        dbManager.saveMatchResult(p.getNickname(), finalScore, numPlayers);
                    }
                    LOGGER.info("[DATABASE] Final match scores saved successfully!");
                } catch (Exception e) {
                    LOGGER.warning("[DATABASE] Failed to save final scores: " + e.getMessage());
                }
            }).start();
            return;
        }

        if (game.getCurrentPlayer() != null
                && disconnectedPlayers.contains(game.getCurrentPlayer().getNickname())) {
            autoHandleDisconnection(game.getCurrentPlayer().getNickname());
        }
    }
}