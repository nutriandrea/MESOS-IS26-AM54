package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

public class BuildingTurnOrderBonus implements IEffect<BuildingCard> {
    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        TurnOrderSlot playerSlot = g.getBoard().getTurnOrderTile().stream()
                .filter(slot -> slot.getOccupiedBy() != null && slot.getOccupiedBy().equals(p))
                .findFirst().orElse(null);

        if (playerSlot != null && playerSlot.getBonusFood() > 0) {
            p.modifyFood(1);
        }
    }
}