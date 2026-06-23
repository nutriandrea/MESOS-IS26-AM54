package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Effetto dell'evento Caccia: premia ogni giocatore in base al numero di cacciatori
 * nella sua tribù, tenendo conto degli eventuali modificatori temporanei impostati
 * dagli edifici.
 */
public class EventHunter implements IEffect<EventCard> {

    /**
     * Per ogni giocatore accredita cibo e aura proporzionali al numero di cacciatori
     * (più i bonus temporanei degli edifici), poi azzera i modificatori temporanei.
     *
     * @param p            non usato (l'effetto itera su tutti i giocatori)
     * @param g            la partita corrente
     * @param ec           la carta evento proprietaria dell'effetto
     * @param currentEvent l'evento in risoluzione
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            long cacciatori = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count();

            // Legge i modificatori temporanei impostati dagli edifici (se ci sono)
            int buildingAura = pl.getEventCostAura();
            int buildingFood = pl.getEventCostFood();

            pl.modifyFood((int) cacciatori * ec.getBaseFood() + buildingFood);
            pl.modifyAuraPoints((int) cacciatori * ec.getAuraPerCharacter() + buildingAura);

            // Resetta i modificatori a fine evento
            pl.resetEventAttributes();
        });
    }
}
