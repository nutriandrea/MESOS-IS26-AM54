package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

public class BuildingDoubleInventor implements IEffect<BuildingCard> {
    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        List<CharacterCard> tribe = p.getMyTribe();
        if (tribe.isEmpty()) return;

        CharacterCard last = tribe.get(tribe.size() - 1);
        if (last.getJobType() == JobType.INVENTOR) {
            long count = tribe.stream()
                    .filter(c -> c.getJobType() == JobType.INVENTOR && c.getLevel() == last.getLevel())
                    .count();
            if (count > 0 && count % 2 == 0) p.modifyFood(3);
        }
    }
}