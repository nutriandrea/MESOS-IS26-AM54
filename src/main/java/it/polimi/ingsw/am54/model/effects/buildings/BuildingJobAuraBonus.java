package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Aura bonus by job type: at end of game grants Aura points based on
 * to the number of characters of a given type owned by the player.
 * Handles Era 3 cards (e.g. Farmer x4, Hunter x3) and the
 * Era 2 card that doubles the base points of Builders.
 *
 * @author AM54
 */
public class BuildingJobAuraBonus implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final int multiplier;
    private final boolean useCardBaseAura;

    /**
     * Constructs the effect reading the job type, multiplier, and
     * base-Aura flag from the JSON parameters.
     *
     * @param params JSON object with optional "jobType", "multiplier",
     *               and "useCardBaseAura" fields, may be {@code null}
     */
    public BuildingJobAuraBonus(JsonObject params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").getAsString()) : null;
        this.multiplier = params != null && params.has("multiplier") ? params.get("multiplier").getAsInt() : 0;
        this.useCardBaseAura = params != null && params.has("useCardBaseAura") && params.get("useCardBaseAura").getAsBoolean();
    }

    // UPDATED SIGNATURE FOR THE ENDGAME PHASE
    /**
     * Grants Aura points at the end of the game based on the number of
     * characters of the configured job type in the player's tribe,
     * multiplied by the configured multiplier or using each card's base Aura.
     *
     * @param p    the player receiving the bonus
     * @param g    the current game instance
     * @param card the building card providing this effect
     */
    @Override
    public void applyOnEndGame(Player p, Game g, BuildingCard card) {
        if (jobType == null) return;

        if (useCardBaseAura) {
            p.getMyTribe().stream()
                    .filter(c -> c.getJobType() == jobType)
                    .forEach(c -> p.modifyAuraPoints(c.getAura()));
        } else if (multiplier > 0) {
            long count = p.getMyTribe().stream().filter(c -> c.getJobType() == jobType).count();
            p.modifyAuraPoints((int) (count * multiplier));
        }
    }
}