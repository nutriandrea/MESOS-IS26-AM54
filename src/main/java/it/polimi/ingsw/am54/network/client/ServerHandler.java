package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ============================================================================
 * ServerHandler — Thread di ascolto asincrono lato client (Socket TCP)
 * ============================================================================
 *
 * RUOLO:
 * Thread indipendente che rimane in ascolto continuo dei messaggi provenienti
 * dal server sulla connessione Socket TCP.
 *
 * COSA FA:
 * 1. Legge oggetti dallo stream di input del socket
 * 2. Se e' un PingRequest: risponde AUTOMATICAMENTE con un PongResponse
 *    (il client non deve fare nulla, e' tutto automatico)
 * 3. Se e' un Update: aggiorna il ClientModel di conseguenza
 * 4. Se il socket si chiude: ferma il thread
 *
 * GESTIONE PING (trasparente per il client):
 * Quando il server invia un PingRequest, il client risponde immediatamente
 * con un PongResponse contenente lo stesso timestamp. Questo permette al
 * server di calcolare il round-trip time e rilevare disconnessioni.
 *
 * ============================================================================
 */
public class ServerHandler implements Runnable {

    /** Stream di input per ricevere messaggi dal server */
    private final ObjectInputStream in;

    /** Stream di output per rispondere ai ping */
    private final ObjectOutputStream out;

    /** Modello locale del client: viene aggiornato con gli update ricevuti */
    private final ClientModel clientModel;

    /** Flag di stato: true = in ascolto, false = fermato */
    private volatile boolean running = true;

    /**
     * Costruttore.
     *
     * @param in stream di input dal server
     * @param out stream di output verso il server (usato per rispondere ai ping)
     * @param clientModel modello locale da aggiornare
     */
    public ServerHandler(ObjectInputStream in, ObjectOutputStream out, ClientModel clientModel) {
        this.in = in;
        this.out = out;
        this.clientModel = clientModel;
    }

    /**
     * Loop principale: legge messaggi dal server finche' running == true.
     *
     * Per ogni messaggio ricevuto:
     * - Se e' un PingRequest -> rispondi con PongResponse (automatico)
     * - Se e' un Update -> aggiorna il ClientModel
     * - Se eccezione -> ferma il thread
     */
    @Override
    public void run() {
        while (running) {
            try {
                Object message = in.readObject();
                if (message instanceof Update update) {
                    handleUpdate(update);
                }
            } catch (Exception e) {
                // SE IL SERVER CADE, ENTRA QUI:
                if (running) {
                    System.err.println("[SOCKET] Connessione persa, avvio auto-reconnect...");
                    clientModel.triggerConnectionLost();
                    stop();
                }
            }
        }
    }

    /**
     * Risponde automaticamente a un PingRequest del server.
     * Invia un PongResponse con lo stesso timestamp ricevuto.
     *
     * Questo e' completamente trasparente per il client: il giocatore
     * non vede nulla, il ping/pong avviene in background.
     */
    private void handlePing(PingRequest ping) {
        try {
            out.writeObject(new PongResponse(ping.getTimestamp()));
            out.flush();
            out.reset();
        } catch (Exception e) {
            if (running) {
                System.err.println("[SOCKET] Connessione persa al ping, avvio auto-reconnect...");
                clientModel.triggerConnectionLost();
                stop();
            }
        }
    }

    /** Ferma il thread di ascolto */
    public void stop() { this.running = false; }

    /**
     * Smista gli Update ricevuti dal server al ClientModel.
     *
     * DISPATCH POLIMORFICO: message.applyTo(clientModel) delega a ogni sottotipo
     * la propria logica di aggiornamento, senza switch o reflection.
     *
     * ECCEZIONI (gestite prima del dispatch):
     * - PingRequest: risposta automatica a livello socket (trasparente per il client)
     * - StateChangedUpdate: dopo applyTo(), verifica se la GUI deve passare
     *   dalla scena lobby alla scena partita (responsabilita' UI separata dal model)
     */
    private void handleUpdate(Update message) {
        if (message instanceof PingRequest ping) {
            handlePing(ping);
            return;
        }

        String oldState = (message instanceof StateChangedUpdate)
                ? clientModel.getCurrentState() : null;

        message.applyTo(clientModel);

        if (oldState != null) {
            String newState = clientModel.getCurrentState();
            if (it.polimi.ingsw.am54.gui.states.ClientStateRegistry.getHandler(oldState).isLobbyState() &&
                    !it.polimi.ingsw.am54.gui.states.ClientStateRegistry.getHandler(newState).isLobbyState()) {
                try {
                    it.polimi.ingsw.am54.gui.GUIApp.loadBoardScene();
                } catch (Exception | NoClassDefFoundError e) {
                }
            }
        }
    }
}
