package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.network.messages.Update;

/**
 * ============================================================================
 * ClientConnection — Astrazione del canale di comunicazione server -> client
 * ============================================================================
 *
 * RUOLO:
 * Interfaccia che astrae il trasporto di rete (Socket TCP o RMI).
 * Il Controller e la VirtualView usano questa interfaccia senza sapere
 * se stanno comunicando via Socket o RMI.
 *
 * IMPLEMENTAZIONI:
 * - SocketClientHandler: usa ObjectOutputStream per inviare Update
 * - RMIClientHandler: usa clientStub.receiveUpdate() per inviare Update
 *
 * METODI:
 * - sendUpdate(Update): invia un messaggio di update al client
 * - disconnect(): chiude la connessione e pulisce le risorse
 * - startHeartbeat(): avvia il sistema di ping/pong (default = no-op)
 *
 * NOTA SU startHeartbeat():
 * Il metodo ha un'implementazione di default vuota per retrocompatibilita'.
 * SocketClientHandler e RMIClientHandler lo sovrascrivono per avviare
 * il ping scheduler. Se una futura implementazione non ha bisogno di
 * heartbeat, puo' semplicemente non sovrascriverlo.
 * ============================================================================
 */
public interface ClientConnection {

    /**
     * Invia un Update al client.
     * L'implementazione specifica dipende dal tipo di connessione (Socket o RMI).
     *
     * @param update il messaggio da inviare
     */
    void sendUpdate(Update update);

    /**
     * Chiude la connessione e pulisce le risorse.
     * Chiama anche controller.removeClient() per deregistrare il giocatore.
     */
    void disconnect();

    /**
     * Avvia il sistema di heartbeat (ping/pong) per questa connessione.
     * Implementazione di default vuota per retrocompatibilita'.
     *
     * SocketClientHandler e RMIClientHandler sovrascrivono questo metodo
     * per avviare un ScheduledExecutorService che invia ping ogni 5 secondi.
     */
    default void startHeartbeat() {}
}
