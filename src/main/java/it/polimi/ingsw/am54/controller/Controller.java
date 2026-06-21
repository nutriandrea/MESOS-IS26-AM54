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
 * Il giocatore viene marcato come disconnesso ma NON rimosso dal gioco.
 * Gli altri giocatori ricevono PlayerDisconnectedUpdate.
 * - removeClient(): pulizia definitiva. Succede quando il socket si chiude
 * ============================================================================
 */
public class Controller {
    private final Game game;
    private Server server;

    private final it.polimi.ingsw.am54.network.server.PersistenceManager persistenceManager;
    private boolean scoresSavedToDatabase = false;

    /** Mappa nickname -> VirtualView. Ogni VirtualView e' observer del Game */
    private final Map<String, VirtualView> virtualViews = new ConcurrentHashMap<>();

    /** Set di nickname dei giocatori temporaneamente disconnessi */
    private final Set<String> disconnectedPlayers = ConcurrentHashMap.newKeySet();

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    /** Database manager per salvare i risultati delle partite. */
    private final DatabaseManager dbManager;

    /** Flag: true quando un solo giocatore e' attivo e il gioco e' sospeso */
    private volatile boolean suspended = false;

    /** Timeout dopo cui il giocatore rimasto attivo vince (in secondi) */
    private static final long SOLO_TIMEOUT_SECONDS = 60;

