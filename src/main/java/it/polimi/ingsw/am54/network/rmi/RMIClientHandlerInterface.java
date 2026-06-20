package it.polimi.ingsw.am54.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI Interface for Client requests to the Server.
 * Every game action has a dedicated method (not a generic sendRequest).
 * The server-side implementation (RMIClientHandler) forwards to the Controller.
 */
public interface RMIClientHandlerInterface extends Remote {
    // RMI exploited to the max doesn't work like socket,
    // but has a specific method for each action the client can call

    /**
     * Requests to join the game with the given nickname.
     *
     * @param nickname the nickname to join with
     * @throws RemoteException if the RMI call fails
     */
    void joinGameRequest(String nickname) throws RemoteException;

    /**
     * Requests the server to start the game setup.
     *
     * @throws RemoteException if the RMI call fails
     */
    void setupGameRequest() throws RemoteException;

    /**
     * Chooses the player's totem color.
     *
     * @param color the color name (e.g. "RED", "BLUE")
     * @throws RemoteException if the RMI call fails
     */
    void chooseTotemColorRequest(String color) throws RemoteException;

    /**
     * Places the player's totem on an offering tile.
     *
     * @param offerPosition the offering tile index
     * @throws RemoteException if the RMI call fails
     */
    void placeTotemRequest(int offerPosition) throws RemoteException;

    /**
     * Draws a card from the specified row.
     *
     * @param row       "upper" or "lower"
     * @param cardIndex the card index within the row
     * @throws RemoteException if the RMI call fails
     */
    void pickCardRequest(String row, int cardIndex) throws RemoteException;

    /**
     * Ends the current player's turn.
     *
     * @throws RemoteException if the RMI call fails
     */
    void endTurnRequest() throws RemoteException;

    /**
     * Requests the leaderboard for matches with the given player count.
     *
     * @param numPlayers the number-of-players filter
     * @throws RemoteException if the RMI call fails
     */
    void getLeaderboardRequest(int numPlayers) throws RemoteException;

    /**
     * Responds to a server heartbeat ping, confirming the client is alive.
     *
     * @throws RemoteException if the RMI call fails
     */
    void pong() throws RemoteException;


    /**
     * Voluntarily disconnects from the server.
     *
     * @throws RemoteException if the RMI call fails
     */
    void disconnect() throws RemoteException;
}
