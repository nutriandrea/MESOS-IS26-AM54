package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.model.TotemColors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class StateChangedUpdate implements Update {

    // DTO per le statistiche del giocatore
    public static class PlayerSnapshot implements Serializable {
        public final int food;
        public final int aura;
        public final int magicPoints;
        public final TotemColors color; // Utile da mandare all'inizio
        public final List<String> tribe;
        public final List<String> buildings;

        public PlayerSnapshot(int food, int aura, int magicPoints, TotemColors color,  List<String> tribe, List<String> buildings) {
            this.food = food;
            this.aura = aura;
            this.magicPoints = magicPoints;
            this.color = color;
            this.tribe = tribe;
            this.buildings = buildings;
        }
    }

    private final String stateName;
    private final Map<String, PlayerSnapshot> playersStats;
    private final List<String> turnOrderTrack;
    private final Map<Integer, String> offeringTrack;
    private final List<String> charUpper, charLower, eventUpper, eventLower, buildUpper, buildLower;

    public StateChangedUpdate(String stateName,
                              Map<String, PlayerSnapshot> playersStats,
                              List<String> turnOrderTrack,
                              Map<Integer, String> offeringTrack,
                              List<String> charUpper, List<String> charLower,
                              List<String> eventUpper, List<String> eventLower,
                              List<String> buildUpper, List<String> buildLower) {
        this.stateName = stateName;
        this.playersStats = playersStats;
        this.turnOrderTrack = turnOrderTrack;
        this.offeringTrack = offeringTrack;
        this.charUpper = charUpper;
        this.charLower = charLower;
        this.eventUpper = eventUpper;
        this.eventLower = eventLower;
        this.buildUpper = buildUpper;
        this.buildLower = buildLower;
    }

    public String getStateName() { return stateName; }
    public Map<String, PlayerSnapshot> getPlayersStats() { return playersStats; }
    public List<String> getTurnOrderTrack() { return turnOrderTrack; }
    public Map<Integer, String> getOfferingTrack() { return offeringTrack; }
    public List<String> getCharUpper() { return charUpper; }
    public List<String> getCharLower() { return charLower; }
    public List<String> getEventUpper() { return eventUpper; }
    public List<String> getEventLower() { return eventLower; }
    public List<String> getBuildUpper() { return buildUpper; }
    public List<String> getBuildLower() { return buildLower; }
}