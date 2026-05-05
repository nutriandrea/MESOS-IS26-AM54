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
            case "HUNTER" -> {
                if (card.getLevel() > 0) {
                    int totalHunters = (int) p.getMyTribe().stream()
                            .filter(c -> c.getJobType() == JobType.HUNTER).count();
                    p.modifyFood(totalHunters);
                }
            }
            case "CONSTRUCTOR" -> p.modifyMyBuildingDiscount(card.getLevel());
            case "SHAMAN" -> p.modifyMagicPoints(card.getLevel());
            case "FARMER" -> p.modifyMyFoodDiscount(p.getMyFoodDiscount()+3);
        }
    }
}
