package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

public class BuildingDoubleInventor implements IEffect<BuildingCard> {

    @Override
    public void applyOnCharacterPicked(Player p, Game g, BuildingCard card, CharacterCard newCharacter) {
        List<CharacterCard> tribe = p.getMyTribe();

        if (newCharacter.getJobType() == JobType.INVENTOR) {
            long count = tribe.stream()
                    .filter(c -> c.getJobType() == JobType.INVENTOR && c.getLevel() == newCharacter.getLevel())
                    .count();

            // Se la conta è pari e maggiore di 0, abbiamo appena chiuso una coppia!
            if (count > 0 && count % 2 == 0) {
                p.modifyFood(3);
            }
        }
    }
}