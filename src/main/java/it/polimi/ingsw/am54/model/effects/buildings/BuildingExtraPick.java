package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

public class BuildingExtraPick implements IEffect<BuildingCard> {
    public BuildingExtraPick(JsonObject params) {
        // Non serve leggere parametri, questa carta fa solo una cosa!
    }

    @Override
    public void applyOnTurnEnd(Player p, Game g, BuildingCard card) {
        p.setExtraPick(true);
    }
}