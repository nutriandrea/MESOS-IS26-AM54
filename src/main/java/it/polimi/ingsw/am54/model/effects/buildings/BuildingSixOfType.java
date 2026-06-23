package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

/**
 * Building effect: grants bonus food every time the player completes a set
 * with at least one character for each of the six jobs.
 */
public class BuildingSixOfType implements IEffect<BuildingCard> {

    /**
     * Upon the arrival of a new character, checks if every job reaches the
     * quota of the newly incremented job; if the set is complete, credits 5 food.
     *
     * @param p            the player who owns the building
     * @param g            the current game
     * @param card         the building that possesses the effect
     * @param newCharacter the character just added to the tribe
     */
    @Override
    public void applyOnCharacterPicked(Player p, Game g, BuildingCard card, CharacterCard newCharacter) {
        List<CharacterCard> tribe = p.getMyTribe();
        JobType job = newCharacter.getJobType();

        if (job == null || job == JobType.NULL) return;

        long currentCount = tribe.stream().filter(c -> c.getJobType() == job).count();
        JobType[] allJobs = { JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN, JobType.FARMER, JobType.ARTIST, JobType.INVENTOR };

        for (JobType j : allJobs) {
            if (tribe.stream().filter(c -> c.getJobType() == j).count() < currentCount) return; // Breaks if a job is lacking
        }

        p.modifyFood(5); // If it gets here, the set is complete!
    }
}