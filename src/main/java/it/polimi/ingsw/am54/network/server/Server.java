package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.database.DatabaseConfig;
import it.polimi.ingsw.am54.database.DatabaseManager;
import it.polimi.ingsw.am54.model.Game;

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
 * ============================================================================
 */
public class Server {

    /** Una singola istanza del gioco */
    private final Game game;

    /** Un solo controller per la partita */
    private final Controller controller;

    /** Database manager for persisting match results (leaderboard). */
    private final DatabaseManager dbManager;

    /** Flag per controllare se il server e' in esecuzione */
    private boolean isRunning;

    /**
     * Costruttore del Server.
     * Inizializza sempre una nuova partita pulita.
     */
    public Server() {
        this.game = new Game();
        this.controller = new Controller(game);
        this.controller.setServer(this);
        this.isRunning = true;

        // Initialise database for leaderboard persistence
        this.dbManager = new DatabaseManager(new DatabaseConfig());
        this.dbManager.init();
        this.controller.setDatabaseManager(this.dbManager);

        System.out.println("Server creato con una nuova partita.");
    }

    public Controller getController() {
        return controller;
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