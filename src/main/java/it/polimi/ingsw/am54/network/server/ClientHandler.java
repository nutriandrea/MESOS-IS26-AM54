package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.network.messages.ClientMessage;
import it.polimi.ingsw.am54.network.messages.Update;

/**
 * Interfaccia che astrae la comunicazione bidirezionale con un singolo client.
 * Il resto del sistema (es. il Controller) userà questa interfaccia per parlare
 * con la rete senza conoscerne i dettagli implementativi (Socket, RMI, ecc.).
 * E il contratto astratto che definisce le modalit`a di comunicazione tra il
 * server e un singolo client.
 */
public interface ClientHandler {

    /**
     * Invia un update DAL SERVER AL CLIENT.
     */
    void sendMessage(Update message);

    /**
     * Invia un messaggio di errore incapsulato al client.
     */
    void sendError(String errorMessage);

    /**
     * Chiude la connessione con questo client.
     */
    void disconnect();
}