package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Hunter's effect: upon purchase, provides food equal to the number of hunters
 * already owned by the player (including the one just picked).
 */
public class CharacterHunter implements IEffect<CharacterCard> {

    /**
     * Counts the hunters in the player's tribe and credits them with as much food.
     * Level 0 hunters produce no effect.
     *
     * @param p    the player who purchased the card
     * @param g    the current game
     * @param card the newly picked hunter
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