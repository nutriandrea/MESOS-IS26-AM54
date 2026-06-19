package it.polimi.ingsw.am54.network.client;

/**
 * Variazione delle risorse di un giocatore dopo un evento.
 * Contiene i valori pre-evento e le differenze applicate (diff).
 */
public class PlayerDelta {
    public String nickname;
    public int oldFood, oldAura, oldMagic;
    public int diffFood, diffAura, diffMagic;
}