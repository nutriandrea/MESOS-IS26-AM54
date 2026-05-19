package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

public class BuildingEndgameAura implements IEffect<BuildingCard> {
    private final int auraBonus;

    public BuildingEndgameAura(JsonNode params) {
        this.auraBonus = params != null && params.has("aura") ? params.get("aura").asInt() : 0;
    }

    @Override
    public void applyOnEndGame(Player p, Game g, BuildingCard card) {
        if (auraBonus > 0) p.modifyAuraPoints(auraBonus);
    }
}