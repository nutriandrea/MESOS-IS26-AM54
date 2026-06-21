package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Endgame Aura: grants an Aura point bonus at the end of the game.
 * The bonus value is defined by the JSON parameters of the building card.
 *
 * @author AM54
 */
public class BuildingEndgameAura implements IEffect<BuildingCard> {
    private final int auraBonus;

    /**
     * Constructs the effect reading the Aura bonus from the JSON parameters.
     *
     * @param params JSON object containing the "aura" bonus value, may be {@code null}
     */
    public BuildingEndgameAura(JsonObject params) {
        this.auraBonus = params != null && params.has("aura") ? params.get("aura").getAsInt() : 0;
    }

    /**
     * Grants the configured Aura point bonus to the player at the end of the game.
     *
     * @param p    the player receiving the bonus
     * @param g    the current game instance
     * @param card the building card providing this effect
     */
    @Override
    public void applyOnEndGame(Player p, Game g, BuildingCard card) {
        if (auraBonus > 0) p.modifyAuraPoints(auraBonus);
    }
}