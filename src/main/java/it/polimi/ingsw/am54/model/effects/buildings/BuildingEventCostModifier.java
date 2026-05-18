package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

// Gestisce le 3 carte di Era 1 e 2 che danno sconti sul cibo durante l'Evento Sostentamento
// (per Artisti, Inventori e Raccoglitori) e le 2 carte che alterano gli eventi Caccia e Pitture Rupestri
// aggiungendo cibo o aura extra
public class BuildingEventCostModifier implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final int foodDiscountMultiplier;
    private final boolean overrideFoodCost;
    private final boolean overrideAuraCost;

    public BuildingEventCostModifier(JsonNode params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").asText()) : null;
        this.foodDiscountMultiplier = params != null && params.has("foodDiscountMultiplier") ? params.get("foodDiscountMultiplier").asInt() : 0;
        this.overrideFoodCost = params != null && params.has("overrideFoodCost") && params.get("overrideFoodCost").asBoolean();
        this.overrideAuraCost = params != null && params.has("overrideAuraCost") && params.get("overrideAuraCost").asBoolean();
    }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        long count = jobType != null ? p.getMyTribe().stream().filter(c -> c.getJobType() == jobType).count() : 0;

        if (foodDiscountMultiplier < 0) p.setEventCostFood((int) (count * foodDiscountMultiplier));
        if (overrideFoodCost) p.setEventCostFood((int) count);
        if (overrideAuraCost) p.setEventCostAura((int) count);
    }
}