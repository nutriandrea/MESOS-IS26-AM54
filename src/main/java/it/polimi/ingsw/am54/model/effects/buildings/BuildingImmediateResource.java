package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

public class BuildingImmediateResource implements IEffect<BuildingCard> {
    private final int auraBonus;
    private final int magicBonus;

    public BuildingImmediateResource(JsonObject params) {
        this.auraBonus = params != null && params.has("aura") ? params.get("aura").getAsInt() : 0;
        this.magicBonus = params != null && params.has("magic") ? params.get("magic").getAsInt() : 0;
    }

    @Override
    public void applyOnPurchase(Player p, Game g, BuildingCard card) {
        if (auraBonus > 0) p.modifyAuraPoints(auraBonus);
        if (magicBonus > 0) p.modifyMagicPoints(magicBonus);
    }
}