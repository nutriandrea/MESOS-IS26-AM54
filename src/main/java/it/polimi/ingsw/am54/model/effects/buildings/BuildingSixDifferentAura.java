package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.Arrays;

/**
 * Bonus for six different characters: at end of game grants 6 Aura points
 * for each complete set of characters of all six available types
 * (Hunter, Builder, Shaman, Farmer, Artist, Inventor).
 *
 * @author AM54
 */
public class BuildingSixDifferentAura implements IEffect<BuildingCard> {

    // UPDATED SIGNATURE FOR THE ENDGAME PHASE
    /**
     * Grants 6 Aura points for each complete set of characters of all six
     * job types present in the player's tribe at the end of the game.
     *
     * @param p    the player receiving the bonus
     * @param g    the current game instance
     * @param card the building card providing this effect
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