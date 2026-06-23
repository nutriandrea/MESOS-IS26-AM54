package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

/**
 * Effetto edificio: a fine turno concede al giocatore una pesca extra.
 */
public class BuildingExtraPick implements IEffect<BuildingCard> {
    public BuildingExtraPick(JsonObject params) {
        // Non serve leggere parametri, questa carta fa solo una cosa!
    }

    /**
     * Attiva il flag di pesca extra sul giocatore alla chiusura del turno.
     *
     * @param p    il giocatore proprietario dell'edificio
     * @param g    la partita corrente
     * @param card l'edificio che possiede l'effetto
     */
    @Override
    public void applyOnTurnEnd(Player p, Game g, BuildingCard card) {
        p.setExtraPick(true);
    }
}