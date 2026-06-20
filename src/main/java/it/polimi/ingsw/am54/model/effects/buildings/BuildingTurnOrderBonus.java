package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

/**
 * Turn order bonus: at the end of the player's turn, if their totem
 * is positioned on a bonus space of the turn order track,
 * allows 1 additional food.
 *
 * @author AM54
 */
public class BuildingTurnOrderBonus implements IEffect<BuildingCard> {

    // UPDATED SIGNATURE FOR THE END-OF-TURN PHASE
    /**
     * Grants 1 additional food at the end of the player's turn if their
     * totem occupies a bonus space on the turn order track.
     *
     * @param p    the player ending their turn
     * @param g    the current game instance
     * @param card the building card providing this effect
     */
    @Override
    public void applyOnTurnEnd(Player p, Game g, BuildingCard card) {
        TurnOrderSlot playerSlot = g.getBoard().getTurnOrderTile().stream()
                .filter(slot -> slot.getOccupiedBy() != null && slot.getOccupiedBy().equals(p))
                .findFirst().orElse(null);

        if (playerSlot != null && playerSlot.getBonusFood() > 0) {
            p.modifyFood(1);
        }
    }
}