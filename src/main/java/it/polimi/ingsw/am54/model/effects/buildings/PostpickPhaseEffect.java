package it.polimi.ingsw.am54.model.effects.buildings;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;
import model.*;

import java.util.List;

//Gestisce tutti gli effetti della fase POSTPICK
public class PostpickPhaseEffect implements IEffect<BuildingCard> {

    private final String effectName;

    public PostpickPhaseEffect(String effectName, JsonNode params) {
        this.effectName = effectName;
    }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        switch (effectName) {
            case "SIX_OF_TYPE" -> {
                List<CharacterCard> tribe = p.getMyTribe();
                if (tribe.isEmpty()) return;

                CharacterCard ultimaCarta = tribe.get(tribe.size() - 1);
                JobType lavoroAppenaPescato = ultimaCarta.getJobType();
                if (lavoroAppenaPescato == null) return;

                long conteggioQuestoLavoro = tribe.stream()
                        .filter(c -> c.getJobType() == lavoroAppenaPescato).count();

                JobType[] tuttiILavori = {
                        JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN,
                        JobType.FARMER, JobType.ARTIST, JobType.INVENTOR
                };

                boolean setCompletato = true;
                for (JobType job : tuttiILavori) {
                    long conteggio = tribe.stream()
                            .filter(c -> c.getJobType() == job).count();
                    if (conteggio < conteggioQuestoLavoro) {
                        setCompletato = false;
                        break;
                    }
                }
                if (setCompletato) {
                    p.modifyFood(1);
                }
            }
            case "DOUBLE_INVENTOR" -> {
                List<CharacterCard> tribe = p.getMyTribe();
                if (tribe.isEmpty()) return;

                CharacterCard ultimaCarta = tribe.get(tribe.size() - 1);
                if (ultimaCarta.getJobType() == JobType.INVENTOR) {
                    int iconaInvenzione = ultimaCarta.getLevel();
                    long conteggioTotale = tribe.stream()
                            .filter(c -> c.getJobType() == JobType.INVENTOR)
                            .filter(c -> c.getLevel() == iconaInvenzione)
                            .count();
                    if (conteggioTotale > 0 && conteggioTotale % 2 == 0) {
                        p.modifyFood(3);
                    }
                }
            }
            case "THREE_MAGIC" -> p.modifyMagicPoints(3);
        }
    }
}
