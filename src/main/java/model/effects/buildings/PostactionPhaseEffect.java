package model.effects.buildings;

import com.fasterxml.jackson.databind.JsonNode;
import model.*;

/**
 * Gestisce tutti gli effetti della fase POSTACTION.
 */
public class PostactionPhaseEffect implements IEffect<BuildingCard> {

    private final String effectName;

    public PostactionPhaseEffect(String effectName, JsonNode params) {
        this.effectName = effectName;
    }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        switch (effectName) {
            case "EXTRA_PICK" -> p.setExtraPick(true);
        }
    }
}