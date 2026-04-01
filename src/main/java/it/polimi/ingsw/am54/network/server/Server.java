package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port;
    private final Game game;
    private final Controller controller;

    public Server(int port) {
        this.port = port;
        this.game = new Game(); // Il Model reale [cite: 226]
        this.controller = new Controller(game); // Il Controller [cite: 212]
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server avviato sulla porta " + port);

            while (true) {
                // Attende la connessione di un client [cite: 125]
                Socket socket = serverSocket.accept();

                // Crea l'Handler e la VirtualView per questo client
                SocketClientHandler handler = new SocketClientHandler(socket, controller);
                VirtualView vView = new VirtualView(handler);

                // Registra la VirtualView come Observer del Model [cite: 165, 176]
                game.addObserver(vView);

                // Avvia il thread dedicato al client
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
