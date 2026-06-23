package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Constructor's effect: upon purchase, increases the player's permanent discount
 * on buildings by a value equal to the card's level.
 */
public class CharacterConstructor implements IEffect<CharacterCard> {

    /**
     * Credits the player with a building discount equal to the constructor's level.
     *
     * @param p    the player who purchased the card
     * @param g    the current game
     * @param card the newly picked constructor
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        p.modifyMyBuildingDiscount(card.getLevel());
    }
}