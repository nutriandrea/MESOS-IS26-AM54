package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;
import java.util.List;
import java.util.Map;

/**
 * Broadcast notification sent by the server when the game ends.
 * Contains the list of winners, the complete scores map
 * and the number of game players.
 */
public final class EndGameUpdate implements Update {
    private final List<String> winners;
    private final Map<String, Integer> allScores;
    private final int numPlayers;

    /**
     * Constructs an end-game notification.
     *
     * @param winners    the list of winner nicknames
     * @param allScores  a map of nickname to final score
     * @param numPlayers the number of players in the match
     */
    public EndGameUpdate(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
        this.winners = winners;
        this.allScores = allScores;
        this.numPlayers = numPlayers;
    }

    /** @return the list of winner nicknames */
    public List<String> getWinners() { return winners; }
    /** @return a map of nickname to final score */
    public Map<String, Integer> getAllScores() { return allScores; }
    /** @return the number of players in the match */
    public int getNumPlayers() { return numPlayers; }

    /**
     * Applies this update to the client model, finalising the game results.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateEndGame(this.winners, this.allScores, this.numPlayers);
    }
}