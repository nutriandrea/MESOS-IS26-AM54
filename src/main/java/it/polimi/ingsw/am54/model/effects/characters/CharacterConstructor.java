package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Effect of the Constructor character: on purchase, modifies the discount
 * of the player for purchasing buildings based on the character level.
 *
 * @author AM54
 */
public class CharacterConstructor implements IEffect<CharacterCard> {

    /**
     * Increases the player's building purchase discount by the level
     * of the Constructor character just acquired.
     *
     * @param p    the player who purchased the character
     * @param g    the current game instance
     * @param card the Constructor character card that was purchased
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        p.modifyMyBuildingDiscount(card.getLevel());
    }
}