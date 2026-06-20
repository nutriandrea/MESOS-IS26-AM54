package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Heartbeat sent by the Server to the Client to verify the active connection.
 * SERVER -> CLIENT
 */
public final class PingRequest implements Update {
    private final long timestamp;

    /**
     * Constructs a ping request with the given timestamp.
     *
     * @param timestamp the time at which the ping was sent
     */
    public PingRequest(long timestamp) {
        this.timestamp = timestamp;
    }

    /** @return the ping timestamp */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * No-op: ping requests are handled at the socket level before dispatch,
     * so this method is never invoked.
     *
     * @param model the client-side model (unused)
     */
    @Override
    public void applyTo(ClientModel model) {
        // PingRequest is handled at socket level before dispatch;
        // this method is never called for this type.
    }
}
