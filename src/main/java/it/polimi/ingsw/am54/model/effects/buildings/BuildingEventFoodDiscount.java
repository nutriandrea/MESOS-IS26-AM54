package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

public class BuildingEventFoodDiscount implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final int multiplier;

    public BuildingEventFoodDiscount(JsonNode params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").asText()) : null;
        this.multiplier = params != null && params.has("multiplier") ? params.get("multiplier").asInt() : 0;
    }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        long count = p.getMyTribe().stream().filter(c -> c.getJobType() == jobType).count();
        p.setEventCostFood((int) (count * multiplier));
    }
}