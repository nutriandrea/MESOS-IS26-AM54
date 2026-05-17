package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

public class BuildingEventBonus implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final boolean food;
    private final boolean aura;

    public BuildingEventBonus(JsonNode params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").asText()) : null;
        this.food = params != null && params.has("food") && params.get("food").asBoolean();
        this.aura = params != null && params.has("aura") && params.get("aura").asBoolean();
    }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        long count = p.getMyTribe().stream().filter(c -> c.getJobType() == jobType).count();
        if (food) p.setEventCostFood((int) count);
        if (aura) p.setEventCostAura((int) count);
    }
}