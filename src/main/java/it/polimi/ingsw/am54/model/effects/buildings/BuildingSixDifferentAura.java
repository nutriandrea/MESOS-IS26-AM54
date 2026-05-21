package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.Arrays;

// Gestisce i 6 Punti Prestigio a fine partita per ogni set di 6 personaggi diversi
public class BuildingSixDifferentAura implements IEffect<BuildingCard> {

    // FIRMA AGGIORNATA PER LA FASE DI FINE PARTITA
    @Override
    public void applyOnEndGame(Player p, Game g, BuildingCard card) {
        JobType[] allJobs = { JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN, JobType.FARMER, JobType.ARTIST, JobType.INVENTOR };
        long minCount = Arrays.stream(allJobs)
                .mapToLong(job -> p.getMyTribe().stream().filter(c -> c.getJobType() == job).count())
                .min().orElse(0);
        p.modifyAuraPoints((int) (6 * minCount));
    }
}