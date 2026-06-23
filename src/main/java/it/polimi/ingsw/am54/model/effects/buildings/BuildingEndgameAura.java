package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Effetto edificio: accredita un bonus fisso di aura al calcolo di fine partita.
 */
public class BuildingEndgameAura implements IEffect<BuildingCard> {
    private final int auraBonus;

    public BuildingEndgameAura(JsonObject params) {
        this.auraBonus = params != null && params.has("aura") ? params.get("aura").getAsInt() : 0;
    }

    /**
     * Aggiunge il bonus di aura configurato al punteggio del giocatore.
     *
     * @param p    il giocatore proprietario dell'edificio
     * @param g    la partita corrente
     * @param card l'edificio che possiede l'effetto
     */
    @Override
    public void applyOnEndGame(Player p, Game g, BuildingCard card) {
        if (auraBonus > 0) p.modifyAuraPoints(auraBonus);
    }
}