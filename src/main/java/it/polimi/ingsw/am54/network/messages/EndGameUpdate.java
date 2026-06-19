package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;
import java.util.List;
import java.util.Map;

/**
 * Notifica broadcast inviata dal server quando la partita termina.
 * Contiene la lista dei vincitori, la mappa punteggi completi
 * e il numero di giocatori della partita.
 */
public final class EndGameUpdate implements Update {
    private final List<String> winners;
    private final Map<String, Integer> allScores;
    private final int numPlayers;

    public EndGameUpdate(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
        this.winners = winners;
        this.allScores = allScores;
        this.numPlayers = numPlayers;
    }

    public List<String> getWinners() { return winners; }
    public Map<String, Integer> getAllScores() { return allScores; }
    public int getNumPlayers() { return numPlayers; }

    @Override
    public void applyTo(ClientModel model) {
        model.updateEndGame(this.winners, this.allScores, this.numPlayers);
    }
}