package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Risposta del Client al PingRequest del Server.
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
        // PongResponse e' gestita a livello socket prima del dispatch;
        // questo metodo non viene mai chiamato per questo tipo.
    }
}
