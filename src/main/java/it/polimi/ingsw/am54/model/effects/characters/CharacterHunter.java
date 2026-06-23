package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Effetto del Cacciatore: all'acquisto fornisce cibo pari al numero di cacciatori
 * già posseduti dal giocatore (incluso quello appena pescato).
 */
public class CharacterHunter implements IEffect<CharacterCard> {

    /**
     * Conta i cacciatori nella tribù del giocatore e gli accredita altrettanto cibo.
     * I cacciatori di livello 0 non producono effetto.
     *
     * @param p    il giocatore che ha acquistato la carta
     * @param g    la partita corrente
     * @param card il cacciatore appena pescato
     */
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        if (card.getLevel() > 0) {
            int totalHunters = (int) p.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count();
            p.modifyFood(totalHunters);
        }
    }
}
