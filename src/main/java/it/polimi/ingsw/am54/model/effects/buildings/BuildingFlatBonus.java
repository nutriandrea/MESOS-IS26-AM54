package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

// Raggruppa la carta che dona 25 Punti Prestigio a fine partita, quella che dona 3 icone Magia (stella)
// per lo Sciamano, e la carta che permette un "Extra Pick" (prendere una carta dalla fila superiore)
public class BuildingFlatBonus implements IEffect<BuildingCard> {
    private final int auraBonus;
    private final int magicBonus;
    private final boolean extraPick;
    private final boolean isEndgame;

    public BuildingFlatBonus(JsonNode params) {
        this.auraBonus = params != null && params.has("aura") ? params.get("aura").asInt() : 0;
        this.magicBonus = params != null && params.has("magic") ? params.get("magic").asInt() : 0;
        this.extraPick = params != null && params.has("extraPick") && params.get("extraPick").asBoolean();
        this.isEndgame = params != null && params.has("isEndgame") && params.get("isEndgame").asBoolean();
    }

    @Override public boolean isEndgameEffect() { return isEndgame; }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        if (auraBonus > 0) p.modifyAuraPoints(auraBonus);
        if (magicBonus > 0) p.modifyMagicPoints(magicBonus);
        if (extraPick) p.setExtraPick(true);
    }
}