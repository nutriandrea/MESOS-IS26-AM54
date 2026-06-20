package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Extra pick: at the end of the player's turn, enables an additional
 * ability to draw a card from the upper row of the board.
 *
 * @author AM54
 */
public class BuildingExtraPick implements IEffect<BuildingCard> {
    /**
     * Constructs the effect. No parameters are read as this building
     * has a single fixed behaviour.
     *
     * @param params JSON parameters (unused), may be {@code null}
     */
    public BuildingExtraPick(JsonObject params) {
        // No need to read parameters, this card does only one thing!
    }

    /**
     * Enables the extra pick ability on the player at the end of their turn,
     * allowing one additional draw from the upper row.
     *
     * @param p    the player ending their turn
     * @param g    the current game instance
     * @param card the building card providing this effect
     */
    @Override
    public void applyOnTurnEnd(Player p, Game g, BuildingCard card) {
        p.setExtraPick(true);
    }
}