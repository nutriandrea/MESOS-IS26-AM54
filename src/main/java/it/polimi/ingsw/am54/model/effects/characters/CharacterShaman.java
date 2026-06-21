package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Effect of the Shaman character: on purchase, increases Magic points
 * of the player based on the character level.
 *
 * @author AM54
 */
public class CharacterShaman implements IEffect<CharacterCard> {

    /**
     * Increases the player's Magic points by the level of the
     * Shaman character just acquired.
     *
     * @param p    the player who purchased the character
     * @param g    the current game instance
     * @param card the Shaman character card that was purchased
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        p.modifyMagicPoints(card.getLevel());
    }
}