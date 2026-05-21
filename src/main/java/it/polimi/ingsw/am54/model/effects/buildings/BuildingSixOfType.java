package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

//ogni volta che completi il set
public class BuildingSixOfType implements IEffect<BuildingCard> {

    @Override
    public void applyOnCharacterPicked(Player p, Game g, BuildingCard card, CharacterCard newCharacter) {
        List<CharacterCard> tribe = p.getMyTribe();
        JobType job = newCharacter.getJobType();

        if (job == null || job == JobType.NULL) return;

        long currentCount = tribe.stream().filter(c -> c.getJobType() == job).count();
        JobType[] allJobs = { JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN, JobType.FARMER, JobType.ARTIST, JobType.INVENTOR };

        for (JobType j : allJobs) {
            if (tribe.stream().filter(c -> c.getJobType() == j).count() < currentCount) return; // Rompe se un lavoro è in difetto
        }

        p.modifyFood(5); // Se arriva qui, il set è completo!
    }
}