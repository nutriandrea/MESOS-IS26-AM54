package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.states.EndGameState;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * ============================================================================
 * Server — Entry point del server AM54 (single game)
 * ============================================================================
 *
 * RESPONSABILITA':
 * - Avviare il server Socket TCP sulla porta specificata (default 12345)
 * - Avviare il server RMI sulla porta specificata (default 1099)
 * - Gestire una singola partita (una sola istanza di Game + Controller)
 * - PERSISTENZA: Carica da salvataggio se esiste, avvia salvataggio periodico
 * ============================================================================
 */
public class Server {

    /** Una singola istanza del gioco */
    private final Game game;

    /** Un solo controller per la partita */
    private final Controller controller;

    /** Gestore della persistenza (salvataggio/caricamento JSON) */
    private final PersistenceManager persistenceManager;

    /** Flag per controllare se il server e' in esecuzione */
    private boolean isRunning;

    /**
     * Costruttore del Server.
     * 
     * COMPORTAMENTO PERSISTENZA:
     * 1. Controlla se esiste un file di salvataggio (game_state.json)
     * 2. Se esiste e la partita non è finita: carica da esso (CRASH RECOVERY)
     * 3. Altrimenti: inizializza una nuova partita vuota
     * 4. Avvia il salvataggio automatico periodico
     * 5. Registra shutdown hook per salvataggio finale
     */
    public Server() {
        this.persistenceManager = new PersistenceManager();
        
        Game loadedGame = null;
        
        // Prova a caricare da salvataggio se esiste
        if (persistenceManager.hasSavedGame()) {
            PersistenceManager.GameFullSnapshot snapshot = persistenceManager.loadState();
            if (snapshot != null) {
                // Non ricarichiamo se la partita era nello stato finale
                if ("EndGameState".equals(snapshot.getCurrentStateName())) {
                    System.out.println("[Server] Salvataggio trovato ma è di una partita finita. Inizializzo nuova partita.");
                    persistenceManager.deleteSaveFile();
                } else {
                    System.out.println("[Server] Trovato salvataggio di partita in corso. Provo a ripristinare...");
                    loadedGame = persistenceManager.restoreGameFromSnapshot(snapshot);
                    if (loadedGame == null) {
                        System.err.println("[Server] Ripristino fallito! Inizializzo nuova partita.");
                    }
                }
            }
        }
        
        // Usa gioco caricato o nuovo
        if (loadedGame != null) {
            this.game = loadedGame;
            System.out.println("[Server] ✓ Partita ripristinata da salvataggio!");
        } else {
            this.game = new Game();
            System.out.println("[Server] Nuova partita inizializzata.");
        }
        
        this.controller = new Controller(game);
        this.controller.setServer(this);
        
        // Collega PersistenceManager a Game e Controller
        persistenceManager.setGameAndController(game, controller);
        
        // Avvia salvataggio periodico
        persistenceManager.startPeriodicSave();
        
        // Shutdown Hook: salva lo stato quando la JVM viene chiusa (Ctrl+C, kill, ecc.)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Server] Shutdown rilevato. Salvataggio stato finale...");
            persistenceManager.stopPeriodicSave();
            // Salva solo se la partita è in corso e non è finita
            if (game != null && !(game.getCurrentState() instanceof EndGameState)) {
                persistenceManager.saveState();
            }
            System.out.println("[Server] Arresto completato.");
        }));
        
        this.isRunning = true;
    }

    public Controller getController() {
        return controller;
    }

    /**
     * Salva lo stato corrente della partita.
     * Chiamato dal Controller dopo ogni azione significativa.
     */
    public void saveGameState() {
        // Non salviamo se siamo in EndGameState (partita finita)
        if (game != null && !(game.getCurrentState() instanceof EndGameState)) {
            persistenceManager.saveState();
        }
    }

    /**
     * Cancella il file di salvataggio.
     * Chiamato quando una nuova partita inizia o quella corrente finisce.
     */
    public void clearSavedGame() {
        persistenceManager.deleteSaveFile();
    }

    /**
     * Restituisce il PersistenceManager (per debug/test).
     */
    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    /**
     * Avvia il server su TCP e RMI.
     *
     * @param tcpPort porta per le connessioni Socket TCP
     * @param rmiPort porta per le connessioni RMI
     */
    public void start(int tcpPort, int rmiPort) {
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
        System.out.println("=== Avvio Server AM54 ===");
        Server server = new Server();
        server.start(12345, 1099);
    }
}