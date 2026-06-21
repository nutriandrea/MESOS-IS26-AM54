package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Manages immunity to point loss (No Loss) and point doubling (Double Aura)
 * during the Shamanic Ritual Event.
 *
 * @author AM54
 */
public class BuildingShamanPassive implements IEffect<BuildingCard> {
    private final boolean noLoss;
    private final boolean doubleAura;

    /**
     * Constructs the effect reading the no-loss and double-Aura flags from JSON.
     *
     * @param params JSON object that may contain "noLoss" and "doubleAura" booleans,
     *               may be {@code null}
     */
    public BuildingShamanPassive(JsonObject params) {
        this.noLoss = params != null && params.has("noLoss") && params.get("noLoss").getAsBoolean();
        this.doubleAura = params != null && params.has("doubleAura") && params.get("doubleAura").getAsBoolean();
    }

    /**
     * Activates the shamanic protection flags on the player before the
     * Shamanic Ritual Event is resolved.
     *
     * @param p            the player receiving the protection
     * @param g            the current game instance
     * @param card         the building card providing this effect
     * @param currentEvent the event currently being resolved
     */
    @Override
    public void applyOnEvent(Player p, Game g, BuildingCard card, EventCard currentEvent) {
        // the player has these 2 flags
        if (noLoss) p.setShamanNoLoss(true);
        if (doubleAura) p.setShamanDoubleAura(true);
    }
}
