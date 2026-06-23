package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Effetto edificio: all'acquisto fornisce immediatamente aura e/o punti magia.
 */
public class BuildingImmediateResource implements IEffect<BuildingCard> {
    private final int auraBonus;
    private final int magicBonus;

    public BuildingImmediateResource(JsonObject params) {
        this.auraBonus = params != null && params.has("aura") ? params.get("aura").getAsInt() : 0;
        this.magicBonus = params != null && params.has("magic") ? params.get("magic").getAsInt() : 0;
    }

    /**
     * Accredita al giocatore l'aura e la magia configurate al momento dell'acquisto.
     *
     * @param p    il giocatore proprietario dell'edificio
     * @param g    la partita corrente
     * @param card l'edificio che possiede l'effetto
     */
    @Override
    public void applyOnPurchase(Player p, Game g, BuildingCard card) {
        if (auraBonus > 0) p.modifyAuraPoints(auraBonus);
        if (magicBonus > 0) p.modifyMagicPoints(magicBonus);
    }
}