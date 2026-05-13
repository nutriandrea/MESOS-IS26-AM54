package it.polimi.ingsw.am54.controller;

import it.polimi.ingsw.am54.database.DatabaseManager;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import it.polimi.ingsw.am54.network.server.Server;
import it.polimi.ingsw.am54.network.server.VirtualView;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * ============================================================================
 * Controller — Coordinatore lato Server (UNA istanza per partita)
 * ============================================================================
 *
 * RESPONSABILITA':
 * - Ricevere le Request dai client (tramite SocketClientHandler/RMIClientHandler)
 * - Inoltrarle al Model (Game) in modo thread-safe (synchronized)
 * - Gestire il ciclo di vita dei giocatori (join, disconnect, reconnect)
 * - Inviare SOLO messaggi di sistema (disconnessione/riconnessione), MAI eventi di gioco
 *
 * DISCONNESSIONE VS RIMOZIONE:
 * - markDisconnected(): il ping/pong rileva che un client non risponde.
 *   Il giocatore viene marcato come disconnesso ma NON rimosso dal gioco.
 *   Gli altri giocatori ricevono PlayerDisconnectedUpdate.
 * - removeClient(): pulizia definitiva. Succede quando il socket si chiude
 * ============================================================================
 */
public class Controller {
    private final Game game;
    private Server server;

    /** Mappa nickname -> VirtualView. Ogni VirtualView e' observer del Game */
    private final Map<String, VirtualView> virtualViews = new ConcurrentHashMap<>();

    /** Set di nickname dei giocatori temporaneamente disconnessi */
    private final Set<String> disconnectedPlayers = ConcurrentHashMap.newKeySet();

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    /** Database manager for persisting match results (may be null if unavailable). */
    private DatabaseManager dbManager;

    /** Flag: true when the game is suspended (only 1 active player). */
    private boolean suspended = false;

    /** Seconds to wait for reconnection before declaring the solo player winner. */
    private static final int SOLO_TIMEOUT_SECONDS = 120;

