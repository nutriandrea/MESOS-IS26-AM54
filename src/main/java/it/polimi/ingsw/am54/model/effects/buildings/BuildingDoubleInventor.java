package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

/**
 * Building effect: rewards with bonus food the completion of a pair of
 * inventors of the same level.
 */
public class BuildingDoubleInventor implements IEffect<BuildingCard> {

    /**
     * If the newly picked character is an inventor and completes a pair of the
     * same level, credits 3 food to the player.
     *
     * @param p            the player who owns the building
     * @param g            the current game
     * @param card         the building that possesses the effect
     * @param newCharacter the character just added to the tribe
     */
    @Override
    public void applyOnCharacterPicked(Player p, Game g, BuildingCard card, CharacterCard newCharacter) {
        List<CharacterCard> tribe = p.getMyTribe();

        if (newCharacter.getJobType() == JobType.INVENTOR) {
            long count = tribe.stream()
                    .filter(c -> c.getJobType() == JobType.INVENTOR && c.getLevel() == newCharacter.getLevel())
                    .count();

            // If the count is even and greater than 0, we just completed a pair!
            if (count > 0 && count % 2 == 0) {
                p.modifyFood(3);
            }
        }
    }
}