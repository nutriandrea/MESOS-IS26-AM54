package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Immediate resource: provides Aura points and/or Magic points instantly
 * at the time of the building card purchase.
 *
 * @author AM54
 */
public class BuildingImmediateResource implements IEffect<BuildingCard> {
    private final int auraBonus;
    private final int magicBonus;

    /**
     * Constructs the effect reading the Aura and Magic bonuses from JSON.
     *
     * @param params JSON object with optional "aura" and "magic" integer fields,
     *               may be {@code null}
     */
    public BuildingImmediateResource(JsonObject params) {
        this.auraBonus = params != null && params.has("aura") ? params.get("aura").getAsInt() : 0;
        this.magicBonus = params != null && params.has("magic") ? params.get("magic").getAsInt() : 0;
    }

    /**
     * Instantly grants the configured Aura and Magic point bonuses to the
     * player at the time of building purchase.
     *
     * @param p    the player purchasing the building
     * @param g    the current game instance
     * @param card the building card providing this effect
     */
    @Override
    public void applyOnPurchase(Player p, Game g, BuildingCard card) {
        if (auraBonus > 0) p.modifyAuraPoints(auraBonus);
        if (magicBonus > 0) p.modifyMagicPoints(magicBonus);
    }
}