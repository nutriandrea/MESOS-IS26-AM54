package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.ClientModel;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Broadcast notification sent by the server to fully synchronize
 * the game state to all clients. Contains every player's state,
 * the turn order, offerings and all cards on the board.
 * Also used for reconnection (sendFullStateOnReconnect).
 */
public final class StateChangedUpdate implements Update {

    // DTO for player stats
    public static class PlayerSnapshot implements Serializable {
        public final int food;
        public final int aura;
        public final int magicPoints;
        public final TotemColors color; // Utile da mandare all'inizio
        public final List<String> tribe;
        public final List<String> buildings;

        /**
         * Constructs a player snapshot.
         *
         * @param food        the player's food
         * @param aura        the player's aura points
         * @param magicPoints the player's magic points
         * @param color       the player's totem color
         * @param tribe       the list of tribe card strings
         * @param buildings   the list of building card strings
         */
        public PlayerSnapshot(int food, int aura, int magicPoints, TotemColors color, List<String> tribe, List<String> buildings) {
            this.food = food;
            this.aura = aura;
            this.magicPoints = magicPoints;
            this.color = color;
            this.tribe = tribe;
            this.buildings = buildings;
        }
    }

    private final int currentEra;
    private final int currentRound;
    private final String stateName;
    private final Map<String, PlayerSnapshot> playersStats;
    private final List<String> turnOrderTrack;
    private final Map<Integer, String> offeringTrack;
    private final List<String> charUpper, charLower, eventUpper, eventLower, buildUpper, buildLower;

    /**
     * Constructs a full state-changed update.
     *
     * @param stateName       the new state name
     * @param currentEra      the current era
     * @param currentRound    the current round
     * @param playersStats    a map of nickname to player snapshot
     * @param turnOrderTrack  the turn order track entries
     * @param offeringTrack   the offering track entries
     * @param charUpper       the upper row character cards
     * @param charLower       the lower row character cards
     * @param eventUpper      the upper row event cards
     * @param eventLower      the lower row event cards
     * @param buildUpper      the upper row building cards
     * @param buildLower      the lower row building cards
     */
    public StateChangedUpdate(String stateName,
                              int currentEra,
                              int currentRound,
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
        this.currentEra = currentEra;
        this.currentRound = currentRound;
    }

    /** @return the new state name */
    public String getStateName() {
        return stateName;
    }

    /** @return a map of nickname to player snapshot */
    public Map<String, PlayerSnapshot> getPlayersStats() {
        return playersStats;
    }

    /** @return the turn order track entries */
    public List<String> getTurnOrderTrack() {
        return turnOrderTrack;
    }

    /** @return the offering track entries */
    public Map<Integer, String> getOfferingTrack() {
        return offeringTrack;
    }

    /** @return the upper row character cards */
    public List<String> getCharUpper() {
        return charUpper;
    }

    /** @return the lower row character cards */
    public List<String> getCharLower() {
        return charLower;
    }

    /** @return the upper row event cards */
    public List<String> getEventUpper() {
        return eventUpper;
    }

    /** @return the lower row event cards */
    public List<String> getEventLower() {
        return eventLower;
    }

    /** @return the upper row building cards */
    public List<String> getBuildUpper() {
        return buildUpper;
    }

    /** @return the lower row building cards */
    public List<String> getBuildLower() {
        return buildLower;
    }

    /** @return the current era */
    public int getCurrentEra() {
        return currentEra;
    }

    /** @return the current round */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * Applies this update to the client model, replacing the full game state.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateState(this);
    }
}