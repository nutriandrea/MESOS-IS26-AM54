package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Effect of the Hunter event (Hunt): for each Hunter character
 * in the player&rsquo;s tribe, bonus food and Aura points are granted.
 * Any temporary modifiers set by buildings are
 * applied before the event resolution.
 *
 * @author AM54
 */
public class EventHunter implements IEffect<EventCard> {

    // UPDATED SIGNATURE FOR THE EVENT PHASE
    /**
     * Resolves the Hunt event by granting each player bonus food and Aura
     * points proportional to the number of Hunters in their tribe,
     * plus any temporary modifiers set by buildings.
     *
     * @param p            the player who triggered the event (unused, affects all)
     * @param g            the current game instance
     * @param ec           the event card being resolved
     * @param currentEvent the event currently being processed
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            long hunters = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count();

            // Reads the temporary modifiers set by buildings (if any)
            int buildingAura = pl.getEventCostAura();
            int buildingFood = pl.getEventCostFood();

            pl.modifyFood((int) hunters * ec.getBaseFood() + buildingFood);
            pl.modifyAuraPoints((int) hunters * ec.getAuraPerCharacter() + buildingAura);

            // Resets modifiers at end of event
            pl.resetEventAttributes();
        });
    }
}