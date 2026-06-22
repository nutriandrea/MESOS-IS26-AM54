package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Sent from a client to the server to request the full leaderboard
 * for matches with a specific number of players.
 */
public final class GetLeaderboardRequest implements Request {
    private final int numPlayers;

    public GetLeaderboardRequest(int numPlayers) {
        this.numPlayers = numPlayers;
    }

    public int getNumPlayers() { return numPlayers; }

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleGetLeaderboard(connection, playerNickname, this.numPlayers);
    }
}
