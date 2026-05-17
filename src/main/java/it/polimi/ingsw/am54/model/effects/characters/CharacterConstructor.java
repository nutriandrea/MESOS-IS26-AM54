package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

public class CharacterConstructor implements IEffect<CharacterCard> {

    @Override
    public void apply(Player p, Game g, CharacterCard card) {
        p.modifyMyBuildingDiscount(card.getLevel());
    }
}