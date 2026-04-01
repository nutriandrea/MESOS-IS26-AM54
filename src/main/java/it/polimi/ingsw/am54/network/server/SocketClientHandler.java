package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.messages.*;
import java.io.*;
import java.net.Socket;

public class SocketClientHandler implements ClientHandler, Runnable {
    private final Socket socket;
    private final Controller controller;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = true;

    public SocketClientHandler(Socket socket, Controller controller) {
        this.socket = socket;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            // Inizializza gli stream per la serializzazione [cite: 126, 134]
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Loop di ascolto comandi dal Client (Logica Applicativa) [cite: 74, 186]
            while (connected) {
                Object input = in.readObject();
                // Qui riceverai i comandi (es. un oggetto 'PickCardRequest')
                // e chiamerai i metodi del controller.
            }
        } catch (Exception e) {
            connected = false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public synchronized void sendMessage(Serializable message) {
        try {
            out.writeObject(message);
            out.flush(); // Forza l'invio dei byte sulla rete [cite: 135]
        } catch (IOException e) {
            closeConnection();
        }
    }

    @Override
    public void sendError(String errorMessage) {
        sendMessage(new ErrorMessage(errorMessage)); // [on error] signalError [cite: 27, 41]
    }

    private void closeConnection() {
        try { connected = false; socket.close(); } catch (IOException e) { /* ignore */ }
    }
}
