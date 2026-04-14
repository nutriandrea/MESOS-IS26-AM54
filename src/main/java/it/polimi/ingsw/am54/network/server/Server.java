package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    private final int port;
    private final Game game;
    private final Controller controller;

    private volatile boolean isRunning;
    private final List<ClientHandler> activeClients;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 12345;
        System.out.println("=== AVVIO SERVER DI RETE ===");
        new Server(port).start();
    }

    public Server(int port) {
        this.port = port;
        this.game = new Game();
        this.controller = new Controller(game);
        this.activeClients = Collections.synchronizedList(new ArrayList<>());
    }

    public void start() {
        this.isRunning = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Avviato con successo sulla porta " + port);

            while (isRunning) {
                Socket socket = serverSocket.accept();
                System.out.println("[Server] Nuova connessione da: " + socket.getInetAddress());

                if (game.getPlayers().size() >= 4) {
                    System.out.println("[Server] Partita piena. Connessione rifiutata.");
                    socket.close();
                    continue;
                }

                SocketClientHandler handler = new SocketClientHandler(socket, controller);
                activeClients.add(handler);

                VirtualView vView = new VirtualView(handler);
                game.addObserver(vView);

                new Thread(handler).start();
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("[Server] Errore critico nel ServerSocket: " + e.getMessage());
            } else {
                System.out.println("[Server] Spegnimento in corso...");
            }
        }
    }

    public void stop() {
        this.isRunning = false;

        for (ClientHandler client : activeClients) {
            client.disconnect();
        }
        activeClients.clear();

        System.out.println("[Server] Spento correttamente.");
    }

    public void removeClient(ClientHandler handler) {
        activeClients.remove(handler);
    }
}
