package it.polimi.ingsw.am54.network.messages;

/**
 * Rappresenta un'azione o una richiesta inviata dal giocatore al server.
 * CLIENT -> SERVER
 */
public interface Request extends ClientMessage {
    String getType();
}