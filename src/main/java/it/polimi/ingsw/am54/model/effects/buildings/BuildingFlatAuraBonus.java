package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

public class BuildingFlatAuraBonus implements IEffect<BuildingCard> {
    private final int amount;
    public BuildingFlatAuraBonus(JsonNode params) {
        this.amount = params != null && params.has("amount") ? params.get("amount").asInt() : 0;
    }

    @Override public boolean isEndgameEffect() { return true; }
    @Override public void apply(Player p, Game g, BuildingCard card) {
        p.modifyAuraPoints(amount);
    }
}