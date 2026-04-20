package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import java.io.*;
import java.net.Socket;

public class SocketServerConnection implements ServerConnection, Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected;
    // Riferimento alla View per passarle gli aggiornamenti
    private final it.polimi.ingsw.am54.model.IGameObserver view;

    public SocketServerConnection(String host, int port, it.polimi.ingsw.am54.model.IGameObserver view) throws IOException {
        this.socket = new Socket(host, port);
        this.view = view;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.connected = true;
    }

    @Override
    public void sendRequest(Request request) {
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
        } catch (IOException e) { disconnect(); }
    }

    @Override
    public void run() {
        try {
            while (connected) {
                Object input = in.readObject();
                if (input instanceof Update update) {
                    handleUpdate(update);
                }
            }
        } catch (Exception e) { disconnect(); }
    }

    private void handleUpdate(Update update) {
        // Qui lo "spacciatore" (Server Handler) smista gli aggiornamenti alla View
        if (update instanceof ErrorMessageUpdate msg) {
            System.err.println("ERRORE: " + msg.getMessage());
        }
        // In base all'Update ricevuto, chiami il metodo corrispondente della view
        // Es: if (update instanceof PlayerJoinedUpdate) view.onPlayerJoined(...)
    }

    @Override
    public void disconnect() {
        connected = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}