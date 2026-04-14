package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.Update;
import java.io.ObjectInputStream;

public class ServerHandler implements Runnable {

    private ObjectInputStream in;
    private volatile boolean running;

    // ECCO IL COSTRUTTORE AGGIORNATO: ora accetta l'ObjectInputStream
    public ServerHandler(ObjectInputStream in) {
        this.in = in;
        this.running = true;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Legge l'Update inviato dal server
                Update message = (Update) in.readObject();
                handleUpdate(message);
            } catch (Exception e) {
                if (running) {
                    System.err.println("[ServerHandler] Connessione col server persa: " + e.getMessage());
                    stop();
                }
            }
        }
    }

    private void handleUpdate(Update message) {
        // Qui ci sarà la logica per smistare i messaggi alla GUI/TUI
        System.out.println("[ServerHandler] Ricevuto aggiornamento: " + message.getClass().getSimpleName());
    }

    public void stop() {
        this.running = false;
    }
}