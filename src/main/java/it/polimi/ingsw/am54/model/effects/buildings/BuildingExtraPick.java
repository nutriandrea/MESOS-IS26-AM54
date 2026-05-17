package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

public class BuildingExtraPick implements IEffect<BuildingCard> {
    @Override public void apply(Player p, Game g, BuildingCard card) {
        p.setExtraPick(true);
    }
}