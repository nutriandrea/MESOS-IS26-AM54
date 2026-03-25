package model.effects.buildings;

import com.fasterxml.jackson.databind.JsonNode;
import model.*;

/**
 * Gestisce tutti gli effetti della fase EVENT e STARTTURN.
 */
public class EventPhaseEffect implements IEffect<BuildingCard> {

    private final String effectName;
    private final JobType jobType;
    private final int multiplier;
    private final boolean food;
    private final boolean aura;

    public EventPhaseEffect(String effectName, JsonNode params) {
        this.effectName = effectName;
        this.jobType = params != null && params.has("jobType")
                ? JobType.valueOf(params.get("jobType").asText()) : null;
        this.multiplier = params != null && params.has("multiplier")
                ? params.get("multiplier").asInt() : 0;
        this.food = params != null && params.has("food")
                ? params.get("food").asBoolean() : false;
        this.aura = params != null && params.has("aura")
                ? params.get("aura").asBoolean() : false;
    }

    public String getEffectName() {
        return effectName;
    }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        switch (effectName) {
            case "EVENT_FOOD_DISCOUNT" -> {
                long count = p.getMyTribe().stream()
                        .filter(c -> c.getJobType() == jobType).count();
                p.setEventCostFood((int) (count * multiplier));
            }
            case "EVENT_BONUS" -> {
                long count = p.getMyTribe().stream()
                        .filter(c -> c.getJobType() == jobType).count();
                if (food) p.setEventCostFood((int) count);
                if (aura) p.setEventCostAura((int) count);
            }
            case "NO_LOSS" -> { /* marker: controllato dall'evento SHAMAN */ }
            case "DOUBLE_AURA_SHAMAN" -> { /* marker: controllato dall'evento SHAMAN */ }
        }
    }
}
