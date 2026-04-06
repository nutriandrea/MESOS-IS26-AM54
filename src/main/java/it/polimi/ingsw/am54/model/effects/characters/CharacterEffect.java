package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;


//Ggstisce tutti gli effetti dei personaggi
//l'effetto da applicare è determinato dal characterType letto dal JSON
public class CharacterEffect implements IEffect<CharacterCard> {

    private final String effectName;

    public CharacterEffect(String effectName) {
        this.effectName = effectName;
    }

    @Override
    public void apply(Player p, Game g, CharacterCard card) {
        switch (effectName) {
            case "HUNTER" -> p.modifyFood((int) (p.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count() + 1));
            case "CONSTRUCTOR" -> p.modifyMyBuildingDiscount(card.getLevel());
            case "SHAMAN" -> p.modifyMagicPoints(card.getLevel());
            case "FARMER" -> p.modifyMyFoodDiscount(card.getLevel());
        }
    }
}
