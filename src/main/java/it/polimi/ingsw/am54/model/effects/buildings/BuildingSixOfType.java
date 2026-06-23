package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

/**
 * Effetto edificio: concede cibo bonus ogni volta che il giocatore completa un set
 * con almeno un personaggio per ciascuno dei sei mestieri.
 */
public class BuildingSixOfType implements IEffect<BuildingCard> {

    /**
     * All'arrivo di un nuovo personaggio verifica se ogni mestiere raggiunge la
     * quota del mestiere appena incrementato; se il set è completo accredita 5 cibo.
     *
     * @param p            il giocatore proprietario dell'edificio
     * @param g            la partita corrente
     * @param card         l'edificio che possiede l'effetto
     * @param newCharacter il personaggio appena aggiunto alla tribù
     */
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