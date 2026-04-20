package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    private final Game game;
    private final Controller controller;
    private boolean isRunning;

    public Server() {
        this.game = new Game();
        this.controller = new Controller(game);
        this.isRunning = true;
    }

    public void start(int tcpPort, int rmiPort) {
        startRMI(rmiPort);
        startTCP(tcpPort);
    }

    private void startRMI(int port) {
        try {
            System.setProperty("java.rmi.server.hostname", "localhost"); // Utile per test locali
            Registry registry = LocateRegistry.createRegistry(port);
            RMIServerImpl rmiServer = new RMIServerImpl(game, controller);
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
                System.out.println("[Server TCP] Nuova connessione socket!");

                SocketClientHandler handler = new SocketClientHandler(clientSocket, controller);
                VirtualView virtualView = new VirtualView(handler);
                game.addObserver(virtualView);

                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Errore critico TCP: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Avvio Server AM54 ===");
        Server server = new Server();
        server.start(12345, 1099); // 12345 per TCP, 1099 per RMI
    }
}
