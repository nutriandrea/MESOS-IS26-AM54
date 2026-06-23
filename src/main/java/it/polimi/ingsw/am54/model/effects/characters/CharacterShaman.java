package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Shaman's effect: upon purchase, provides magic points equal to the card's level.
 */
public class CharacterShaman implements IEffect<CharacterCard> {

    /**
     * Credits the player with magic points equal to the shaman's level.
     *
     * @param p    the player who purchased the card
     * @param g    the current game
     * @param card the newly picked shaman
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        p.modifyMagicPoints(card.getLevel());
    }
}