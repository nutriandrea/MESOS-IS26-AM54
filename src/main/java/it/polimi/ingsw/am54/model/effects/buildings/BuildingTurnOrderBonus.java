package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

// Gestisce il cibo addizionale ottenuto piazzando il Totem sugli spazi bonus del tracciato
public class BuildingTurnOrderBonus implements IEffect<BuildingCard> {

    // FIRMA AGGIORNATA PER LA FASE DI CHIUSURA TURNO
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