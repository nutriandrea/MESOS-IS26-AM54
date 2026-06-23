package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Building effect: credits a fixed aura bonus during the endgame scoring.
 */
public class BuildingEndgameAura implements IEffect<BuildingCard> {
    private final int auraBonus;

    public BuildingEndgameAura(JsonObject params) {
        this.auraBonus = params != null && params.has("aura") ? params.get("aura").getAsInt() : 0;
    }

    /**
     * Adds the configured aura bonus to the player's score.
     *
     * @param p    the player who owns the building
     * @param g    the current game
     * @param card the building that possesses the effect
     */
    @Override
    public void applyOnEndGame(Player p, Game g, BuildingCard card) {
        if (auraBonus > 0) p.modifyAuraPoints(auraBonus);
    }
}