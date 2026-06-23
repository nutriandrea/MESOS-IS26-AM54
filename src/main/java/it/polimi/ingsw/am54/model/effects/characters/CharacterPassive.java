package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

/**
 * Effetto "passivo" condiviso da Artista, Inventore e Raccoglitore: non producono
 * nulla all'acquisto, contano solo come statistiche per scoring ed eventi di fine
 * partita. L'implementazione è volutamente vuota.
 */
public class CharacterPassive implements IEffect<CharacterCard> {
    @Override
    public void applyOnPurchase(Player p, Game g, CharacterCard card) {
        // Le carte Artista Inventore e Farmer non hanno effetti immediati sulla plancia.
        // Esistono solo per le statistiche passive, quindi questo metodo resta vuoto!
    }
}
