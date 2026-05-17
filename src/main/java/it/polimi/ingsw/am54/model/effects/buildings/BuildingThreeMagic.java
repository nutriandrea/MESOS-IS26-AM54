package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

public class BuildingThreeMagic implements IEffect<BuildingCard> {
    @Override public void apply(Player p, Game g, BuildingCard card) {
        p.modifyMagicPoints(3);
    }
}