package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.ClientMessage;
import it.polimi.ingsw.am54.network.messages.PingRequest;
import it.polimi.ingsw.am54.network.messages.PongResponse;
import it.polimi.ingsw.am54.network.messages.Request;
import it.polimi.ingsw.am54.network.messages.Update;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ServerHandler serverHandler;

    public boolean connect(String ip, int port) {
        try {
            this.socket = new Socket(ip, port);
            System.out.println("[Client] Socket connesso al server " + ip + ":" + port);

            this.out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Client] In attesa dell'ACK dal server...");
            Object welcomeMsg = in.readObject();

            if ("CONNECTION_ESTABLISHED".equals(welcomeMsg)) {
                System.out.println("[Client] Handshake completato con successo!");

                this.serverHandler = new ServerHandler(in, this);
                new Thread(serverHandler).start();

                return true;
            } else {
                System.err.println("[Client] Errore critico: ACK non riconosciuto.");
                disconnect();
                return false;
            }

        } catch (Exception e) {
            System.err.println("[Client] Errore di connessione: " + e.getMessage());
            return false;
        }
    }

    public synchronized void sendMessage(ClientMessage msg) {
        if (out != null && !socket.isClosed()) {
            try {
                out.writeObject(msg);
                out.flush();
                out.reset();
            } catch (IOException e) {
                System.err.println("[Client] Errore nell'invio del messaggio: " + e.getMessage());
            }
        }
    }

    public void sendPong(long timestamp) {
        sendMessage(new PongResponse(timestamp));
    }

    public void disconnect() {
        if (serverHandler != null) {
            serverHandler.stop();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("[Client] Disconnesso dal server.");
            }
        } catch (IOException e) {
            System.err.println("[Client] Errore durante la disconnessione: " + e.getMessage());
        }
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }
}