package it.polimi.ingsw.am54.network.messages;

/**
 * Heartbeat inviato dal Server al Client per verificare la connessione attiva.
 * SERVER -> CLIENT
 */
public class PingRequest implements Update {
    private final long timestamp;

    public PingRequest(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static final String TYPE = "ping";
    @Override public String getType() { return TYPE; }
}
