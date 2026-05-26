package it.polimi.ingsw.am54.network.messages;

/**
 * Rappresenta una notifica di cambiamento dello stato del gioco inviata dal server.
 * SERVER -> CLIENT
 */
public interface Update extends ClientMessage {
    String getType();
}