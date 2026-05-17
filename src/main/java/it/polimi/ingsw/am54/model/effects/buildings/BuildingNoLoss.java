package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

public class BuildingNoLoss implements IEffect<BuildingCard> {
    @Override public boolean isNoLossEffect() { return true; }
    @Override public void apply(Player p, Game g, BuildingCard card) { /* Controllato dall'evento SHAMAN */ }
}