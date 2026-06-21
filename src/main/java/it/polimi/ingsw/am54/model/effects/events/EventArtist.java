package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Effect of the Artist event (Cave Paintings): evaluates the number of
 * Artist characters in each player&rsquo;s tribe. If the number
 * is below the threshold, the player suffers a penalty;
 * otherwise, they gain bonus Aura points based on the event&rsquo;s multiplier.
 *
 * @author AM54
 */
public class EventArtist implements IEffect<EventCard> {

    // UPDATED SIGNATURE FOR THE EVENT PHASE
    /**
     * Resolves the Cave Paintings event by evaluating the number of Artist
     * characters in each player's tribe. Players below the threshold suffer
     * a penalty; those at or above it gain bonus Aura points.
     *
     * @param p            the player who triggered the event (unused, affects all)
     * @param g            the current game instance
     * @param ec           the event card being resolved
     * @param currentEvent the event currently being processed
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            long painters = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.ARTIST).count();

            // Reads the temporary modifiers set by buildings (if any)
            int buildingFood = pl.getEventCostFood();
            if (buildingFood > 0) pl.modifyFood(buildingFood);

            pl.modifyAuraPoints(painters < ec.getThreshold()
                    ? -ec.getPenalty()
                    : (int) painters * ec.getBonusMultiplier());

            // Resets modifiers at end of event
            pl.resetEventAttributes();
        });
    }
}