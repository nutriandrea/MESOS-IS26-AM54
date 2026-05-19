package it.polimi.ingsw.am54.model.effects.buildings;
import it.polimi.ingsw.am54.model.*;
import java.util.List;

// Gestisce i 3 Cibo ottenuti formando coppie di Inventori uguali
public class BuildingDoubleInventor implements IEffect<BuildingCard> {

    // CAMBIATO SOLO IL NOME DEL METODO IN applyOnPurchase
    @Override
    public void applyOnPurchase(Player p, Game g, BuildingCard card) {
        List<CharacterCard> tribe = p.getMyTribe();
        if (tribe.isEmpty()) return;

        CharacterCard last = tribe.get(tribe.size() - 1);
        if (last.getJobType() == JobType.INVENTOR) {
            long count = tribe.stream()
                    .filter(c -> c.getJobType() == JobType.INVENTOR && c.getLevel() == last.getLevel())
                    .count();
            if (count > 0 && count % 2 == 0) p.modifyFood(3);
        }
    }
}