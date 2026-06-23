package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Effetto dello Sciamano: all'acquisto fornisce punti magia pari al livello della carta.
 */
public class CharacterShaman implements IEffect<CharacterCard> {

    /**
     * Accredita al giocatore punti magia pari al livello dello sciamano.
     *
     * @param p    il giocatore che ha acquistato la carta
     * @param g    la partita corrente
     * @param card lo sciamano appena pescato
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        p.modifyMagicPoints(card.getLevel());
    }
}
