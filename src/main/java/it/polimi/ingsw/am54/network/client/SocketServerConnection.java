package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import java.io.*;
import java.net.Socket;

public class SocketServerConnection implements ServerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected;
    private ServerHandler serverHandler;

    public SocketServerConnection(String host, int port, ClientModel clientModel) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.connected = true;
        this.serverHandler = new ServerHandler(this.in, clientModel);

        new Thread(this.serverHandler).start();
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
    public void disconnect() {
        connected = false;
        // Fermiamo prima il thread in ascolto in modo pulito
        if (serverHandler != null) {
            serverHandler.stop();
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}