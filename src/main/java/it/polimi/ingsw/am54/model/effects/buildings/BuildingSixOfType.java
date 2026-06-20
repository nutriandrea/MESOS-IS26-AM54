package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

/**
 * Bonus for six cards of the same type: every time the player completes
 * a set of at least six characters of the same job type,
 * recives 5 food as a bonus.
 *
 * @author AM54
 */
public class BuildingSixOfType implements IEffect<BuildingCard> {

    /**
     * Grants 5 food whenever the player's tribe reaches at least six
     * characters of the same job type.
     *
     * @param p            the player who picked the character
     * @param g            the current game instance
     * @param card         the building card providing this effect
     * @param newCharacter the character that was just picked
     */
    @Override
    public void applyOnCharacterPicked(Player p, Game g, BuildingCard card, CharacterCard newCharacter) {
        List<CharacterCard> tribe = p.getMyTribe();
        JobType job = newCharacter.getJobType();

        if (job == null || job == JobType.NULL) return;

        long currentCount = tribe.stream().filter(c -> c.getJobType() == job).count();
        if (currentCount >= 6) {
            p.modifyFood(5);
        }
    }
}