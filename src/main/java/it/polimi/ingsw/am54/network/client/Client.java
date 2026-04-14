package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.ClientMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ServerHandler serverHandler;

    // Ho cambiato il tipo di ritorno in boolean: così la GUI sa se la connessione è andata a buon fine
    public boolean connect(String ip, int port) {
        try {
            this.socket = new Socket(ip, port);
            System.out.println("[Client] Socket connesso al server " + ip + ":" + port);

            // 1. Apriamo l'Output Stream e facciamo flush
            this.out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            // 2. Apriamo l'Input Stream
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // 3. ASPETTIAMO L'ACK DEL SERVER (Fase di Handshake)
            System.out.println("[Client] In attesa dell'ACK dal server...");
            Object welcomeMsg = in.readObject();

            if ("CONNECTION_ESTABLISHED".equals(welcomeMsg)) {
                System.out.println("[Client] Handshake completato con successo!");

                // 4. Solo ORA avviamo il thread di ascolto passandogli l'InputStream già aperto
                this.serverHandler = new ServerHandler(in);
                new Thread(serverHandler).start();

                return true; // La GUI ora può far inserire il nickname
            } else {
                System.err.println("[Client] Errore critico: ACK non riconosciuto.");
                disconnect();
                return false;
            }

        } catch (Exception e) { // Cattura IOException e ClassNotFoundException (per il readObject)
            System.err.println("[Client] Errore di connessione: " + e.getMessage());
            return false;
        }
    }

    public synchronized void sendMessage(ClientMessage msg) {
        if (out != null && !socket.isClosed()) {
            try {
                out.writeObject(msg);
                out.flush();
                // Utile per resettare la cache degli oggetti inviati, evita memory leak e problemi di aggiornamento
                out.reset();
            } catch (IOException e) {
                System.err.println("[Client] Errore nell'invio del messaggio: " + e.getMessage());
            }
        }
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
}