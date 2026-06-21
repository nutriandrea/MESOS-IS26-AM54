// In file: network/messages/EventUpdate.java
package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Broadcast notification sent by the server after an event resolution.
 * Contains the complete snapshot of all players' stats
 * (food, aura, magic, tribe, buildings) and the updated board state.
 * Used by the client to instantly update the interface.
 */
public final class EventUpdate implements Update {

    // Compact DTO for a single player's statistics
    public static class PlayerSnapshot implements Serializable {
        public final int food;
        public final int aura;
        public final int magicPoints;
        public final List<String> tribe;
        public final List<String> buildings;

        /**
         * Constructs a player statistics snapshot.
         *
         * @param food        the player's food
         * @param aura        the player's aura points
         * @param magicPoints the player's magic points
         * @param tribe       the list of tribe card strings
         * @param buildings   the list of building card strings
         */
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

    /**
     * Constructs an event-resolution update.
     *
     * @param eventName    the name of the resolved event
     * @param playersStats a map of nickname to player snapshot
     * @param charUpper    the upper row character cards
     * @param charLower    the lower row character cards
     * @param eventUpper   the upper row event cards
     * @param eventLower   the lower row event cards
     * @param buildUpper   the upper row building cards
     * @param buildLower   the lower row building cards
     */
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

    /** @return the event name */
    public String getEventName() { return eventName; }
    /** @return a map of nickname to player snapshot */
    public Map<String, PlayerSnapshot> getPlayersStats() { return playersStats; }
    /** @return the upper row character cards */
    public List<String> getCharUpper() { return charUpper; }
    /** @return the lower row character cards */
    public List<String> getCharLower() { return charLower; }
    /** @return the upper row event cards */
    public List<String> getEventUpper() { return eventUpper; }
    /** @return the lower row event cards */
    public List<String> getEventLower() { return eventLower; }
    /** @return the upper row building cards */
    public List<String> getBuildUpper() { return buildUpper; }
    /** @return the lower row building cards */
    public List<String> getBuildLower() { return buildLower; }

    /**
     * Applies this update to the client model, updating all stats and the board.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateEventSnapshot(this);
    }
}