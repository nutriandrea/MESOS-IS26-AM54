package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

/**
 * Effetto edificio: premia con cibo bonus il completamento di una coppia di
 * inventori dello stesso livello.
 */
public class BuildingDoubleInventor implements IEffect<BuildingCard> {

    /**
     * Se il personaggio appena pescato è un inventore e chiude una coppia dello
     * stesso livello, accredita 3 cibo al giocatore.
     *
     * @param p            il giocatore proprietario dell'edificio
     * @param g            la partita corrente
     * @param card         l'edificio che possiede l'effetto
     * @param newCharacter il personaggio appena aggiunto alla tribù
     */
    @Override
    public void applyOnCharacterPicked(Player p, Game g, BuildingCard card, CharacterCard newCharacter) {
        List<CharacterCard> tribe = p.getMyTribe();

        if (newCharacter.getJobType() == JobType.INVENTOR) {
            long count = tribe.stream()
                    .filter(c -> c.getJobType() == JobType.INVENTOR && c.getLevel() == newCharacter.getLevel())
                    .count();

            // Se la conta è pari e maggiore di 0, abbiamo appena chiuso una coppia!
            if (count > 0 && count % 2 == 0) {
                p.modifyFood(3);
            }
        }
    }
}