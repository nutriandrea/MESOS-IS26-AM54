package it.polimi.ingsw.am54.network.messages;

/**
 * Sent from a client to the server to request the full leaderboard
 * for matches with a specific number of players.
 */
public class GetLeaderboardRequest implements Request {
    private final int numPlayers;

    public GetLeaderboardRequest(int numPlayers) {
        this.numPlayers = numPlayers;
    }

    public int getNumPlayers() { return numPlayers; }
}
