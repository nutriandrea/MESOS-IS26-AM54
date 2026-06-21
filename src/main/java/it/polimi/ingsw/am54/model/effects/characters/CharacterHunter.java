package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Effect of the Hunter character: on purchase, if the level of the
 * character is greater than zero, grants food based on the number
 * of total Hunters present in the player&rsquo;s tribe.
 *
 * @author AM54
 */
public class CharacterHunter implements IEffect<CharacterCard> {

    /**
     * Grants food equal to the total number of Hunters in the player's
     * tribe when a Hunter character of level greater than zero is purchased.
     *
     * @param p    the player who purchased the character
     * @param g    the current game instance
     * @param card the Hunter character card that was purchased
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        if (card.getLevel() > 0) {
            int totalHunters = (int) p.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count();
            p.modifyFood(totalHunters);
        }
    }
}