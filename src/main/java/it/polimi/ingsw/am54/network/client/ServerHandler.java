package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ============================================================================
 * ServerHandler — Asynchronous listening thread on the client side (TCP Socket)
 * ============================================================================
 *
 * ROLE:
 * Independent thread that continuously listens for messages coming
 * from the server over the TCP Socket connection.
 *
 * WHAT DOES IT DO:
 * 1. Reads objects from the socket input stream
 * 2. If it is a PingRequest: responds AUTOMATICALLY with a PongResponse
 *    (the client does not need to do anything, it is all automatic)
 * 3. If it is an Update: updates the ClientModel accordingly
 * 4. If the socket closes: stops the thread
 *
 * PING HANDLING (transparent to the client):
 * When the server sends a PingRequest, the client responds immediately
     * with a PongResponse containing the same timestamp. This allows the
 * server calculates round-trip time and detects disconnections.
 *
 * ============================================================================
 */
public class ServerHandler implements Runnable {

    /** Stream di input per ricevere messaggi dal server */
    private final ObjectInputStream in;

    /** Output stream to respond to pings */
    private final ObjectOutputStream out;

    /** Local client model: updated with received updates */
    private final ClientModel clientModel;

    /** Status flag: true = listening, false = stopped */
    private volatile boolean running = true;

    /**
     * Constructor.
     *
     * @param in input stream from the server
     * @param out output stream to the server (used to respond to pings)
     * @param clientModel local model to update
     */
    public ServerHandler(ObjectInputStream in, ObjectOutputStream out, ClientModel clientModel) {
        this.in = in;
        this.out = out;
        this.clientModel = clientModel;
    }

    /**
     * Main loop: reads messages from the server while running == true.
     *
     * For each received message:
     * - If PingRequest -> respond with PongResponse (automatic)
     * - If it is an Update -> updates the ClientModel
     * - If exception -> stops the thread
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
                // IF THE SERVER GOES DOWN, IT ENTERS HERE:
                if (running) {
                    System.err.println("[SOCKET] Connessione persa, avvio auto-reconnect...");
                    clientModel.triggerConnectionLost();
                    stop();
                }
            }
        }
    }

    /**
     * Automatically responds to a server PingRequest.
     * Sends a PongResponse with the same received timestamp.
     *
     * This is completely transparent to the client: the player
     * sees nothing, the ping/pong happens in the background.
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
     * Routes Updates received from the server to the ClientModel.
     *
     * POLYMORPHIC DISPATCH: message.applyTo(clientModel) delegates to each subtype
     * its own update logic, without switch or reflection.
     *
     * EXCEPTIONS (handled before dispatch):
     * - PingRequest: automatic response at socket level (transparent to the client)
     * - StateChangedUpdate: after applyTo(), checks if the GUI should transition
     *   from lobby scene to game scene (UI responsibility separated from model)
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
