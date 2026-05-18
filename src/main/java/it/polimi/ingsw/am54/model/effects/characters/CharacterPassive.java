package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

public class CharacterPassive implements IEffect<CharacterCard> {
    @Override
    public void apply(Player p, Game g, CharacterCard card) {
        // Le carte Artista Inventore e Farmer non hanno effetti immediati sulla plancia.
        // Esistono solo per le statistiche passive, quindi questo metodo resta vuoto!
    }
}