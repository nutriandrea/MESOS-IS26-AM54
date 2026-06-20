package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.EndGameUpdate;
import it.polimi.ingsw.am54.network.messages.EventUpdate;
import it.polimi.ingsw.am54.network.messages.LeaderboardUpdate;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import it.polimi.ingsw.am54.network.messages.Update;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * RMI Interface for callbacks from the Server to the Client.
 * The server calls these methods on the remote client stub
 * to notify game events, state updates, heartbeat, etc.
 */
public interface RMIClientInterface extends Remote {
    /**
     * Notifies the client that a new player has joined.
     *
     * @param nickname the joined player's nickname
     * @throws RemoteException if the RMI call fails
     */
    void onPlayerJoined(String nickname) throws RemoteException;

    /**
     * Notifies the client that a player has chosen their totem color.
     *
     * @param nickname the choosing player's nickname
     * @param color    the chosen totem color
     * @throws RemoteException if the RMI call fails
     */
    void onTotemColorChosen(String nickname, TotemColors color) throws RemoteException;

    /**
     * Notifies the client that a player has placed their totem.
     *
     * @param nickname the placing player's nickname
     * @param position the offering tile position
     * @throws RemoteException if the RMI call fails
     */
    void onTotemPlaced(String nickname, int position) throws RemoteException;

    /**
     * Notifies the client that a player's totem was returned from the offering track.
     *
     * @param nickname the returning player's nickname
     * @param position the vacated offering tile position
     * @param food     the player's updated food balance
     * @param aura     the player's updated aura balance
     * @throws RemoteException if the RMI call fails
     */
    void onTotemReturned(String nickname, int position, int food, int aura) throws RemoteException;

    /**
     * Notifies the client that an event has been resolved, sending the
     * full player-stats snapshot and updated board rows.
     *
     * @param name       the resolved event name
     * @param stats      a map of nickname to player snapshot
     * @param charUp     the upper row character cards
     * @param charLow    the lower row character cards
     * @param eventUp    the upper row event cards
     * @param eventLow   the lower row event cards
     * @param buildUp    the upper row building cards
     * @param buildLow   the lower row building cards
     * @throws RemoteException if the RMI call fails
     */
    void onEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats,
                         List<String> charUp, List<String> charLow, List<String> eventUp, List<String> eventLow,
                         List<String> buildUp, List<String> buildLow)  throws RemoteException;

    /**
     * Notifies the client that a player has drawn a card.
     *
     * @param nickname   the drawing player's nickname
     * @param row        the row drawn from
     * @param cardIndex  the index of the drawn card
     * @param newFood    the player's updated food balance
     * @param newMagic   the player's updated magic balance
     * @param offering   the full offering track snapshot
     * @throws RemoteException if the RMI call fails
     */
    void onCardPicked(String nickname, String row, int cardIndex,
                      int newFood, int newMagic, Map<Integer, String> offering)
            throws RemoteException;

    /**
     * Notifies the client that the active player has changed.
     *
     * @param nickname the new current player's nickname
     * @throws RemoteException if the RMI call fails
     */
    void onTurnChanged(String nickname) throws RemoteException;

    /**
     * Notifies the client of a full state change, sending the complete
     * game state snapshot.
     *
     * @param stateName       the new state name
     * @param era             the current era
     * @param currentRound    the current round
     * @param players         a map of nickname to player snapshot
     * @param turnOrder       the turn order track entries
     * @param offering        the offering track entries
     * @param charUp          the upper row character cards
     * @param charLow         the lower row character cards
     * @param eventUp         the upper row event cards
     * @param eventLow        the lower row event cards
     * @param buildUp         the upper row building cards
     * @param buildLow        the lower row building cards
     * @throws RemoteException if the RMI call fails
     */
    void onStateChanged(String stateName, int era, int currentRound, Map<String, StateChangedUpdate.PlayerSnapshot> players,
                        List<String> turnOrder, Map<Integer, String> offering, List<String> charUp,
                        List<String> charLower, List<String> eventUp, List<String> eventLower,
                        List<String> buildUp, List<String> buildLower)
            throws RemoteException;

    /**
     * Notifies the client that the game has ended.
     *
     * @param update the end-game update containing winners and scores
     * @throws RemoteException if the RMI call fails
     */
    void onEndGame(EndGameUpdate update) throws RemoteException;

    /**
     * Sends an error message to the client.
     *
     * @param message the error description
     * @throws RemoteException if the RMI call fails
     */
    void onErrorMessage(String message) throws RemoteException;

    /**
     * Sends leaderboard data to the client.
     *
     * @param update the leaderboard update
     * @throws RemoteException if the RMI call fails
     */
    void onLeaderboardUpdate(LeaderboardUpdate update) throws RemoteException;

    /**
     * Notifies the client that a player has disconnected.
     *
     * @param nickname the disconnected player's nickname
     * @throws RemoteException if the RMI call fails
     */
    void onPlayerDisconnected(String nickname) throws RemoteException;

    /**
     * Notifies the client that a player has reconnected.
     *
     * @param nickname the reconnected player's nickname
     * @throws RemoteException if the RMI call fails
     */
    void onPlayerReconnected(String nickname) throws RemoteException;

    /**
     * Notifies the client that the game has been suspended.
     *
     * @throws RemoteException if the RMI call fails
     */
    void onGameSuspended() throws RemoteException;

    /**
     * Notifies the client that the game has been resumed.
     *
     * @throws RemoteException if the RMI call fails
     */
    void onGameResumed() throws RemoteException;

    /**
     * Heartbeat ping from the server to verify the client is alive.
     *
     * @throws RemoteException if the RMI call fails
     */
    void ping() throws RemoteException; // Per l'heartbeat
}
