package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

// Gestisce l'immunità alla perdita di punti (No Loss) e il raddoppio dei punti (Double Aura)
// durante l'Evento Rituale Sciamanico
public class BuildingShamanPassive implements IEffect<BuildingCard> {
    private final boolean noLoss;
    private final boolean doubleAura;

    public BuildingShamanPassive(JsonNode params) {
        this.noLoss = params != null && params.has("noLoss") && params.get("noLoss").asBoolean();
        this.doubleAura = params != null && params.has("doubleAura") && params.get("doubleAura").asBoolean();
    }

    @Override
    public void applyOnEvent(Player p, Game g, BuildingCard card, EventCard currentEvent) {
        // Nessun check sull'evento! Io alzo le mie difese.
        // Sarà l'evento in corso a decidere se guardare queste variabili o no.
        if (noLoss) p.setShamanNoLoss(true);
        if (doubleAura) p.setShamanDoubleAura(true);
    }
}