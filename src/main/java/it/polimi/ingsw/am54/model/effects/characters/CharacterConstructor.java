package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Effetto del Costruttore: all'acquisto aumenta lo sconto permanente del giocatore
 * sugli edifici, di un valore pari al livello della carta.
 */
public class CharacterConstructor implements IEffect<CharacterCard> {

    /**
     * Accredita al giocatore uno sconto edifici pari al livello del costruttore.
     *
     * @param p    il giocatore che ha acquistato la carta
     * @param g    la partita corrente
     * @param card il costruttore appena pescato
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        p.modifyMyBuildingDiscount(card.getLevel());
    }
}
