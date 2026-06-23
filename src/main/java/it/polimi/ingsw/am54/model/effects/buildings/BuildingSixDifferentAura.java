package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.Arrays;

// Handles the 6 Prestige Points at the end of the game for each set of 6 different characters
public class BuildingSixDifferentAura implements IEffect<BuildingCard> {

    /**
     * At the end of the game, awards 6 aura for each complete set of the six different jobs:
     * the number of sets equals the least represented job in the tribe.
     *
     * @param p    the player who owns the building
     * @param g    the current game
     * @param card the building that possesses the effect
     */
    @Override
    public void applyOnEndGame(Player p, Game g, BuildingCard card) {
        JobType[] allJobs = { JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN, JobType.FARMER, JobType.ARTIST, JobType.INVENTOR };
        long minCount = Arrays.stream(allJobs)
                .mapToLong(job -> p.getMyTribe().stream().filter(c -> c.getJobType() == job).count())
                .min().orElse(0);
        p.modifyAuraPoints((int) (6 * minCount));
    }
}