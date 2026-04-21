package it.polimi.ingsw.am54.network.messages;

import java.util.List;

public class EndGameUpdate implements Update {

    private final List<String> winners;

    public EndGameUpdate(List<String> winners) {
        this.winners = winners;
    }

    public List<String> getWinners() { return winners; }
}