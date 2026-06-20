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
 * Server — Entry point of the AM54 server (single game)
 * ============================================================================
 */
public class Server {

    private final Game game;
    private final Controller controller;
    private boolean isRunning;

    /**
     * Constructs the server, attempting to restore a saved game from
     * backup. If a valid backup exists, the game and controller are
     * recovered and the state is validated; otherwise a fresh game is created.
     */
    public Server() {
        PersistenceManager pm = new PersistenceManager("backup.json");
        Game loadedGame = pm.loadGame();

        // Activates ONLY if the backup file exists and is valid
        if (loadedGame != null) {
            this.game = loadedGame;
            this.controller = new Controller(this.game);

            // CALLED HERE: Only during the actual restore
            this.controller.validateGameStateAfterRecovery();

            System.out.println("[Server] Crash rilevato: partita ripristinata.");
        } else {
            // Normal clean startup
            this.game = new Game();
            this.controller = new Controller(game);
            System.out.println("Server creato con una nuova partita.");
        }

        this.controller.setServer(this);
        this.isRunning = true;
    }

    /** @return the game controller */
    public Controller getController() {
        return controller;
    }

    /**
     * Starts both the RMI and TCP server engines.
     *
     * @param tcpPort the TCP port to listen on
     * @param rmiPort the RMI registry port
     */
    public void start(int tcpPort, int rmiPort) {
        startRMI(rmiPort);
        startTCP(tcpPort);
    }

    private void startRMI(int port) {
        try {
            // The RMI hostname has already been set globally in main!
            Registry registry = LocateRegistry.createRegistry(port);
            RMIServerImpl rmiServer = new RMIServerImpl(this);
            registry.rebind("AM54-Server", rmiServer);
            System.out.println("[Server] Motore RMI avviato su porta " + port);
        } catch (Exception e) {
            System.err.println("[Server] Unable avviare RMI: " + e.getMessage());
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

    /**
     * Entry point: auto-detects the LAN IP, sets the RMI hostname and
     * starts the server on the default ports (TCP 12345, RMI 1099).
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println("=== Avvio Server AM54 ===");

        String serverIp = "127.0.0.1"; // Fallback di sicurezza

        try (java.net.DatagramSocket s = new java.net.DatagramSocket()) {
            // Pretends to connect externally to discover the active Wi-Fi adapter IP
            s.connect(java.net.InetAddress.getByName("8.8.8.8"), 10002);
            serverIp = s.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            System.err.println("Autodetect IP fallito. Uso localhost.");
        }

        // Sets l'hostname RMI in modo dinamico
        System.setProperty("java.rmi.server.hostname", serverIp);
        System.out.println("[Server] RMI Hostname globale impostato a: " + serverIp);
        System.out.println("\n---> COMUNICA QUESTO IP AGLI ALTRI GIOCATORI: " + serverIp + " <--- \n");

        Server server = new Server();
        server.start(12345, 1099);
    }
}