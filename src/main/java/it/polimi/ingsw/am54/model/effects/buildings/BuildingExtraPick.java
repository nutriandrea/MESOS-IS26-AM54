package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Building effect: grants the player an extra pick at the end of the turn.
 */
public class BuildingExtraPick implements IEffect<BuildingCard> {
    public BuildingExtraPick(JsonObject params) {
        // No need to read parameters, this card only does one thing!
    }

    /**
     * Activates the extra pick flag on the player at the end of the turn.
     *
     * @param p    the player who owns the building
     * @param g    the current game
     * @param card the building that possesses the effect
     */
    @Override
    public void applyOnTurnEnd(Player p, Game g, BuildingCard card) {
        p.setExtraPick(true);
    }
}