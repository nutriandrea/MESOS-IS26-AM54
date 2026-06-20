package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

/**
 * Double Inventor: grants 3 food each time a pair is completed
 * of Inventor characters of the same level in the player&rsquo;s tribe.
 *
 * @author AM54
 */
public class BuildingDoubleInventor implements IEffect<BuildingCard> {

    /**
     * Grants 3 food each time a pair of Inventor characters of the same
     * level is completed in the player's tribe.
     *
     * @param p            the player who picked the character
     * @param g            the current game instance
     * @param card         the building card providing this effect
     * @param newCharacter the character that was just picked
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