package it.polimi.ingsw.am54.network.messages;

public class PongResponse implements Request {
    private final long timestamp;

    public PongResponse(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
