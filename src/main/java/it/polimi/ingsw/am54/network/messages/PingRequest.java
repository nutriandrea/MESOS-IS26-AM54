package it.polimi.ingsw.am54.network.messages;

public class PingRequest implements Update {
    private final long timestamp;

    public PingRequest() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
