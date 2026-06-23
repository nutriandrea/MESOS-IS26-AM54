package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;
import java.util.List;

/**
 * Sent from the server to a client to deliver the leaderboard data.
 * Contains the position of the requesting player and the full ranking list
 * for matches with a given number of players.
 */
public final class LeaderboardUpdate implements Update {

    /** Position of the player in this leaderboard (1-based, -1 = not ranked). */
    private final int playerPosition;

    /** Full list of leaderboard entries, ordered by score descending. */
    private final List<LeaderboardEntry> entries;

    /** Number of players filter used for this leaderboard. */
    private final int numPlayers;

    public LeaderboardUpdate(int playerPosition, List<LeaderboardEntry> entries, int numPlayers) {
        this.playerPosition = playerPosition;
        this.entries = entries;
        this.numPlayers = numPlayers;
    }

    public int getPlayerPosition()            { return playerPosition; }
    public List<LeaderboardEntry> getEntries() { return entries; }
    public int getNumPlayers()                { return numPlayers; }

    @Override
    public void applyTo(ClientModel model) {
        model.updateLeaderboard(this.playerPosition, this.entries, this.numPlayers);
    }
}