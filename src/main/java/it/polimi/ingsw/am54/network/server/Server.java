package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.model.states.EventState;
import it.polimi.ingsw.am54.model.states.EndGameState;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * ============================================================================
 * Server — Entry point del server AM54 (single game + persistence)
 * ============================================================================
 *
 * RESPONSABILITA':
 * - Avviare il server Socket TCP sulla porta specificata (default 12345)
 * - Avviare il server RMI sulla porta specificata (default 1099)
 * - Gestire una singola partita (una sola istanza di Game + Controller)
 * - Gestire la persistenza: recovery da game_state.json altrimenti nuova partita
 * - Salvataggio automatico ogni 30 secondi + su shutdown
 *
 * PERSISTENCE (CRASH RECOVERY):
 * - All'avvio verifica l'esistenza di game_state.json
 * - Se esiste: carica snapshot e ripristina stato del gioco
 * - Se non esiste: crea nuova partita
 * - Su shutdown: salvataggio finale tramite ShutdownHook
 * ============================================================================
 */
public class Server {

    /** Una singola istanza del gioco */
    private final Game game;
    
    /** Un solo controller per la partita */
    private final Controller controller;

    /** Manager per la persistenza (salvataggio/caricamento stato) */
    private final PersistenceManager persistenceManager;

    /** Flag per controllare se il server e' in esecuzione */
    private boolean isRunning;

    /**
     * Costruttore del Server.
     * Inizializza la partita (nuova o da recovery) e il persistence manager.
     * 
     * LOGICA DI RECOVERY:
     * 1. Verifica se esiste game_state.json (persistenceManager.hasSavedGame())
     * 2. Se esiste: carica snapshot e ripristina giocatori, board, stato
     * 3. Se non esiste: crea nuova partita vuota
     * 4. Avvia il salvataggio automatico periodico
     */
    public Server() {
        this.persistenceManager = new PersistenceManager();
        
        // Check per recovery: se esiste un salvataggio, ripristina la partita
        if (persistenceManager.hasSavedGame()) {
            PersistenceManager.GameFullSnapshot snapshot = persistenceManager.loadState();
            if (snapshot != null) {
                // Nuovo game vuoto
                this.game = new Game();
                this.controller = new Controller(game);
                this.controller.setServer(this);
                
                // Ripristina stato da snapshot (giocatori, colori, board, era, stato)
                restoreGameFromSnapshot(snapshot);
                
                System.out.println("[Server] Partita ripristinata: " + snapshot.getPlayerCount() + " giocatori, era " + snapshot.getCurrentEra());
            } else {
                // Errore nel caricamento, crea nuova partita
                this.game = new Game();
                this.controller = new Controller(game);
                this.controller.setServer(this);
                System.out.println("Server creato con una nuova partita.");
            }
        } else {
            // Nessun salvataggio esistente, partita nuova
            this.game = new Game();
            this.controller = new Controller(game);
            this.controller.setServer(this);
            System.out.println("Server creato con una nuova partita.");
        }
        
        // Collega Game e Controller al persistence manager per i salvataggi
        persistenceManager.setGameAndController(game, controller);
        
        // Avvia il timer per salvataggio automatico ogni 30 secondi
        persistenceManager.startPeriodicSave();
        
        this.isRunning = true;
    }

    /**
     * Ripristina lo stato del gioco da uno snapshot di persistenza.
     * 
     * PASSI DI RECOVERY:
     * 1. Ricrea i giocatori con joinGame()
     * 2. Ripristina i colori totem scelti
     * 3. Inizializza il board (setupGame)
     * 4. Ripristina era e round correnti
     * 5. Ripristina lo stato del gioco (State Machine)
     *
     * @param snapshot lo snapshot caricato da game_state.json
     */
    private void restoreGameFromSnapshot(PersistenceManager.GameFullSnapshot snapshot) {
        try {
            // 1. Ricrea i giocatori nel gioco
            for (PersistenceManager.PlayerSnapshot ps : snapshot.getPlayers()) {
                game.joinGame(ps.getNickname());
            }
            
            // 2. Ripristina i colori totem scelti da ogni giocatore
            for (PersistenceManager.PlayerSnapshot ps : snapshot.getPlayers()) {
                if (ps.getTotemColor() != null) {
                    game.chooseTotemColor(ps.getNickname(), ps.getTotemColor());
                }
            }
            
            // 3. Inizializza la board (carte, offering track, turn order)
            game.setupGame();
            
            // 4. Ripristina era e round
            game.setCurrentEra(snapshot.getCurrentEra());
            game.setRoundCount(snapshot.getRoundCount());
            
            // 5. Ripristina lo stato della State Machine
            // Nota: non ripristiniamo SetUpGameState perché siamo già in game
            GameState newState = null;
            switch (snapshot.getCurrentStateName()) {
                case "StartTurnState": newState = new StartTurnState(); break;
                case "PickingState": newState = new PickingState(); break;
                case "EventState": newState = new EventState(); break;
                case "EndGameState": newState = new EndGameState(); break;
                default: newState = new StartTurnState(); // Default per sicurezza
            }
            game.setState(newState);
            
            System.out.println("[Server] Giocatori ripristinati: " + snapshot.getPlayers().size());
            System.out.println("[Server] Stato ripristinato: " + snapshot.getCurrentStateName() + ", era " + snapshot.getCurrentEra());
        } catch (Exception e) {
            System.err.println("[Server] Errore nel ripristino: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Controller getController() {
        return controller;
    }

    /**
     * Restituisce il persistence manager per permettere ai controller
     * di triggerare salvataggi dopo azioni di gioco.
     */
    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    /**
     * Avvia il server su entrambe le porte.
     * Registra anche uno ShutdownHook per il salvataggio su chiusura.
     *
     * @param tcpPort porta per le connessioni Socket TCP
     * @param rmiPort porta per le connessioni RMI
     */
    public void start(int tcpPort, int rmiPort) {
        // ShutdownHook: eseguito quando il processo viene terminato
        // Salva lo stato finale del gioco prima di chiudere
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Server] Chiusura in corso...");
            persistenceManager.stopPeriodicSave();
            persistenceManager.saveState();
            System.out.println("[Server] Salvataggio finale completato.");
        }));

        startRMI(rmiPort);
        startTCP(tcpPort);
    }

    private void startRMI(int port) {
        try {
            System.setProperty("java.rmi.server.hostname", "localhost");
            Registry registry = LocateRegistry.createRegistry(port);
            RMIServerImpl rmiServer = new RMIServerImpl(this);
            registry.rebind("AM54-Server", rmiServer);
            System.out.println("[Server] Motore RMI avviato su porta " + port);
        } catch (Exception e) {
            System.err.println("[Server] Impossibile avviare RMI: " + e.getMessage());
        }
    }

    private void startTCP(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Motore TCP avviato su porta " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server TCP] Nuova connessione da: " + clientSocket.getInetAddress());

                SocketClientHandler handler = new SocketClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Errore critico TCP: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Avvio Server AM54 (single game + persistence) ===");
        Server server = new Server();
        server.start(12345, 1099);
    }
}