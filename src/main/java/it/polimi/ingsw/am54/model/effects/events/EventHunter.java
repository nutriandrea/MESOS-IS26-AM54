package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

public class EventHunter implements IEffect<EventCard> {

    // FIRMA AGGIORNATA PER LA FASE EVENTI
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