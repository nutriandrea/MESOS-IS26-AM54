package it.polimi.ingsw.am54.network.messages;

/**
 * Notifica broadcast a tutti i client che il gioco e' stato ripreso
 * dopo che un secondo giocatore si e' riconnesso.
 * SERVER -> CLIENT
 */
public class GameResumedUpdate implements Update {

    public static final String TYPE = "game_resumed";
    @Override public String getType() { return TYPE; }
}
