package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Heartbeat inviato dal Server al Client per verificare la connessione attiva.
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
        // PingRequest e' gestita a livello socket prima del dispatch;
        // questo metodo non viene mai chiamato per questo tipo.
    }
}
