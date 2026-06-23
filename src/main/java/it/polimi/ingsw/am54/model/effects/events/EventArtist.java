package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Cave Paintings event effect: those who reach the artists threshold gain
 * proportional aura, those who do not reach it suffer a penalty.
 */
public class EventArtist implements IEffect<EventCard> {

    /**
     * For each player compares the number of artists with the event threshold:
     * below threshold applies the aura penalty, from the threshold up credits
     * {@code artists * multiplier}; finally resets temporary modifiers.
     *
     * @param p            not used (the effect iterates over all players)
     * @param g            the current game
     * @param ec           the event card owning the effect
     * @param currentEvent the event being resolved
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            long painters = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.ARTIST).count();

            // Reads temporary modifiers set by buildings (if any)
            int buildingFood = pl.getEventCostFood();
            if (buildingFood > 0) pl.modifyFood(buildingFood);

            pl.modifyAuraPoints(painters < ec.getThreshold()
                    ? -ec.getPenalty()
                    : (int) painters * ec.getBonusMultiplier());

            // Resets modifiers at the end of the event
            pl.resetEventAttributes();
        });
    }
}