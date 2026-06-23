package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.Request;

/**
 * Generic interface for connection to the Server.
 * Hides whether we are using Socket or RMI.
 */
public interface ServerConnection {
    // Sends messages to the server, one method per type, RMI style
    void joinGame(String nickname);
    void setupGame();
    void chooseTotem(TotemColors color);
    void placeTotem(int position);
    void pickCard(String row, int index);
    void endTurn();
    void requestLeaderboard(int numPlayers);

    // Closes the connection to the server
    void disconnect();
}