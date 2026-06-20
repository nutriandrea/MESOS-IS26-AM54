package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Response from the Client to the Server's PingRequest.
 * CLIENT -> SERVER
 */
public final class PongResponse implements Request {
    private final long timestamp;

    /**
     * Constructs a pong response echoing the ping timestamp.
     *
     * @param timestamp the timestamp received in the originating ping
     */
    public PongResponse(long timestamp) {
        this.timestamp = timestamp;
    }

    /** @return the echoed ping timestamp */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * No-op: pong responses are handled at the socket level before dispatch,
     * so this method is never invoked.
     *
     * @param controller      unused
     * @param playerNickname  unused
     * @param connection       unused
     */
    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        // PongResponse is handled at socket level before dispatch;
        // this method is never called for this type.
    }
}
