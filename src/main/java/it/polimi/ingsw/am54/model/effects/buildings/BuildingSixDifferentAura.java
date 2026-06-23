package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.Arrays;

// Gestisce i 6 Punti Prestigio a fine partita per ogni set di 6 personaggi diversi
public class BuildingSixDifferentAura implements IEffect<BuildingCard> {

    /**
     * A fine partita assegna 6 aura per ogni set completo dei sei mestieri diversi:
     * il numero di set equivale al mestiere meno rappresentato nella tribù.
     *
     * @param p    il giocatore proprietario dell'edificio
     * @param g    la partita corrente
     * @param card l'edificio che possiede l'effetto
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