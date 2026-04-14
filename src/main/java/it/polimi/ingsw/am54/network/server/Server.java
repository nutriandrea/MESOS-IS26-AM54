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

    // Flag per spegnere il server in modo pulito
    private volatile boolean isRunning;

    // Lista thread-safe per tenere traccia di chi è online
    private final List<ClientHandler> activeClients;

    public Server(int port) {
        this.port = port;
        this.game = new Game(); // Il Model reale
        this.controller = new Controller(game); // Il Controller
        this.activeClients = Collections.synchronizedList(new ArrayList<>());
    }

    public void start() {
        this.isRunning = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Avviato con successo sulla porta " + port);

            while (isRunning) {
                // Il server si ferma qui finché non bussa qualcuno
                Socket socket = serverSocket.accept();
                System.out.println("[Server] Nuova connessione da: " + socket.getInetAddress());

                // --- IL BUTTAFUORI ---
                // Se la partita è piena (es. max 4 giocatori) o già iniziata, rifiutiamo gentilmente.
                // Sostituisci "4" e "isStarted()" con la logica reale del tuo gioco.
                if (game.getPlayers().size() >= 4 /* || game.isStarted() */) {
                    System.out.println("[Server] Partita piena. Connessione rifiutata.");
                    socket.close();
                    continue; // Torna su ad aspettare un'altra connessione
                }

                // Crea l'Handler per il client
                SocketClientHandler handler = new SocketClientHandler(socket, controller);

                // Lo aggiungiamo alla lista dei presenti
                activeClients.add(handler);

                // Crea la VirtualView per ingannare l'MVC
                VirtualView vView = new VirtualView(handler);

                // La VirtualView inizia a spiare il Model
                game.addObserver(vView);

                // Avvia il thread dedicato a questo giocatore
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

    /**
     * Metodo per spegnere il server chiudendo tutte le connessioni attive.
     */
    public void stop() {
        this.isRunning = false;

        // Disconnetti brutalmente tutti i client per evitare che rimangano appesi
        for (ClientHandler client : activeClients) {
            client.disconnect();
        }
        activeClients.clear();

        System.out.println("[Server] Spento correttamente.");
    }

    /**
     * (Opzionale) Metodo utile se in futuro il Controller dovrà buttare fuori qualcuno.
     */
    public void removeClient(ClientHandler handler) {
        activeClients.remove(handler);
    }
}