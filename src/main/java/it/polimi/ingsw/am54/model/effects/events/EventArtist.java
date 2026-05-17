package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

public class EventArtist implements IEffect<EventCard> {

    @Override
    public void apply(Player p, Game g, EventCard ec) {
        g.getPlayers().forEach(pl -> {
            long painters = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.ARTIST).count();

            pl.modifyAuraPoints(painters <= ec.getThreshold()
                    ? -ec.getPenalty()
                    : (int) painters * ec.getBonusMultiplier());

            pl.resetEventAttributes();
        });
    }
}