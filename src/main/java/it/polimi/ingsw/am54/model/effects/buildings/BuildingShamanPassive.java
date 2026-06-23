package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

// Handles immunity to point loss (No Loss) and point doubling (Double Aura)
// during the Shamanic Ritual Event
public class BuildingShamanPassive implements IEffect<BuildingCard> {
    private final boolean noLoss;
    private final boolean doubleAura;

    public BuildingShamanPassive(JsonObject params) {
        this.noLoss = params != null && params.has("noLoss") && params.get("noLoss").getAsBoolean();
        this.doubleAura = params != null && params.has("doubleAura") && params.get("doubleAura").getAsBoolean();
    }

    /**
     * Sets temporary shields on the player which are then read by the Shamanic Ritual
     * event: immunity to aura loss and/or doubling of the bonus.
     *
     * @param p            the player who owns the building
     * @param g            the current game
     * @param card         the building that possesses the effect
     * @param currentEvent the event currently being resolved
     */
    @Override
    public void applyOnEvent(Player p, Game g, BuildingCard card, EventCard currentEvent) {
        // these two flags are in the player
        if (noLoss) p.setShamanNoLoss(true);
        if (doubleAura) p.setShamanDoubleAura(true);
    }
}