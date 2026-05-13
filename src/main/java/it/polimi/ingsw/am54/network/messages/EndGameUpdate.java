package it.polimi.ingsw.am54.network.messages;

import java.util.List;
import java.util.Map;

public class EndGameUpdate implements Update {

    private final List<String> winners;
    private final Map<String, Integer> allScores;
    private final int numPlayers;

    public EndGameUpdate(List<String> winners) {
        this.winners = winners;
        this.allScores = Map.of();
        this.numPlayers = 0;
    }

    public EndGameUpdate(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
        this.winners = winners;
        this.allScores = allScores;
        this.numPlayers = numPlayers;
    }

    public List<String> getWinners()          { return winners; }
    public Map<String, Integer> getAllScores() { return allScores; }
    public int getNumPlayers()                { return numPlayers; }
}