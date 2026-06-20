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

    /**
     * Constructs a leaderboard entry.
     *
     * @param nickname   the player's nickname
     * @param score      the final score achieved
     * @param numPlayers the number of players in the match
     * @param matchDate  the date/time of the match
     * @param position   the rank position (1-based)
     */
    public LeaderboardEntry(String nickname, int score, int numPlayers,
                            LocalDateTime matchDate, int position) {
        this.nickname = nickname;
        this.score = score;
        this.numPlayers = numPlayers;
        this.matchDate = matchDate != null ? matchDate.toString() : null;
        this.position = position;
    }

    /** @return the player's nickname */
    public String getNickname()    { return nickname; }
    /** @return the final score */
    public int getScore()          { return score; }
    /** @return the number of players in the match */
    public int getNumPlayers()     { return numPlayers; }
    /** @return the match date as an ISO-8601 string */
    public String getMatchDate()   { return matchDate; }
    /** @return the rank position (1-based) */
    public int getPosition()       { return position; }
}
