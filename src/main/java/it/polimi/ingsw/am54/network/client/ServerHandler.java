package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ============================================================================
 * ServerHandler — Client-side asynchronous listening thread (TCP Socket)
 * ============================================================================
 *
 * ROLE:
 * Independent thread continuously listening for incoming messages from the
 * server on the TCP Socket connection.
 *
 * WHAT IT DOES:
 * 1. Reads objects from the socket's input stream
 * 2. If it is a PingRequest: automatically answers with a PongResponse
 * (client does nothing, it's automatic)
 * 3. If it is an Update: updates the ClientModel accordingly
 * 4. If the socket closes: stops the thread
 *
 * PING MANAGEMENT (transparent to the client):
 * When the server sends a PingRequest, the client immediately responds
 * with a PongResponse containing the same timestamp. This allows the
 * server to calculate the round-trip time and detect disconnections.
 *
 * ============================================================================
 */
public class ServerHandler implements Runnable {

    /** Input stream to receive messages from the server */
    private final ObjectInputStream in;

    /** Output stream to respond to pings */
    private final ObjectOutputStream out;

    /** Local client model: is updated with received updates */
    private final ClientModel clientModel;

    /** Status flag: true = listening, false = stopped */
    private volatile boolean running = true;

    /**
     * Constructor.
     *
     * @param in input stream from the server
     * @param out output stream towards the server (used to respond to pings)
     * @param clientModel local model to update
     */
    public ServerHandler(ObjectInputStream in, ObjectOutputStream out, ClientModel clientModel) {
        this.in = in;
        this.out = out;
        this.clientModel = clientModel;
    }

    /**
     * Main loop: reads messages from the server as long as running == true.
     * * For each received message:
     * - If PingRequest -> respond with PongResponse (automatic)
     * - If Update -> update ClientModel
     * - If exception -> stop the thread
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
                // IF THE SERVER CRASHES, ENTERS HERE:
                if (running) {
                    System.err.println("[SOCKET] Connessione persa, avvio auto-reconnect...");
                    clientModel.triggerConnectionLost();
                    stop();
                }
            }
        }
    }

    /**
     * Automatically responds to a PingRequest from the server.
     * Sends a PongResponse with the received timestamp.
     *
     * This is completely transparent to the client: the player
     * sees nothing, ping/pong happens in the background.
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

    /** Stops the listening thread */
    public void stop() { this.running = false; }

    /**
     * Dispatches Updates received from the server to the ClientModel.
     *
     * POLYMORPHIC DISPATCH: message.applyTo(clientModel) delegates the update logic to
     * each subtype, without switch or reflection.
     *
     * EXCEPTIONS (handled before dispatch):
     * - PingRequest: automatic response at socket level (transparent to client)
     * - StateChangedUpdate: after applyTo(), checks if GUI must switch
     * from lobby scene to game scene (UI responsibility separated from model)
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