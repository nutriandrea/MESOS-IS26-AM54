package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Heartbeat sent by the Server to the Client to verify the active connection.
 * SERVER -> CLIENT
 */
public final class PingRequest implements Update {
    private final long timestamp;

    public PingRequest(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyTo(ClientModel model) {
        // PingRequest is handled at the socket level before dispatch;
        // this method is never called for this type.
    }
}