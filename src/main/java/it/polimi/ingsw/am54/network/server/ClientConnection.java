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
 * The Controller and the VirtualView use this interface without knowing
 * whether they are communicating via Socket or RMI.
 *
 * IMPLEMENTATIONS:
 * - SocketClientHandler: uses ObjectOutputStream to send Updates
 * - RMIClientHandler: uses clientStub.receiveUpdate() to send Updates
 *
 * METHODS:
 * - sendUpdate(Update): sends an update message to the client
 * - disconnect(): closes the connection and cleans up resources
 * - startHeartbeat(): starts the ping/pong system (default = no-op)
 *
 * NOTE ON startHeartbeat():
 * The method has an empty default implementation for backward compatibility.
 * SocketClientHandler and RMIClientHandler override it to start
 * the ping scheduler. If a future implementation does not need
 * a heartbeat, it can simply not override it.
 * ============================================================================
 */
public interface ClientConnection {

    /**
     * Methods to send updates to the client
     */

    // specific methods (no longer the generic sendUpdate(Update))
    void notifyPlayerJoined(String nickname);
    void notifyTotemColor(String nickanme, TotemColors color);
    void notifyTotemPlaced(String nickname, int position);
    void notifyCardPicked(String nickname, String row, int index, int food, int magic, Map<Integer, String> offering);
    void notifyTotemReturned(String nickname, int position, int food, int aura);
    void notifyEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats,
                             List<String> charUp, List<String> charLow,
                             List<String> eventUp, List<String> eventLow,
                             List<String> buildUp, List<String> buildLow);
    void notifyTurnChanged(String nickname);
    void notifyStateChanged( String stateName,
                             int currentEra, int currentRound,
                             Map<String, StateChangedUpdate.PlayerSnapshot> playersStats,
                             List<String> turnOrderTrack,
                             Map<Integer, String> offeringTrack,
                             List<String> charUpper, List<String> charLower,
                             List<String> eventUpper, List<String> eventLower,
                             List<String> buildUpper, List<String> buildLower);
    void notifyEndGame(List<String> winners, Map<String, Integer> allScores, int numPlayers);
    void notifyError(String message);
    void sendPing();
    void sendLeaderboardUpdate(LeaderboardUpdate update);

    /**
     * Methods to handle players disconnecting and reconnecting
     */
    void notifyPlayerDisconnected(String nickname);
    void notifyPlayerReconnected(String nickname);

    /**
     * Notifies the client that the game has been suspended (only 1 active player).
     * Empty default implementation for backward compatibility.
     */
    default void notifyGameSuspended() {}

    /**
     * Notifies the client that the game has resumed.
     * Empty default implementation for backward compatibility.
     */
    default void notifyGameResumed() {}

    /**
     * Closes the connection and cleans up resources.
     * Also calls controller.removeClient() to deregister the player.
     */
    void disconnect();

    /**
     * Starts the heartbeat system (ping/pong) for this connection.
     * Empty default implementation for backward compatibility.
     *
     * SocketClientHandler and RMIClientHandler override this method
     * to start a ScheduledExecutorService that sends pings every 5 seconds.
     */
    default void startHeartbeat() {}
}