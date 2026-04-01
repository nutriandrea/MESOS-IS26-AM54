package it.polimi.ingsw.am54.network.server;

import java.io.Serializable;

/**
 * Gestisce la comunicazione bidirezionale con un singolo client[cite: 118, 125].
 */
public interface ClientHandler {
    /**
     * Invia un update dal server al client
     */
    void sendMessage(Serializable message);

    /**
     * Invia un errore specifico al client
     */
    void sendError(String errorMessage);
}
