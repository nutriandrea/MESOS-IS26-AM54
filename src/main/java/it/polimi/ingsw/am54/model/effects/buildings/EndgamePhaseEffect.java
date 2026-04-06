package it.polimi.ingsw.am54.model.effects.buildings;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

import java.util.Arrays;

//Gestisce tutti gli effetti della fase ENDGAME
public class EndgamePhaseEffect implements IEffect<BuildingCard> {

    private final String effectName;
    private final JobType jobType;
    private final int multiplier;
    private final int amount;

    public EndgamePhaseEffect(String effectName, JsonNode params) {
        this.effectName = effectName;

        // se params è null è inutile fare i controlli dopo
        if (params == null) {
            this.jobType = null;
            this.multiplier = 0;
            this.amount = 0;
            return;
        }

        this.jobType = params.has("jobType") ? JobType.valueOf(params.get("jobType").asText()) : null;
        this.multiplier = params.has("multiplier") ? params.get("multiplier").asInt() : 0;
        this.amount = params.has("amount") ? params.get("amount").asInt() : 0;
    }

    @Override
    public boolean isEndgameEffect() {
        return true;
    }

    public String getEffectName() {
        return effectName;
    }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        switch (effectName) {
            case "AURA_PER_JOB" -> {
                long count = p.getMyTribe().stream()
                        .filter(c -> c.getJobType() == jobType).count();
                p.modifyAuraPoints((int) (multiplier * count));
            }
            case "DOUBLE_CONSTRUCTOR_AURA" -> {
                p.getMyTribe().stream()
                        .filter(c -> c.getJobType() == JobType.CONSTRUCTOR)
                        .forEach(c -> p.modifyAuraPoints(c.getAura()));
            }
            case "SIX_DIFFERENT_AURA" -> {
                JobType[] allJobs = {
                        JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN,
                        JobType.FARMER, JobType.ARTIST, JobType.INVENTOR
                };
                long minCount = Arrays.stream(allJobs)
                        .mapToLong(job -> p.getMyTribe().stream()
                                .filter(c -> c.getJobType() == job).count())
                        .min().orElse(0);
                p.modifyAuraPoints((int) (6 * minCount));
            }
            case "FLAT_AURA_BONUS" -> p.modifyAuraPoints(amount);
        }
    }
}
