package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

// Gestisce il bonus in cibo ottenuto al completamento di un set di 6 personaggi
public class BuildingSixOfType implements IEffect<BuildingCard> {

    // FIRMA AGGIORNATA: Si innesca subito dopo aver pescato/comprato
    @Override
    public void applyOnPurchase(Player p, Game g, BuildingCard card) {
        List<CharacterCard> tribe = p.getMyTribe();
        if (tribe.isEmpty()) return;

        JobType job = tribe.get(tribe.size() - 1).getJobType();
        if (job == null) return;

        long currentCount = tribe.stream().filter(c -> c.getJobType() == job).count();
        JobType[] allJobs = { JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN, JobType.FARMER, JobType.ARTIST, JobType.INVENTOR };

        for (JobType j : allJobs) {
            if (tribe.stream().filter(c -> c.getJobType() == j).count() < currentCount) return; // Rompe se un lavoro è in difetto
        }
        p.modifyFood(5);
    }
}