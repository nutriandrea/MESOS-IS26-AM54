package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * ============================================================================
 * Server — AM54 server entry point (single game)
 * ============================================================================
 */
public class Server {

    private final Game game;
    private final Controller controller;
    private boolean isRunning;

    /**
     * Builds the server. Attempts to restore a game from {@code backup.json}:
     * if a valid backup exists it resumes that game (crash recovery), otherwise
     * it creates a new and clean one.
     */
    public Server() {
        PersistenceManager pm = new PersistenceManager("backup.json");
        Game loadedGame = pm.loadGame();

        // It activates ONLY if the backup file exists and is valid
        if (loadedGame != null) {
            this.game = loadedGame;
            this.controller = new Controller(this.game);

            // CALL HERE: Only during actual recovery
            this.controller.validateGameStateAfterRecovery();

            System.out.println("[Server] Crash rilevato: partita ripristinata.");
        } else {
            // Clean normal start
            this.game = new Game();
            this.controller = new Controller(game);
            System.out.println("Server creato con una nuova partita.");
        }

        this.controller.setServer(this);
        this.isRunning = true;
    }

    public Controller getController() {
        return controller;
    }

    /**
     * Starts both server transports: first the RMI registry, then the TCP acceptance
     * loop (which is blocking and keeps the server alive).
     *
     * @param tcpPort the port for TCP Socket connections
     * @param rmiPort the port of the RMI registry
     */
    public void start(int tcpPort, int rmiPort) {
        startRMI(rmiPort);
        startTCP(tcpPort);
    }

    /**
     * Creates the RMI registry on the indicated port and publishes the game server
     * ("AM54-Server") there. Any errors are logged without interrupting the server.
     *
     * @param port the port of the RMI registry
     */
    private void startRMI(int port) {
        try {
            // The RMI hostname has already been globally set in main!
            Registry registry = LocateRegistry.createRegistry(port);
            RMIServerImpl rmiServer = new RMIServerImpl(this);
            registry.rebind("AM54-Server", rmiServer);
            System.out.println("[Server] Motore RMI avviato su porta " + port);
        } catch (Exception e) {
            System.err.println("[Server] Impossibile avviare RMI: " + e.getMessage());
        }
    }

    /**
     * Starts the TCP acceptance loop: for each connecting client it creates a
     * {@link SocketClientHandler} on a dedicated thread. Blocking as long as the server
     * remains active.
     *
     * @param port the port on which to listen for TCP connections
     */
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

    /**
     * Server entry point: automatically detects the IP of the active network card
     * (to communicate to players), sets it as the RMI hostname and starts the
     * server on TCP port 12345 and RMI port 1099.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("=== Avvio Server AM54 ===");

        String serverIp = "127.0.0.1"; // Safety fallback

        try (java.net.DatagramSocket s = new java.net.DatagramSocket()) {
            // Pretends to connect outside to discover the active Wi-Fi card's IP
            s.connect(java.net.InetAddress.getByName("8.8.8.8"), 10002);
            serverIp = s.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            System.err.println("Autodetect IP fallito. Uso localhost.");
        }

        // Sets the RMI hostname dynamically
        System.setProperty("java.rmi.server.hostname", serverIp);
        System.out.println("[Server] RMI Hostname globale impostato a: " + serverIp);
        System.out.println("\n---> COMUNICA QUESTO IP AGLI ALTRI GIOCATORI: " + serverIp + " <--- \n");

        Server server = new Server();
        server.start(12345, 1099);
    }
}