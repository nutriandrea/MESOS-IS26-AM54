package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * "Passive" effect shared by Artist, Inventor, and Gatherer: they produce
 * nothing upon purchase, they only count as statistics for endgame scoring and
 * events. The implementation is intentionally empty.
 */
public class CharacterPassive implements IEffect<CharacterCard> {
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        // Artist, Inventor and Farmer cards have no immediate effects on the board.
        // They exist only for passive statistics, so this method remains empty!
    }
}