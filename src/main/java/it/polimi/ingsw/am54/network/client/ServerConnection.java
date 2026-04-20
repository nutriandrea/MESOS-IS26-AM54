package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.Request;

/**
 * Interfaccia generica per la connessione verso il Server.
 * Nasconde se stiamo usando Socket o RMI.
 */
public interface ServerConnection {
    void sendRequest(Request request);
    void disconnect();
}
