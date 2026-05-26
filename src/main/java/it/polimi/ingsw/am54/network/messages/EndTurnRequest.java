package it.polimi.ingsw.am54.network.messages;

/**
 * Richiesta inviata dal Client per terminare volontariamente il turno.
 */
public class EndTurnRequest implements Request {
    // Vuota: il server deduce il mittente dal Socket/RMI

    public static final String TYPE = "end_turn";
    @Override public String getType() { return TYPE; }
}
