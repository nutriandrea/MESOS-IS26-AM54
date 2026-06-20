package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Effect of the passive character (Artist, Inventor, Farmer):
 * has no immediate effects on the game board at the time of purchase.
 * These characters exist exclusively for passive statistics
 * and to activate the effects of other buildings and events.
 *
 * @author AM54
 */
public class CharacterPassive implements IEffect<CharacterCard> {
    /**
     * No-op implementation; passive characters (Artist, Inventor, Farmer)
     * have no immediate effect at purchase time.
     *
     * @param p    the player who purchased the character
     * @param g    the current game instance
     * @param card the passive character card that was purchased
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        // Artist, Inventor and Farmer cards have no immediate effects on the board.
        // They exist only for passive statistics, so this method remains empty!
    }
}