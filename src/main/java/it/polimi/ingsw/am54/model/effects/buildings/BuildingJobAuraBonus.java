package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

// Gestisce le 6 carte di Era 3 che danno punti prestigio a fine partita in base al tipo di personaggio
// (es. Contadino x4, Cacciatore x3), più la carta di Era 2 che raddoppia i punti base dei Costruttori
public class BuildingJobAuraBonus implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final int multiplier;
    private final boolean useCardBaseAura;

    public BuildingJobAuraBonus(JsonObject params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").getAsString()) : null;
        this.multiplier = params != null && params.has("multiplier") ? params.get("multiplier").getAsInt() : 0;
        this.useCardBaseAura = params != null && params.has("useCardBaseAura") && params.get("useCardBaseAura").getAsBoolean();
    }

    // FIRMA AGGIORNATA PER LA FASE FINE PARTITA
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