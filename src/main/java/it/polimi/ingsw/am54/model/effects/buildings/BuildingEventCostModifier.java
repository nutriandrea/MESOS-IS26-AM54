package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

// Handles the 3 Era 1 and 2 cards that provide food discounts during the Sustenance Event (for Artists, Inventors and Gatherers)
// and the 2 cards that alter the Hunting and Cave Paintings events
// by adding extra food or aura
public class BuildingEventCostModifier implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final int foodDiscountMultiplier;
    private final boolean overrideFoodCost;
    private final boolean overrideAuraCost;

    public BuildingEventCostModifier(JsonObject params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").getAsString()) : null;
        this.foodDiscountMultiplier = params != null && params.has("foodDiscountMultiplier") ? params.get("foodDiscountMultiplier").getAsInt() : 0;
        this.overrideFoodCost = params != null && params.has("overrideFoodCost") && params.get("overrideFoodCost").getAsBoolean();
        this.overrideAuraCost = params != null && params.has("overrideAuraCost") && params.get("overrideAuraCost").getAsBoolean();
    }

    /**
     * Sets temporary modifiers on the player which are then read by the event effect:
     * food discount on Sustenance (for their own job), or extra food/aura
     * on Hunting/Cave Paintings events when the event matches the building type.
     *
     * @param p            the player who owns the building
     * @param g            the current game
     * @param card         the building that possesses the effect
     * @param currentEvent the event currently being resolved
     */
    @Override
    public void applyOnEvent(Player p, Game g, BuildingCard card, EventCard currentEvent) {
        if (jobType == null || currentEvent == null) return;

        long count = p.getMyTribe().stream().filter(c -> c.getJobType() == jobType).count();

        // 1. Discounts for the Sustenance Event (FARMER)
        // These discounts always use a negative multiplier in the JSON.
        // Special case: Sustenance (Arbitrary link Building <-> Event) so I use "FARMER"
        if (foodDiscountMultiplier < 0 && currentEvent.isSustenanceEvent()) {
            p.setEventCostFood(p.getEventCostFood() + (int) (count * foodDiscountMultiplier));
        }

        // 2. Food Bonus for specific Events (e.g. Hunting, Cave Paintings)
        // The building reacts ONLY if the event name matches its JobType (HUNTER or ARTIST)
        if ((overrideFoodCost || foodDiscountMultiplier > 0) && currentEvent.matchesJobType(jobType)) {
            int multi = foodDiscountMultiplier > 0 ? foodDiscountMultiplier : 1;
            p.setEventCostFood(p.getEventCostFood() + (int) (count * multi));
        }

        // 3. Aura Points Bonus (E.g. Hunter's Building)
        if (overrideAuraCost && currentEvent.matchesJobType(jobType)) {
            p.setEventCostAura(p.getEventCostAura() + (int) count);
        }
    }
}