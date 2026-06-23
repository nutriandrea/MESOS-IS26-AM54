package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Client's response to the Server's PingRequest.
 * CLIENT -> SERVER
 */
public final class PongResponse implements Request {
    private final long timestamp;

    public PongResponse(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        // PongResponse is handled at the socket level before dispatch;
        // this method is never called for this type.
    }
}