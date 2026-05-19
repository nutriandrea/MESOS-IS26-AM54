package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

public class BuildingExtraPick implements IEffect<BuildingCard> {
    public BuildingExtraPick(JsonNode params) {
        // Non serve leggere parametri, questa carta fa solo una cosa!
    }

    @Override
    public void applyOnPurchase(Player p, Game g, BuildingCard card) {
        p.setExtraPick(true);
    }
}