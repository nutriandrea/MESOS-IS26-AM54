package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

public class CharacterFarmer implements IEffect<CharacterCard> {

    @Override
    public void apply(Player p, Game g, CharacterCard card) {
        p.modifyMyFoodDiscount(p.getMyFoodDiscount() + 3);
    }
}