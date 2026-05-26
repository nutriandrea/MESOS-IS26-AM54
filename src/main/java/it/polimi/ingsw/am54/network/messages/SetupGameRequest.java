package it.polimi.ingsw.am54.network.messages;

/**
 * Richiesta inviata dal Client per segnalare di essere pronto per il Setup.
 * Essendo un "Trigger", non ha bisogno di parametri: basta il suo arrivo!
 */
public class SetupGameRequest implements Request {

    // Classe volutamente vuota.
    // Nessun parametro necessario, nessun costruttore speciale richiesto.


    public static final String TYPE = "setup_game";
    @Override public String getType() { return TYPE; }
}