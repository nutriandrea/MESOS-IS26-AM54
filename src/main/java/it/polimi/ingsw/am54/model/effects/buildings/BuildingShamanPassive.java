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

    @Override public boolean isNoLossEffect() { return noLoss; }
    @Override public boolean isDoubleAuraShamanEffect() { return doubleAura; }
    @Override public void apply(Player p, Game g, BuildingCard card) { /* Nessun effetto attivo */ }
}