package it.polimi.ingsw.am54.model.effects.buildings;
import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;

// Gestisce l'immunità alla perdita di punti (No Loss) e il raddoppio dei punti (Double Aura)
// durante l'Evento Rituale Sciamanico
public class BuildingShamanPassive implements IEffect<BuildingCard> {
    private final boolean noLoss;
    private final boolean doubleAura;

    public BuildingShamanPassive(JsonObject params) {
        this.noLoss = params != null && params.has("noLoss") && params.get("noLoss").getAsBoolean();
        this.doubleAura = params != null && params.has("doubleAura") && params.get("doubleAura").getAsBoolean();
    }

    /**
     * Imposta sul giocatore gli scudi temporanei letti poi dall'evento Rituale
     * Sciamanico: immunità alla perdita di aura e/o raddoppio del bonus.
     *
     * @param p            il giocatore proprietario dell'edificio
     * @param g            la partita corrente
     * @param card         l'edificio che possiede l'effetto
     * @param currentEvent l'evento in corso di risoluzione
     */
    @Override
    public void applyOnEvent(Player p, Game g, BuildingCard card, EventCard currentEvent) {
        // nel player ci sono ste 2 flag
        if (noLoss) p.setShamanNoLoss(true);
        if (doubleAura) p.setShamanDoubleAura(true);
    }
}