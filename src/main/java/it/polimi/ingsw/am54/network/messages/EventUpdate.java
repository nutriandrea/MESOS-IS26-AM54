// Nel file: network/messages/EventUpdate.java
package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Notifica broadcast inviata dal server dopo la risoluzione di un evento.
 * Contiene lo snapshot completo delle statistiche di tutti i giocatori
 * (cibo, aura, magia, tribù, edifici) e lo stato aggiornato del tabellone.
 * Usata dal client per aggiornare istantaneamente l'interfaccia.
 */
public final class EventUpdate implements Update {

    // DTO compatto per le statistiche di un singolo giocatore
    public static class PlayerSnapshot implements Serializable {
        public final int food;
        public final int aura;
        public final int magicPoints;
        public final List<String> tribe;
        public final List<String> buildings;

        public PlayerSnapshot(int food, int aura, int magicPoints, List<String> tribe, List<String> buildings) {
            this.food = food;
            this.aura = aura;
            this.magicPoints = magicPoints;
            this.tribe = tribe;
            this.buildings = buildings;
        }
    }

    private final String eventName;
    private final Map<String, PlayerSnapshot> playersStats;
    private final List<String> charUpper, charLower, eventUpper, eventLower, buildUpper, buildLower;

    public EventUpdate(String eventName, Map<String, PlayerSnapshot> playersStats,
                       List<String> charUpper, List<String> charLower,
                       List<String> eventUpper, List<String> eventLower,
                       List<String> buildUpper, List<String> buildLower) {
        this.eventName = eventName;
        this.playersStats = playersStats;
        this.charUpper = charUpper;
        this.charLower = charLower;
        this.eventUpper = eventUpper;
        this.eventLower = eventLower;
        this.buildUpper = buildUpper;
        this.buildLower = buildLower;
    }

    public String getEventName() { return eventName; }
    public Map<String, PlayerSnapshot> getPlayersStats() { return playersStats; }
    public List<String> getCharUpper() { return charUpper; }
    public List<String> getCharLower() { return charLower; }
    public List<String> getEventUpper() { return eventUpper; }
    public List<String> getEventLower() { return eventLower; }
    public List<String> getBuildUpper() { return buildUpper; }
    public List<String> getBuildLower() { return buildLower; }

    @Override
    public void applyTo(ClientModel model) {
        model.updateEventSnapshot(this);
    }
}