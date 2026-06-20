package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.Request;

/**
 * Generic Interface for the connection to the Server.
 * Hides whether we are using Socket or RMI.
 * Implementazioni: SocketServerConnection (TCP), RMIServerConnection (RMI).
 */
public interface ServerConnection {
    /**
     * Requests to join the game with the given nickname.
     *
     * @param nickname the nickname to join with
     */
    void joinGame(String nickname);

    /** Requests the server to start the game setup (host only). */
    void setupGame();

    /**
     * Chooses the player's totem color.
     *
     * @param color the chosen totem color
     */
    void chooseTotem(TotemColors color);

    /**
     * Places the player's totem on an offering tile.
     *
     * @param position the offering tile index
     */
    void placeTotem(int position);

    /**
     * Draws a card from the specified row.
     *
     * @param row   "upper" or "lower"
     * @param index the card index within the row
     */
    void pickCard(String row, int index);

    /** Ends the current player's turn. */
    void endTurn();

    /**
     * Requests the leaderboard for matches with the given player count.
     *
     * @param numPlayers the number-of-players filter
     */
    void requestLeaderboard(int numPlayers);

    /** Closes the connection with the server. */
    void disconnect();
}