    /** Scheduler for the solo-player timeout. */
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);

    /** The scheduled timeout task (cancel if someone reconnects). */
    private ScheduledFuture<?> timeoutFuture;

    public Controller(Game game) {
        this.game = game;
    }

    /**
     * Sets the database manager. Call before the game starts so that
     * match results can be persisted at the end of the game.
     */
    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Imposta il riferimento al Server.
     * Necessario per permettere al Controller di chiamare saveState()
     * sul PersistenceManager dopo ogni azione di gioco.
     *
     * @param server l'istanza del Server
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Salva lo stato corrente del gioco su file JSON.
     * Chiamato dal Controller dopo ogni azione di gioco
     * (setup, chooseTotemColor, placeTotem, pickCard, endTurn).
     * 
     * Salvataggio aggiuntivo oltre a quello periodico di 30s,
     * per garantire che le ultime azioni siano salvate subito.
     */
    private void saveState() {
        if (server != null) {
            server.saveGameState();
        }
    }

    @FunctionalInterface
    private interface GameAction {
        void execute() throws Exception;
    }

    /* =====================================================================
     * JOIN / RECONNECT
     * ===================================================================== */

    public void handleJoinGame(String nickname, ClientConnection connection) {
        synchronized (game.getPlayers()) {
            if (disconnectedPlayers.contains(nickname) || virtualViews.containsKey(nickname)) {
                handleReconnect(nickname, connection);
                return;
            }

            VirtualView playerView = new VirtualView(connection, nickname);
            virtualViews.put(nickname, playerView);
            game.addObserver(playerView);

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
                return;
            }
        }

        connection.startHeartbeat();
    }

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
            LOGGER.info("Giocatore " + nickname + " si e' riconnesso.");

            newView.sendFullStateOnReconnect(game);

            if (game.getCurrentPlayer() != null) {
                newView.onTurnChanged(game, game.getCurrentPlayer());
            }
        }

        connection.startHeartbeat();
        broadcastPlayerReconnected(nickname);

        // Se il gioco era sospeso e ora ci sono almeno 2 giocatori attivi, riprendi
        if (suspended && activePlayerCount() >= 2) {
            resumeGame();
        }
    }

    /* =====================================================================
     * GESTIONE COMANDI DI GIOCO
     * ===================================================================== */

    public void handleSetupGame(String nickname) {
        executeCommand(nickname, () -> {
            game.setupGame();
            // Salvataggio solo dopo endTurn o periodico
        });
    }

    public void handleChooseTotemColor(String nickname, String color) {
        executeCommand(nickname, () -> {
            game.chooseTotemColor(nickname, color);
            // Salvataggio solo dopo endTurn o periodico
        });
    }

    public void handlePlaceTotem(String nickname, int position) {
        executeCommand(nickname, () -> {
            game.placeTotem(nickname, position);
            // Salvataggio solo dopo endTurn o periodico
        });
    }

    public void handlePickCard(String nickname, String row, int index) {
        executeCommand(nickname, () -> {
            game.pickCard(nickname, row, index);
            checkAndResolveAutomaticPhases();
            // Salvataggio solo dopo endTurn o periodico
        });
    }

    public void handleEndTurn(String nickname) {
        executeCommand(nickname, () -> {
            game.endTurn(nickname);
            checkAndResolveAutomaticPhases();
            saveState();
        });
    }

    /* =====================================================================
     * DISCONNESSIONE
     * ===================================================================== */

    public void markDisconnected(String nickname) {
        disconnectedPlayers.add(nickname);
        LOGGER.warning("Giocatore " + nickname + " marcato come disconnesso.");
        broadcastPlayerDisconnected(nickname);
        autoHandleDisconnection(nickname);
    }

    public void removeClient(String nickname) {
        VirtualView view = virtualViews.remove(nickname);
        // Il giocatore resta in disconnectedPlayers per permettere la riconnessione.
        // Se non e' gia' presente (es. connessione chiusa senza ping timeout), lo aggiungiamo.
        disconnectedPlayers.add(nickname);
        if (view != null) {
            synchronized (game.getPlayers()) {
                game.removeObserver(view);
            }
        }
        LOGGER.info("Giocatore " + nickname + " rimosso dal server.");
        autoHandleDisconnection(nickname);
    }

    public boolean isDisconnected(String nickname) {
        return disconnectedPlayers.contains(nickname);
    }

    public int getPlayerCount() {
        return game.getPlayersNumber();
    }

    /* =====================================================================
     * METODI PRIVATI
     * ===================================================================== */

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

    /* =====================================================================
     * GESTIONE SOSPENSIONE GIOCO
     * ===================================================================== */

    /**
     * Restituisce il numero di giocatori attualmente connessi (non disconnessi).
     */
    private int activePlayerCount() {
        return game.getPlayersNumber() - disconnectedPlayers.size();
    }

    /**
     * Invia a tutti i giocatori connessi la notifica di sospensione del gioco.
     */
    private void broadcastGameSuspended() {
        for (VirtualView vv : virtualViews.values()) {
            vv.showGameSuspended();
        }
    }

    /**
     * Invia a tutti i giocatori connessi la notifica di ripresa del gioco.
     */
    private void broadcastGameResumed() {
        for (VirtualView vv : virtualViews.values()) {
            vv.showGameResumed();
        }
    }

    /**
     * Salta automaticamente il turno del giocatore disconnesso se e' il suo turno,
     * poi verifica se sospendere il gioco (1 solo giocatore attivo).
     * Chiamato sia da markDisconnected() che da removeClient().
     *
     * LOGICA:
     * 1. Imposta sul Model il numero di giocatori attivi (esclusi i disconnessi)
     * 2. Se il disconnesso era il giocatore di turno, chiama skipTurn() dello stato corrente
     * 3. Se dopo lo skip siamo ancora in StartTurnState e tutti gli attivi hanno piazzato,
     *    forza la transizione a PickingState (perche' skipTurn non piazza totem)
     * 4. Se siamo passati a PickingState, imposta il primo picker
     * 5. Controlla se sospendere il gioco
     */
    private void autoHandleDisconnection(String nickname) {
        synchronized (game.getPlayers()) {
            if (game.getCurrentState() instanceof it.polimi.ingsw.am54.model.states.SetUpGameState) return;
            if (game.getCurrentState() instanceof it.polimi.ingsw.am54.model.states.EndGameState) return;

            // Informa il Model di quanti giocatori sono ancora attivi
            game.setActivePlayerCount(activePlayerCount());

            try {
                Player p = game.getPlayerByNickname(nickname);
                if (game.getCurrentPlayer() != null && game.getCurrentPlayer().equals(p)) {
                    game.getCurrentState().skipTurn(game, nickname);

                    // skipTurn in StartTurnState libera lo slot ma non piazza totem.
                    // Se tutti gli attivi hanno completato, dobbiamo forzare la transizione.
                    if (game.getCurrentState() instanceof it.polimi.ingsw.am54.model.states.StartTurnState) {
                        if (game.getBoard().allTotemsPlaced(activePlayerCount())) {
                            game.setState(new it.polimi.ingsw.am54.model.states.PickingState());
                            Player first = game.getBoard().getFirstPlayerFromOfferingTrack();
                            if (first != null) game.setCurrentPlayer(first);
                        }
                    }

                    checkAndResolveAutomaticPhases();
                }
            } catch (Exception e) {
                LOGGER.warning("Skip turn fallito per " + nickname + ": " + e.getMessage());
            }

            checkSuspension();
        }
    }

    /**
     * Se il numero di giocatori attivi e' sceso a 1, sospende il gioco.
     */
    private void checkSuspension() {
        if (suspended) return;
        if (activePlayerCount() <= 1) {
            suspendGame();
        }
    }

    /**
     * Sospende il gioco perche' e' rimasto un solo giocatore attivo.
     * Avvia un timeout: se nessun altro si riconnette, il giocatore superstite vince.
     */
    private void suspendGame() {
        if (suspended) return;
        suspended = true;
        LOGGER.warning("Solo 1 giocatore attivo: gioco sospeso per " + SOLO_TIMEOUT_SECONDS + " secondi.");
        broadcastGameSuspended();

        timeoutFuture = timeoutScheduler.schedule(() -> {
            synchronized (game.getPlayers()) {
                if (!suspended) return;
                Player winner = findSoloActivePlayer();
                if (winner != null) {
                    LOGGER.info("Timeout sospensione scaduto: " + winner.getNickname() + " vince la partita!");
                    game.notifyEndGame(Collections.singletonList(winner));
                }
                suspended = false;
            }
        }, SOLO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Riprende il gioco dopo che un secondo giocatore si e' riconnesso.
     */
    private void resumeGame() {
        if (!suspended) return;
        suspended = false;
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
        LOGGER.info("Gioco ripreso: almeno 2 giocatori attivi.");
        broadcastGameResumed();
    }

    /**
     * Trova il giocatore attualmente attivo (non disconnesso).
     * Usato dal timeout di sospensione per decretare il vincitore.
     */
    private Player findSoloActivePlayer() {
        return game.getPlayers().stream()
                .filter(p -> !disconnectedPlayers.contains(p.getNickname()))
                .findFirst()
                .orElse(null);
    }

    private void executeCommand(String nickname, GameAction action) {
        if (suspended) {
            VirtualView view = virtualViews.get(nickname);
            if (view != null) {
                view.showError("Gioco sospeso: in attesa di riconnessioni...");
            }
            return;
        }
        // Informa il Model del numero di giocatori attivi per ogni azione di gioco.
        // Necessario per isPickingComplete() e allTotemsPlaced() quando ci sono
        // giocatori disconnessi che non partecipano al round corrente.
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

    /**
     * Called after a game ends. Saves all player results to the database
     * and sends leaderboard data to every connected client.
     */
    private void onGameEnded() {
        if (dbManager == null || !dbManager.isAvailable()) return;

        int numPlayers = game.getPlayersNumber();
        Map<String, Integer> allScores = new LinkedHashMap<>();

        // Calculate and persist scores for every player
        for (Player p : game.getPlayers()) {
            int score = game.calculatePlayerScore(p);
            allScores.put(p.getNickname(), score);
            dbManager.saveMatchResult(p.getNickname(), score, numPlayers);
        }

        // Query the full leaderboard for this player-count bracket
        List<LeaderboardEntry> leaderboard = dbManager.getLeaderboard(numPlayers);

        // Send the leaderboard to each connected player
        synchronized (virtualViews) {
            for (Map.Entry<String, VirtualView> entry : virtualViews.entrySet()) {
                String nickname = entry.getKey();
                VirtualView vv = entry.getValue();

                int playerPos = -1;
                for (LeaderboardEntry le : leaderboard) {
                    if (le.getNickname().equalsIgnoreCase(nickname)) {
                        playerPos = le.getPosition();
                        break;
                    }
                }
                vv.sendLeaderboard(playerPos, leaderboard, numPlayers);
            }
        }
    }

    /**
     * Handles a client request to view the leaderboard for matches
     * with a specific number of players.
     */
    public void handleGetLeaderboard(String nickname, int numPlayers) {
        if (dbManager == null || !dbManager.isAvailable()) return;

        List<LeaderboardEntry> leaderboard = dbManager.getLeaderboard(numPlayers);

        int playerPos = -1;
        for (LeaderboardEntry le : leaderboard) {
            if (le.getNickname().equalsIgnoreCase(nickname)) {
                playerPos = le.getPosition();
                break;
            }
        }

        synchronized (virtualViews) {
            VirtualView vv = virtualViews.get(nickname);
            if (vv != null) {
                vv.sendLeaderboard(playerPos, leaderboard, numPlayers);
            }
        }
    }

    private void checkAndResolveAutomaticPhases() throws Exception {
        // Se siamo finiti nella fase Eventi, risolvila automaticamente
        if (game.getCurrentState() instanceof it.polimi.ingsw.am54.model.states.EventState) {
            game.getCurrentState().resolvePhase(game);
        }

        // Se dopo gli eventi (o in altri modi) siamo finiti nella fase finale, calcola i vincitori
        if (game.getCurrentState() instanceof it.polimi.ingsw.am54.model.states.EndGameState) {
            game.getCurrentState().resolvePhase(game);
            // Persist results and send leaderboard after the game ends
            onGameEnded();
        }
    }
}