    /** Scheduler per il timeout di sospensione */
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    /** Riferimento al task di timeout attivo (per cancellarlo in caso di ripresa) */
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
     * Imposta il riferimento al Server.
     * Necessario per permettere al Controller di chiamare saveState()
     * sul PersistenceManager dopo ogni azione di gioco.
     *
     * @param server l'istanza del Server
     */
    public void setServer(Server server) {
        this.server = server;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    /**
     * Salva lo stato corrente del gioco su file JSON.
     * Chiamato dal Controller dopo ogni azione di gioco
     * (setup, chooseTotemColor, placeTotem, pickCard).
     * * Se il server non è inizializzato (server null), non fa nulla.
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

    public void handleJoinGame(String nickname, ClientConnection connection) {
        synchronized (game.getPlayers()) {
            // CASO 1: Il giocatore era caduto/disconnesso e sta tornando
            if (disconnectedPlayers.contains(nickname)) {
                handleReconnect(nickname, connection);
                return;
            }

            // CASO 2: Qualcuno sta provando a entrare con un nome GIA' ATTIVO nel server
            if (virtualViews.containsKey(nickname)) {
                // Creiamo una view temporanea SOLO per poter spedire indietro l'errore
                VirtualView rejectedView = new VirtualView(connection, nickname);
                rejectedView.showError("Nickname già in uso! Scegline un altro.");
                return; // Blocchiamo l'entrata e usciamo dal metodo!
            }

            // CASO 3: Il nome è libero, normale procedura di Join
            VirtualView playerView = new VirtualView(connection, nickname);
            virtualViews.put(nickname, playerView);
            game.addObserver(playerView);

            // Manda al nuovo giocatore lo stato di chi è già nella lobby
            for (Player p : game.getPlayers()) {
                playerView.onPlayerJoined(game, p);
                if (p.getTotemColors() != null) {
                    playerView.onTotemColorChosen(game, p, p.getTotemColors());
                }
            }

            try {
                // Prova ad aggiungerlo al gioco vero e proprio (che controlla se ci sono troppi giocatori)
                game.joinGame(nickname);
            } catch (Exception e) {
                // Se la partita è piena, rimuove la view e manda l'errore
                virtualViews.remove(nickname);
                game.removeObserver(playerView);
                playerView.showError(e.getMessage());
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

            notifyAllClientsStateChanged();
        }

        connection.startHeartbeat();
        broadcastPlayerReconnected(nickname);

        if (suspended && activePlayerCount() >= 2) {
            resumeGame();
        }
    }

    public void validateGameStateAfterRecovery() {
        synchronized (game.getPlayers()) {

            // Ripristiniamo il turno in base alla fase attuale della partita
            it.polimi.ingsw.am54.model.Player recovered = game.getCurrentState().recoverActivePlayer(game);
            if (recovered != null) {
                game.setCurrentPlayer(recovered);
            }

            // 2. Log diagnostico sulla console del server per confermare
            it.polimi.ingsw.am54.model.Player current = game.getCurrentPlayer();
            if (current != null) {
                System.out.println("[RECOVERY] Fase della partita: " + game.getCurrentState().getClass().getSimpleName());
                System.out.println("[RECOVERY] Il Server ha assegnato il turno a: " + current.getNickname());
            }
        }
    }

    /* =====================================================================
     * GESTIONE COMANDI DI GIOCO
     * ===================================================================== */

    public void handleSetupGame(String nickname) {
        executeCommand(nickname, () -> {
            game.setupGame();
            saveState();
        });
    }

    public void handleChooseTotemColor(String nickname, String color) {
        executeCommand(nickname, () -> {
            game.chooseTotemColor(nickname, color);
            saveState();
        });
    }

    public void handlePlaceTotem(String nickname, int position) {
        executeCommand(nickname, () -> {
            game.placeTotem(nickname, position);
            saveState();
        });
    }

    public void handlePickCard(String nickname, String row, int index) {
        executeCommand(nickname, () -> {
            game.pickCard(nickname, row, index);
            checkAndResolveAutomaticPhases();
            saveState();
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

        if (game.getCurrentState().isRemovePermanent()) {
            LOGGER.info("Giocatore " + nickname + " abbandona la lobby. Libero il posto e il Totem.");

            // 1. Rimuove il giocatore dal modello e rimette il colore Totem a disposizione
            game.removePlayerEntirely(nickname);

            // Rimuoviamo il nickname dal set dei disconnessi, così è libero di rientrare
            disconnectedPlayers.remove(nickname);

            if (view != null) game.removeObserver(view);

            if (game.getPlayers().isEmpty()) {
                persistenceManager.clearBackup();
            } else {

                notifyAllClientsStateChanged();
            }
        } else {
            // --- LOGICA DI DISCONNESSIONE TEMPORANEA (Crash in-game) ---
            disconnectedPlayers.add(nickname);
            if (view != null) {
                synchronized (game.getPlayers()) {
                    game.removeObserver(view);
                }
            }
            LOGGER.info("Giocatore " + nickname + " disconnesso temporaneamente.");
            broadcastPlayerDisconnected(nickname);
            autoHandleDisconnection(nickname);
        }
    }

    private void notifyAllClientsStateChanged() {
        for (VirtualView view : virtualViews.values()) {
            // Se la view è ancora valida, mandiamo un aggiornamento
            view.onStateChanged(game, game.getCurrentState());
        }
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
     */
    private void autoHandleDisconnection(String nickname) {
        synchronized (game.getPlayers()) {
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
                LOGGER.warning("Skip turn fallito per " + nickname + ": " + e.getMessage());
            }

            checkSuspension();
        }
    }

    /**
     * Se il numero di giocatori attivi e' sceso a 1, sospende il gioco.
     */
    /**
     * Se il numero di giocatori attivi e' sceso a 1, sospende il gioco.
     * Se scende a 0, resetta la stanza e cancella il backup.
     */
    private void checkSuspension() {
        // ========================================================
        // NUOVA LOGICA: Controllo "Stanza Vuota" a partita in corso
        // ========================================================
        if (activePlayerCount() == 0) {
            LOGGER.warning("[SERVER] Tutti i giocatori sono disconnessi. Annullamento partita...");

            // 1. Cancella fisicamente il salvataggio
            persistenceManager.clearBackup();

            // 2. Ferma il timer di vittoria a tavolino (se era partito)
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
            suspended = false;

            // 3. Pulisce la memoria e riporta il server in Lobby!
            game.getPlayers().clear();
            disconnectedPlayers.clear();
            virtualViews.clear();
            game.setState(new it.polimi.ingsw.am54.model.states.SetUpGameState());
            return;
        }

        // ========================================================
        // Vecchia logica per 1 solo giocatore (Vittoria a tavolino)
        // ========================================================
        if (suspended) return;
        if (activePlayerCount() == 1) {
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
                    LOGGER.info("Timeout sospensione scaduto: " + winner.getNickname() + " vince la partita per abbandono!");

                    // 1. Mettiamo il Model nello stato di fine partita
                    game.setState(new it.polimi.ingsw.am54.model.states.EndGameState());

                    // 2. Chiamiamo una risoluzione speciale per la vittoria a tavolino
                    // Perché non usiamo resolvePhase()? Perché resolvePhase() calcola i punteggi
                    // e applica l'aura. Qui vogliamo far vincere il giocatore e basta.
                    try {
                        // Salvataggio nel DB del vincitore a tavolino (come avevi già fatto)
                        if (!scoresSavedToDatabase) {
                            scoresSavedToDatabase = true;
                            int finalScore = game.calculatePlayerScore(winner);
                            int numPlayers = game.getPlayers().size();
                            dbManager.saveMatchResult(winner.getNickname(), finalScore, numPlayers);
                            LOGGER.info("[DATABASE] Punteggio per abbandono salvato.");
                        }

                        // Lasciamo che sia il Model a notificare la fine della partita, non il Controller
                        game.notifyEndGame(Collections.singletonList(winner));

                    } catch (Exception e) {
                        LOGGER.severe("Errore durante la vittoria a tavolino: " + e.getMessage());
                    }
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

    private Player findSoloActivePlayer() {
        return game.getPlayers().stream()
                .filter(p -> !disconnectedPlayers.contains(p.getNickname()))
                .findFirst()
                .orElse(null);
    }

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
                view.showError("Gioco sospeso: in attesa di riconnessioni...");
            }
            return;
        }
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
                    LOGGER.info("[DATABASE] Punteggi di fine partita salvati con successo!");
                } catch (Exception e) {
                    LOGGER.warning("[DATABASE] Errore salvataggio punteggi: " + e.getMessage());
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