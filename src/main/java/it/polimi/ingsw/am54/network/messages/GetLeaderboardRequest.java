package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Sent from a client to the server to request the full leaderboard
 * for matches with a specific number of players.
 */
public final class GetLeaderboardRequest implements Request {
    private final int numPlayers;

    /**
     * Constructs a leaderboard request for matches with the given player count.
     *
     * @param numPlayers the number of players to filter by
     */
    public GetLeaderboardRequest(int numPlayers) {
        this.numPlayers = numPlayers;
    }

    /** @return the number-of-players filter */
    public int getNumPlayers() { return numPlayers; }

    /**
     * Forwards the leaderboard request to the controller.
     *
     * @param controller      the server-side game controller
     * @param playerNickname  the nickname of the requesting player
     * @param connection       the client connection to reply on
     */
    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleGetLeaderboard(connection, this.numPlayers);
    }
}
