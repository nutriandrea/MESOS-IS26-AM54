package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

public class EventHunter implements IEffect<EventCard> {

    @Override
    public void apply(Player p, Game g, EventCard ec) {
        g.getPlayers().forEach(pl -> {
            long cacciatori = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count();

            int buildingAura = pl.getEventCostAura();

            pl.modifyFood((int) cacciatori * ec.getBaseFood());
            pl.modifyAuraPoints((int) cacciatori * ec.getAuraPerCharacter() + buildingAura);
            pl.resetEventAttributes();
        });
    }
}