package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A single entry in the leaderboard.
 * Sent from server to client as part of a LeaderboardUpdate.
 */
public class LeaderboardEntry implements Serializable {
    private final String nickname;
    private final int score;
    private final int numPlayers;
    private final String matchDate;   // ISO-8601 string (LocalDateTime serialisation-safe)
    private final int position;

    public LeaderboardEntry(String nickname, int score, int numPlayers,
                            LocalDateTime matchDate, int position) {
        this.nickname = nickname;
        this.score = score;
        this.numPlayers = numPlayers;
        this.matchDate = matchDate != null ? matchDate.toString() : null;
        this.position = position;
    }

    public String getNickname()    { return nickname; }
    public int getScore()          { return score; }
    public int getNumPlayers()     { return numPlayers; }
    public String getMatchDate()   { return matchDate; }
    public int getPosition()       { return position; }
}