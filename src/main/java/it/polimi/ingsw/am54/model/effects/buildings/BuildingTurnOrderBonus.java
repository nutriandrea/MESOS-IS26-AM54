package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

// Handles the additional food obtained by placing the Totem on the bonus spaces of the track
public class BuildingTurnOrderBonus implements IEffect<BuildingCard> {

    /**
     * At the end of the turn, if the player occupies a turn order slot with a positive
     * food bonus, credits them with 1 additional food.
     *
     * @param p    the player who owns the building
     * @param g    the current game
     * @param card the building that possesses the effect
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