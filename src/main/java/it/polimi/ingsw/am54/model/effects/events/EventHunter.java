package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Hunting event effect: rewards each player based on the number of hunters
 * in their tribe, taking into account any temporary modifiers set
 * by buildings.
 */
public class EventHunter implements IEffect<EventCard> {

    /**
     * For each player credits food and aura proportional to the number of hunters
     * (plus temporary building bonuses), then resets temporary modifiers.
     *
     * @param p            not used (the effect iterates over all players)
     * @param g            the current game
     * @param ec           the event card owning the effect
     * @param currentEvent the event being resolved
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            long cacciatori = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count();

            // Reads temporary modifiers set by buildings (if any)
            int buildingAura = pl.getEventCostAura();
            int buildingFood = pl.getEventCostFood();

            pl.modifyFood((int) cacciatori * ec.getBaseFood() + buildingFood);
            pl.modifyAuraPoints((int) cacciatori * ec.getAuraPerCharacter() + buildingAura);

            // Resets modifiers at the end of the event
            pl.resetEventAttributes();
        });
    }
}