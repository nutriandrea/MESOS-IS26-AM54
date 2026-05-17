package it.polimi.ingsw.am54.model.effects.characters;

import it.polimi.ingsw.am54.model.*;

public class CharacterHunter implements IEffect<CharacterCard> {

    @Override
    public void apply(Player p, Game g, CharacterCard card) {
        if (card.getLevel() > 0) {
            int totalHunters = (int) p.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count();
            p.modifyFood(totalHunters);
        }
    }
}