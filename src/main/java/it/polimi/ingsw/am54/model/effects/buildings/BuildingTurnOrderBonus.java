package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

// Gestisce il cibo addizionale ottenuto piazzando il Totem sugli spazi bonus del tracciato
public class BuildingTurnOrderBonus implements IEffect<BuildingCard> {

    /**
     * A fine turno, se il giocatore occupa uno slot dell'ordine di turno con bonus
     * cibo positivo, gli accredita 1 cibo aggiuntivo.
     *
     * @param p    il giocatore proprietario dell'edificio
     * @param g    la partita corrente
     * @param card l'edificio che possiede l'effetto
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