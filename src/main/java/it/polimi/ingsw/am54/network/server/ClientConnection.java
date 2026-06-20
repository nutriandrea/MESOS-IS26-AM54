package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.network.messages.EventUpdate;
import it.polimi.ingsw.am54.network.messages.LeaderboardUpdate;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import it.polimi.ingsw.am54.network.messages.Update;
import it.polimi.ingsw.am54.model.TotemColors;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * ClientConnection — Abstraction of the server -> client communication channel
 * ============================================================================
 *
 * ROLE:
 * Interface that abstracts the network transport (TCP Socket or RMI).
 * Controller and VirtualView use this interface without knowing
 * whether they are communicating via Socket or RMI.
 *
 * IMPLEMENTATIONS:
 * - SocketClientHandler: uses ObjectOutputStream to send Update
 * - RMIClientHandler: uses clientStub.receiveUpdate() to send Update
 *
 * METHODS:
 * - sendUpdate(Update): sends an update message to the client
 * - disconnect(): closes the connection and cleans up resources
 * - startHeartbeat(): starts the ping/pong system (default = no-op)
 *
 * NOTA SU startHeartbeat():
 * The method has an empty default implementation for backward compatibility.
 * SocketClientHandler and RMIClientHandler override it to start
 * the ping scheduler. If a future implementation does not need
 * heartbeat, it can simply not override it.
 * ============================================================================
 */
public interface ClientConnection {

    /**
     * Methods to send updates to the client
     */

    /**
     * Notifies the client that a new player has joined.
     *
     * @param nickname the joined player's nickname
     */
    void notifyPlayerJoined(String nickname);

    /**
     * Notifies the client that a player has chosen a totem color.
     *
     * @param nickname the choosing player's nickname
     * @param color    the chosen totem color
     */
    void notifyTotemColor(String nickname, TotemColors color);

    /**
     * Notifies the client that a player has placed their totem.
     *
     * @param nickname the placing player's nickname
     * @param position the offering tile position
     */
    void notifyTotemPlaced(String nickname, int position);

    /**
     * Notifies the client that a player has drawn a card.
     *
     * @param nickname  the drawing player's nickname
     * @param row       the row drawn from
     * @param index     the card index
     * @param food      the player's updated food balance
     * @param magic     the player's updated magic balance
     * @param offering  the full offering track snapshot
     */
    void notifyCardPicked(String nickname, String row, int index, int food, int magic, Map<Integer, String> offering);

    /**
     * Notifies the client that a player's totem was returned from the offering track.
     *
     * @param nickname the returning player's nickname
     * @param position the vacated offering tile position
     * @param food     the player's updated food balance
     * @param aura     the player's updated aura balance
     */
    void notifyTotemReturned(String nickname, int position, int food, int aura);

    /**
     * Notifies the client that an event has been resolved.
     *
     * @param name     the resolved event name
     * @param stats    a map of nickname to player snapshot
     * @param charUp   the upper row character cards
     * @param charLow  the lower row character cards
     * @param eventUp  the upper row event cards
     * @param eventLow the lower row event cards
     * @param buildUp  the upper row building cards
     * @param buildLow the lower row building cards
     */
    void notifyEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats,
                             List<String> charUp, List<String> charLow,
                             List<String> eventUp, List<String> eventLow,
                             List<String> buildUp, List<String> buildLow);

    /**
     * Notifies the client that the active player has changed.
     *
     * @param nickname the new current player's nickname
     */
    void notifyTurnChanged(String nickname);

    /**
     * Notifies the client of a full state change.
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
    void notifyStateChanged( String stateName,
                            int currentEra, int currentRound,
                            Map<String, StateChangedUpdate.PlayerSnapshot> playersStats,
                            List<String> turnOrderTrack,
                            Map<Integer, String> offeringTrack,
                            List<String> charUpper, List<String> charLower,
                            List<String> eventUpper, List<String> eventLower,
                            List<String> buildUpper, List<String> buildLower);

    /**
     * Notifies the client that the game has ended.
     *
     * @param winners    the list of winner nicknames
     * @param allScores  a map of nickname to final score
     * @param numPlayers the number of players in the match
     */
    void notifyEndGame(List<String> winners, Map<String, Integer> allScores, int numPlayers);

    /**
     * Sends an error message to the client.
     *
     * @param message the error description
     */
    void notifyError(String message);

    /** Sends a heartbeat ping to the client. */
    void sendPing();

    /**
     * Sends leaderboard data to the client.
     *
     * @param update the leaderboard update
     */
    void sendLeaderboardUpdate(LeaderboardUpdate update);

    /**
     * Methods to handle players disconnecting and reconnecting
     */

    /**
     * Notifies the client that a player has disconnected.
     *
     * @param nickname the disconnected player's nickname
     */
    void notifyPlayerDisconnected(String nickname);

    /**
     * Notifies the client that a player has reconnected.
     *
     * @param nickname the reconnected player's nickname
     */
    void notifyPlayerReconnected(String nickname);

    /**
     * Notifies the client that the game has been suspended (only 1 active player).
     * Implementazione di default vuota per retrocompatibilita'.
     */
    default void notifyGameSuspended() {}

    /**
     * Notifies the client that the game has been resumed.
     * Implementazione di default vuota per retrocompatibilita'.
     */
    default void notifyGameResumed() {}

    /**
     * Closes the connection and cleans up resources.
     * Also calls controller.removeClient() to unregister the player.
     */
    void disconnect();

    /**
     * Starts the heartbeat system (ping/pong) for this connection.
     * Implementazione di default vuota per retrocompatibilita'.
     *
     * SocketClientHandler and RMIClientHandler override this method
     * to start a ScheduledExecutorService that sends pings every 5 seconds.
     */
    default void startHeartbeat() {}
}
