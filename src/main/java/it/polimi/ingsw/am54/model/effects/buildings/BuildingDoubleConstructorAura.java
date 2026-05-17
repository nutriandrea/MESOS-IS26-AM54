package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;

public class BuildingDoubleConstructorAura implements IEffect<BuildingCard> {
    @Override public boolean isEndgameEffect() { return true; }
    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.CONSTRUCTOR)
                .forEach(c -> p.modifyAuraPoints(c.getAura()));
    }
}