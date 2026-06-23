package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

// Handles the 6 Era 3 cards that grant prestige points at the end of the game based on the character type
// (e.g. Farmer x4, Hunter x3), plus the Era 2 card that doubles the base points of Constructors
public class BuildingJobAuraBonus implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final int multiplier;
    private final boolean useCardBaseAura;

    public BuildingJobAuraBonus(JsonObject params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").getAsString()) : null;
        this.multiplier = params != null && params.has("multiplier") ? params.get("multiplier").getAsInt() : 0;
        this.useCardBaseAura = params != null && params.has("useCardBaseAura") && params.get("useCardBaseAura").getAsBoolean();
    }

    /**
     * At the end of the game, credits aura based on characters of the configured job:
     * either by summing their base aura ({@code useCardBaseAura}), or by multiplying their
     * number by the building's multiplier.
     *
     * @param p    the player who owns the building
     * @param g    the current game
     * @param card the building that possesses the effect
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