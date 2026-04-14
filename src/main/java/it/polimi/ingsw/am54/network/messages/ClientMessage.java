package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

/**
 * Interfaccia radice per TUTTI i messaggi che viaggiano sulla rete.
 * Essendo vuota, funge da "etichetta" (Marker Interface) per garantire
 * che solo oggetti consentiti vengano inviati tramite i Socket,
 * rendendoli automaticamente Serializzabili.
 */
public interface ClientMessage extends Serializable {
    // Nessun metodo qui dentro!
}