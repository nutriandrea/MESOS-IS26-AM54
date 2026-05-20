package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

public class EventArtist implements IEffect<EventCard> {

    // FIRMA AGGIORNATA PER LA FASE EVENTI
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            long painters = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.ARTIST).count();

            // Legge i modificatori temporanei impostati dagli edifici (se ci sono)
            int buildingFood = pl.getEventCostFood();
            if (buildingFood > 0) pl.modifyFood(buildingFood);

            pl.modifyAuraPoints(painters < ec.getThreshold()
                    ? -ec.getPenalty()
                    : (int) painters * ec.getBonusMultiplier());

            // Resetta i modificatori a fine evento
            pl.resetEventAttributes();
        });
    }
}