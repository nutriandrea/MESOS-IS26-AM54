package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Modifies the cost of events for players. Handles:
 * <ul>
 *   <li>Food discounts during the Sustenance Event for Artists, Inventors and Gatherers</li>
 *   <li>Additional food or aura bonuses for specific events (Hunt, Cave Paintings)</li>
 * </ul>
 *
 * @author AM54
 */
public class BuildingEventCostModifier implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final int foodDiscountMultiplier;
    private final boolean overrideFoodCost;
    private final boolean overrideAuraCost;

    /**
     * Constructs the effect reading the job type, food discount multiplier,
     * and override flags from the JSON parameters.
     *
     * @param params JSON object with optional "jobType", "foodDiscountMultiplier",
     *               "overrideFoodCost", and "overrideAuraCost" fields, may be {@code null}
     */
    public BuildingEventCostModifier(JsonObject params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").getAsString()) : null;
        this.foodDiscountMultiplier = params != null && params.has("foodDiscountMultiplier") ? params.get("foodDiscountMultiplier").getAsInt() : 0;
        this.overrideFoodCost = params != null && params.has("overrideFoodCost") && params.get("overrideFoodCost").getAsBoolean();
        this.overrideAuraCost = params != null && params.has("overrideAuraCost") && params.get("overrideAuraCost").getAsBoolean();
    }

    // UPDATED SIGNATURE FOR THE EVENT PHASE
    /**
     * Modifies the player's temporary event cost modifiers (food and/or Aura)
     * based on the count of matching job-type characters and the event type.
     *
     * @param p            the player whose costs are being modified
     * @param g            the current game instance
     * @param card         the building card providing this effect
     * @param currentEvent the event currently being resolved
     */
    @Override
    public void applyOnEvent(Player p, Game g, BuildingCard card, EventCard currentEvent) {
        if (jobType == null || currentEvent == null) return;

        long count = p.getMyTribe().stream().filter(c -> c.getJobType() == jobType).count();

        // 1. Discounts for the Sustenance Event (FARMER)
        // These discounts always use a negative multiplier in JSON.
        // Special case: Sustenance (Arbitrary Building <-> Event binding) so use "FARMER"
        if (foodDiscountMultiplier < 0 && currentEvent.isSustenanceEvent()) {
            p.setEventCostFood(p.getEventCostFood() + (int) (count * foodDiscountMultiplier));
        }

        // 2. Food Bonus for specific Events (e.g. Hunt, Cave Paintings)
        // The building reacts ONLY if the event name matches its JobType (HUNTER or ARTIST)
        if ((overrideFoodCost || foodDiscountMultiplier > 0) && currentEvent.matchesJobType(jobType)) {
            int multi = foodDiscountMultiplier > 0 ? foodDiscountMultiplier : 1;
            p.setEventCostFood(p.getEventCostFood() + (int) (count * multi));
        }

        // 3. Aura Points Bonus (e.g. Hunter Building)
        if (overrideAuraCost && currentEvent.matchesJobType(jobType)) {
            p.setEventCostAura(p.getEventCostAura() + (int) count);
        }
    }
}