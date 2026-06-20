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
 * ============================================================================
 * Controller — Server-side Coordinator (ONE instance per game)
 * ============================================================================
 *
 * <h2>Main responsibilities</h2>
 * <ul>
 *   <li><b>Request reception</b> — receives the {@code Request}s from clients
 *       via {@code SocketClientHandler} / {@code RMIClientHandler}</li>
 *   <li><b>Thread-safe forwarding to the Model</b> — every mutation of the {@link Game} occurs
 *       under the {@link #gameLock} lock (broader than a simple lock on getPlayers())</li>
 *   <li><b>Player lifecycle</b> — management of join, disconnection
 *       temporary, reconnection and permanent removal</li>
   * <li><b>System notifications</b> — sends ONLY system messages
   *       (disconnection/reconnection suspension), NEVER game events;
 *       game notifications are delegated to the observing {@link VirtualView}s</li>
   * <li><b>Game suspension</b> — when only one active player remains,
 *       a timeout of {@value #SOLO_TIMEOUT_SECONDS} seconds fires; if no one
 *       reconnects, the survivor wins by default</li>
 *   <li><b>Persistenza</b> — saves the state to a JSON file after every action
 *       and records scores in the database at game end</li>
 * </ul>
 *
 * <h2>Disconnection vs Removal</h2>
 * <dl>
 *   <dt>{@link #markDisconnected(String)}</dt>
 *   <dd>Ping/pong detects that a client is not responding. The player is marked
 *       as disconnected but NOT removed from the game. Other players receive
 *       {@code PlayerDisconnectedUpdate}.</dd>
 *   <dt>{@link #removeClient(String)}</dt>
 *   <dd>Permanent cleanup. Occurs when the socket closes or the player
 *       voluntarily leaves. If in the lobby phase, the slot is freed;
 *       if in a game, the player is only marked as disconnected.</dd>
 * </dl>
 *
 * <h2>Typical flow</h2>
 * <ol>
 *   <li>{@link #handleJoinGame(String, ClientConnection)} → join o reconnect</li>
 *   <li>{@link #handleSetupGame(String)} → game start (host)</li>
 *   <li>{@link #handleChooseTotemColor(String, String)} → totem color choice</li>
 *   <li>{@link #handlePlaceTotem(String, int)} → placement on the offering track</li>
 *   <li>{@link #handlePickCard(String, String, int)} → draft di una carta</li>
 *   <li>{@link #handleEndTurn(String)} → end turn</li>
 *   <li>{@link #markDisconnected(String)} / {@link #removeClient(String)} → uscita</li>
 * </ol>
 *
 * <h2>Thread safety</h2>
 * All methods that modify the {@link Game} acquire {@link #gameLock}.
 * Messages to clients go out outside the lock to avoid deadlocks.
 *
 * @author AM54
 * @see Game
 * @see VirtualView
 * @see Server
 */
public class Controller {
    /** Lock dedicated to all Game mutations — broader than game.getPlayers() */
    private final Object gameLock = new Object();
    private final Game game;
    private Server server;

    private final it.polimi.ingsw.am54.network.server.PersistenceManager persistenceManager;
    private boolean scoresSavedToDatabase = false;

    /** Map nickname -> VirtualView. Each VirtualView is observer of the Game */
    private final Map<String, VirtualView> virtualViews = new ConcurrentHashMap<>();

    /** Set of nicknames of temporarily disconnected players */
    private final Set<String> disconnectedPlayers = ConcurrentHashMap.newKeySet();

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    /** Database manager to save match results. */
    private final DatabaseManager dbManager;

    /** Flag: true when only one player is active and the game is suspended */
    private volatile boolean suspended = false;

    /** Timeout after which the remaining active player wins (in seconds) */
    private static final long SOLO_TIMEOUT_SECONDS = 60;

    /** Scheduler for the suspension timeout */
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    /** Reference to active timeout task (to cancel it if resumed) */
    private ScheduledFuture<?> timeoutFuture;

    /**
     * Constructs the Controller for a game.
     *
     * @param game the Model (Game) instance to control
     */
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
     * Sets the reference to the Server.
     * Necessary to allow the Controller to call saveState()
     * on the PersistenceManager after every game action.
     *
     * @param server the Server instance
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Returns the database manager for historical match access.
     *
     * @return the DatabaseManager associated with this Controller
     */
    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    /**
     * Saves the current game state to a JSON file.
     * Called by the Controller after every game action
     * (setup, chooseTotemColor, placeTotem, pickCard).
     * If the game has ended, deletes the backup instead of saving it.
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

    /* =====================================================================
     * JOIN / RECONNECT
     * ===================================================================== */

    /**
     * Handles a player's request to join the game.
     * <p>
     * Tre casi possibili:
     * <ol>
     *   <li><b>Reconnection</b> — if the nickname is in the disconnected set,
     *       delega a {@link #handleReconnect(String, ClientConnection)}</li>
     *   <li><b>Nickname already active</b> — if the name is already in {@link #virtualViews},
     *       sends error to the client and rejects the connection</li>
     *   <li><b>New player</b> — creates a {@link VirtualView}, registers it as
     *       observer of the {@link Game} and invokes {@link Game#joinGame(String)}</li>
     * </ol>
     * On success, also starts the client heartbeat.
     *
     * @param nickname  the nickname chosen by the player
     * @param connection the client connection (Socket or RMI)
     */
    public void handleJoinGame(String nickname, ClientConnection connection) {
        synchronized (gameLock) {
            // CASE 1: The player had disconnected and is returning
            if (disconnectedPlayers.contains(nickname)) {
                handleReconnect(nickname, connection);
                return;
            }

            // CASE 2: Someone is trying to enter with an ALREADY ACTIVE name on the server
            if (virtualViews.containsKey(nickname)) {
                // Create a temporary view ONLY to send back the error
                VirtualView rejectedView = new VirtualView(connection, nickname);
                rejectedView.showError("Nickname already in use! Choose another one.");
                return; // Blocchiamo l'entrata e usciamo dal metodo!
            }

            // CASE 3: The name is free, normal Join procedure
            VirtualView playerView = new VirtualView(connection, nickname);
            virtualViews.put(nickname, playerView);
            game.addObserver(playerView);

            // Sends the new player the state of who is already in the lobby
            for (Player p : game.getPlayers()) {
                playerView.onPlayerJoined(game, p);
                if (p.getTotemColors() != null) {
                    playerView.onTotemColorChosen(game, p, p.getTotemColors());
                }
            }

            try {
                // Try to add them to the actual game (which checks if there are too many players)
                game.joinGame(nickname);
            } catch (Exception e) {
                // If the game is full, removes the view and sends the error
                virtualViews.remove(nickname);
                game.removeObserver(playerView);
                playerView.showError(e.getMessage());
                return;
            }
        }

        connection.startHeartbeat();
    }

    /**
     * Handles the reconnection of a previously disconnected player.
     * <p>
     * Replaces the old {@link VirtualView} of the player with a new one,
     * reinserts the totem in the turn order if we are still in the
     * {@code StartTurnState}, sends the complete game state to the client
     * and notifies all other players.
     * <p>
     * If the game was suspended and now there are at least 2 active players,
     * automatically resumes the game, canceling the timeout.
     *
     * @param nickname  the nickname of the reconnecting player
     * @param connection the new client connection
     */
    public void handleReconnect(String nickname, ClientConnection connection) {
        synchronized (gameLock) {
            disconnectedPlayers.remove(nickname);

            VirtualView oldView = virtualViews.get(nickname);
            if (oldView != null) {
                game.removeObserver(oldView);
            }
            VirtualView newView = new VirtualView(connection, nickname);
            virtualViews.put(nickname, newView);
            game.addObserver(newView);
            LOGGER.info("Player " + nickname + " reconnected.");

            // =====================================================================
            // REINSERIMENTO TOTEM IN TURN ORDER
            // Returns to the first free position ONLY if we are in the Turn Order phase.
            // If we are in Picking, their totem will stay out until the next round!
            // =====================================================================
            String currentStateName = game.getCurrentState().getClass().getSimpleName();

            if (currentStateName.equals("StartTurnState")) {
                try {
                    Player reconnectedPlayer = game.getPlayerByNickname(nickname);
                    if (reconnectedPlayer != null && !reconnectedPlayer.hasPlacedTotem()) {
                        game.getBoard().addPlayerToFirstAvailableTurnOrderSlot(reconnectedPlayer);
                    }
                } catch (Exception e) {
                    LOGGER.warning("Unable to recover reconnected player for Turn Order: " + e.getMessage());
                }
            }

            newView.sendFullStateOnReconnect(game);

            if (game.getCurrentPlayer() != null) {
                newView.onTurnChanged(game, game.getCurrentPlayer());
            }
        }

        connection.startHeartbeat();
        broadcastPlayerReconnected(nickname);

        // If the game was suspended and now there are at least 2 active players, resume (cancel the 60 seconds)
        if (suspended && activePlayerCount() >= 2) {
            resumeGame();
        }
    }

    /**
     * Validates and restores the game state after a persistence recovery.
     * <p>
     * Retrieves the player whose turn it is by consulting the current
     * Model state and sets them as the current player.
     * Useful after a server restart with a game in progress.
     */
    public void validateGameStateAfterRecovery() {
        // Restore the turn based on the current game phase
        it.polimi.ingsw.am54.model.Player recovered = game.getCurrentState().recoverActivePlayer(game);
        if (recovered != null) {
            game.setCurrentPlayer(recovered);
        }

        // 2. Diagnostic log on the server console to confirm
        it.polimi.ingsw.am54.model.Player current = game.getCurrentPlayer();
        if (current != null) {
            System.out.println("[RECOVERY] Game phase: " + game.getCurrentState().getClass().getSimpleName());
            System.out.println("[RECOVERY] Server assigned turn to: " + current.getNickname());
        }
    }

    /* =====================================================================
     * GAME COMMAND HANDLING
     * ===================================================================== */

    /**
     * Starts the game (called by the host from the lobby).
     * Initializes the board, distributes initial resources and prepares the first round.
     *
     * @param nickname the nickname of the host starting the game
     */
    public void handleSetupGame(String nickname) {
        executeCommand(nickname, () -> {
            game.setupGame();
            saveState();
        });
    }

    /**
     * Handles the choice of totem color by a player.
     * The color is permanently associated with the player for the entire game.
     *
     * @param nickname the player's nickname
     * @param color    the chosen color (e.g. "RED", "BLUE")
     */
    public void handleChooseTotemColor(String nickname, String color) {
        executeCommand(nickname, () -> {
            game.chooseTotemColor(nickname, color);
            saveState();
        });
    }

    /**
     * Handles the placement of a player's totem on the offering track.
     *
     * @param nickname the player's nickname
     * @param position the index of the position on the offering track
     */
    public void handlePlaceTotem(String nickname, int position) {
        executeCommand(nickname, () -> {
            game.placeTotem(nickname, position);
            saveState();
        });
    }

    /**
     * Handles the drawing of a card from the market by a player.
     * After drawing, checks and automatically resolves the following phases
     * (es. eventi, fine round) if necessary.
     *
     * @param nickname the player's nickname
     * @param row      the market row ("upper" or "lower")
     * @param index    the position in the row
     */
    public void handlePickCard(String nickname, String row, int index) {
        executeCommand(nickname, () -> {
            game.pickCard(nickname, row, index);
            checkAndResolveAutomaticPhases();
            saveState();
        });
    }

    /**
     * Handles a player's end turn command.
     * After the turn ends, checks and automatically resolves
     * any subsequent phases (events, round change, etc.).
     *
     * @param nickname the player's nickname ending the turn
     */
    public void handleEndTurn(String nickname) {
        executeCommand(nickname, () -> {
            game.endTurn(nickname);
            checkAndResolveAutomaticPhases();
            saveState();
        });
    }

    /* =====================================================================
     * DISCONNECTION
     * ===================================================================== */

    /**
     * Marks a player as disconnected (detected by heartbeat ping/pong).
     * <p>
     * The player is added to the {@link #disconnectedPlayers} set but NOT removed
     * from the game, so they can reconnect. All other players receive
     * a disconnection notification. If the disconnected player had the turn,
     * it is automatically skipped.
     *
     * @param nickname the disconnected player's nickname
     */
    public void markDisconnected(String nickname) {
        disconnectedPlayers.add(nickname);
        LOGGER.warning("Player " + nickname + " marked as disconnected.");
        broadcastPlayerDisconnected(nickname);
        autoHandleDisconnection(nickname);
    }

    /**
     * Permanently removes a client from the game.
     * <p>
     * Due comportamenti distinti:
     * <ul>
     *   <li><b>In lobby (isRemovePermanent true)</b> — the player is removed
     *       completely from the Model, their totem color is freed and the spot made
     *       available. If the room stays empty, the backup is deleted.</li>
     *   <li><b>In game (isRemovePermanent false)</b> — the player is marked
     *       as temporarily disconnected, waiting for a possible reconnection.</li>
     * </ul>
     *
     * @param nickname the player's nickname to remove
     */
    public void removeClient(String nickname) {
        VirtualView view = virtualViews.remove(nickname);

        if (game.getCurrentState().isRemovePermanent()) {
            LOGGER.info("Player " + nickname + " leaves the lobby. Slot and Totem freed.");

            // 1. Removes the player from the model and frees the Totem color
            game.removePlayerEntirely(nickname);

            // Remove the nickname from the disconnected set, so they can re-enter
            disconnectedPlayers.remove(nickname);

            if (view != null) game.removeObserver(view);

            if (game.getPlayers().isEmpty()) {
                persistenceManager.clearBackup();
            } else {
                // =================================================================
                // FIX: Force notification to other clients.
                // Since ClientModel updates its internal list (and therefore index 0)
                // based on the new list received from the server, setGameReady is not needed.
                // The client will receive the updated list and handle the Host itself.
                // =================================================================
                notifyAllClientsStateChanged();
            }
        } else {
            // --- TEMPORARY DISCONNECTION LOGIC (In-game Crash) ---
            disconnectedPlayers.add(nickname);
            if (view != null) {
                synchronized (gameLock) {
                    game.removeObserver(view);
                }
            }
            LOGGER.info("Player " + nickname + " temporarily disconnected.");
            broadcastPlayerDisconnected(nickname);
            autoHandleDisconnection(nickname);
        }
    }

    private void notifyAllClientsStateChanged() {
        for (VirtualView view : virtualViews.values()) {
            // If the view is still valid, send an update
            view.onStateChanged(game, game.getCurrentState());
        }
    }

    /**
     * Checks if a player is currently marked as disconnected.
     *
     * @param nickname the player's nickname
     * @return {@code true} if the player is in the disconnected set
     */
    public boolean isDisconnected(String nickname) {
        return disconnectedPlayers.contains(nickname);
    }

    /**
     * Returns the number of players currently in the game.
     *
     * @return the total number of players (including any disconnected ones)
     */
    public int getPlayerCount() {
        return game.getPlayersNumber();
    }

    /* =====================================================================
     * PRIVATE METHODS
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
     * GAME SUSPENSION HANDLING
     * ===================================================================== */

    /**
     * Returns the number of currently connected players (not disconnected).
     */
    private int activePlayerCount() {
        return game.getPlayersNumber() - disconnectedPlayers.size();
    }

    /**
     * Sends the game suspension notification to all connected players.
     */
    private void broadcastGameSuspended() {
        for (VirtualView vv : virtualViews.values()) {
            vv.showGameSuspended();
        }
    }

    /**
     * Sends the game resume notification to all connected players.
     */
    private void broadcastGameResumed() {
        for (VirtualView vv : virtualViews.values()) {
            vv.showGameResumed();
        }
    }

    /**
     * Automatically skips the disconnected player's turn if it is their turn,
     * then checks whether to suspend the game (only 1 active player).
     */
    private void autoHandleDisconnection(String nickname) {
        synchronized (gameLock) {
            if (game.getCurrentState().isDisconnectBlocked()) return;

            game.setActivePlayerCount(activePlayerCount());

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
                LOGGER.warning("Skip turn failed for " + nickname + ": " + e.getMessage());
            }

            checkSuspension();
        }
    }

    /**
     * If the number of active players has dropped to 1, suspends the game.
     */
    /**
     * If the number of active players has dropped to 1, suspends the game.
     * If it drops to 0, resets the room and deletes the backup.
     */
    private void checkSuspension() {
        // ========================================================
        // NEW LOGIC: "Empty Room" check during an active game
        // ========================================================
        if (activePlayerCount() == 0) {
            LOGGER.warning("[SERVER] All players disconnected. Cancelling game...");

            // 1. Physically deletes the save
            persistenceManager.clearBackup();

            // 2. Stops the default victory timer (if it had started)
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
            suspended = false;

            // 3. Clears memory and brings the server back to Lobby!
            game.getPlayers().clear();
            disconnectedPlayers.clear();
            virtualViews.clear();
            game.setState(new it.polimi.ingsw.am54.model.states.SetUpGameState());
            return;
        }

        // ========================================================
        // Old logic for a single player (Default Victory)
        // ========================================================
        if (suspended) return;
        if (activePlayerCount() == 1) {
            suspendGame();
        }
    }

    /**
     * Suspends the game because only one active player is left.
     * Starts a timeout: if nobody reconnects, the remaining player wins.
     */
    private void suspendGame() {
        if (suspended) return;
        suspended = true;
        LOGGER.warning("Only 1 active player: game suspended for " + SOLO_TIMEOUT_SECONDS + " seconds.");
        broadcastGameSuspended();

        timeoutFuture = timeoutScheduler.schedule(() -> {
            synchronized (gameLock) {
                if (!suspended) return;
                Player winner = findSoloActivePlayer();

                if (winner != null) {
                    LOGGER.info("Suspension timeout expired: " + winner.getNickname() + " wins the game by default!");

                    // 1. Put the Model in end-game state
                    game.setState(new it.polimi.ingsw.am54.model.states.EndGameState());

                    // 2. Call a special resolution for the default victory
                    // Why don't we use resolvePhase()? Because resolvePhase() calculates scores
                    // and applies aura. Here we want the player to simply win.
                    try {
                        // Save the default winner in the DB (as you already did)
                        if (!scoresSavedToDatabase) {
                            scoresSavedToDatabase = true;
                            int finalScore = game.calculatePlayerScore(winner);
                            int numPlayers = game.getPlayers().size();
                            dbManager.saveMatchResult(winner.getNickname(), finalScore, numPlayers);
                            LOGGER.info("[DATABASE] Default victory score saved.");
                        }

                        // Let the Model notify the end of the game, not the Controller
                        game.notifyEndGame(Collections.singletonList(winner));

                    } catch (Exception e) {
                        LOGGER.severe("Error during default victory: " + e.getMessage());
                    }
                }
                suspended = false;
            }
        }, SOLO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Resumes the game after a second player has reconnected.
     */
    private void resumeGame() {
        if (!suspended) return;
        suspended = false;
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
        LOGGER.info("Game resumed: at least 2 active players.");
        broadcastGameResumed();
    }

    private Player findSoloActivePlayer() {
        return game.getPlayers().stream()
                .filter(p -> !disconnectedPlayers.contains(p.getNickname()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves and sends the global leaderboard to the requesting client.
     * Reads all entries from the database, calculates the player's position
     * and sends a {@link LeaderboardUpdate} to the client.
     *
     * @param connection the connection of the client requesting the leaderboard
     * @param numPlayers the number of players in the game (for filtering)
     */
    public void handleGetLeaderboard(ClientConnection connection, int numPlayers) {
        List<it.polimi.ingsw.am54.network.messages.LeaderboardEntry> entries = dbManager.getAllLeaderboardEntries();
        int playerPosition = -1;
        LOGGER.info("Leaderboard request: " + entries.size() + " entries found");
        connection.sendLeaderboardUpdate(new LeaderboardUpdate(playerPosition, entries, numPlayers));
    }

    private void executeCommand(String nickname, GameAction action) {
        if (suspended) {
            VirtualView view = virtualViews.get(nickname);
            if (view != null) {
                view.showError("Game suspended: waiting for reconnections...");
            }
            return;
        }
        synchronized (gameLock) {
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

    private void checkAndResolveAutomaticPhases() throws Exception {
        if (game.getCurrentState().isAutoResolvable() && !game.getCurrentState().isGameOver()) {
            game.getCurrentState().resolvePhase(game);
            saveState();
        }

        if (game.getCurrentState().isGameOver() && !scoresSavedToDatabase) {
            scoresSavedToDatabase = true;
            int numPlayers = game.getPlayers().size();

            new Thread(() -> {
                try {
                    for (Player p : game.getPlayers()) {
                        int finalScore = game.calculatePlayerScore(p);
                        dbManager.saveMatchResult(p.getNickname(), finalScore, numPlayers);
                    }
                    LOGGER.info("[DATABASE] End-game scores saved successfully!");
                } catch (Exception e) {
                    LOGGER.warning("[DATABASE] Error saving scores: " + e.getMessage());
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
