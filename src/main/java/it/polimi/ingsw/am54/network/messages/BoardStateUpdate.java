package it.polimi.ingsw.am54.network.messages;

import java.util.List;

public class BoardStateUpdate implements Update {
    private final int currentEra;
    private final int roundCount;
    private final String currentPlayer;
    private final List<String> playerNames;
    private final List<String> playerColors;
    private final List<String> playerNicknames;

    private final List<Integer> offeringPositions;
    private final List<Integer> offeringFood;
    private final List<Boolean> offeringAvailable;
    private final List<String> offeringOccupiedBy;

    private final List<String> turnOrderNicknames;
    private final List<Integer> turnOrderFood;

    private final List<String> upperRowCards;
    private final List<String> lowerRowCards;
    private final List<String> upperBuildings;
    private final List<String> lowerBuildings;

    public BoardStateUpdate(int currentEra, int roundCount, String currentPlayer,
                           List<String> playerNames, List<String> playerColors,
                           List<Integer> offeringPositions, List<Integer> offeringFood,
                           List<Boolean> offeringAvailable, List<String> offeringOccupiedBy,
                           List<String> turnOrderNicknames, List<Integer> turnOrderFood,
                           List<String> upperRowCards, List<String> lowerRowCards,
                           List<String> upperBuildings, List<String> lowerBuildings) {
        this.currentEra = currentEra;
        this.roundCount = roundCount;
        this.currentPlayer = currentPlayer;
        this.playerNames = playerNames;
        this.playerColors = playerColors;
        this.offeringPositions = offeringPositions;
        this.offeringFood = offeringFood;
        this.offeringAvailable = offeringAvailable;
        this.offeringOccupiedBy = offeringOccupiedBy;
        this.turnOrderNicknames = turnOrderNicknames;
        this.turnOrderFood = turnOrderFood;
        this.upperRowCards = upperRowCards;
        this.lowerRowCards = lowerRowCards;
        this.upperBuildings = upperBuildings;
        this.lowerBuildings = lowerBuildings;
        this.playerNicknames = playerNames;
    }

    public int getCurrentEra() { return currentEra; }
    public int getRoundCount() { return roundCount; }
    public String getCurrentPlayer() { return currentPlayer; }
    public List<String> getPlayerNames() { return playerNames; }
    public List<String> getPlayerColors() { return playerColors; }
    public List<Integer> getOfferingPositions() { return offeringPositions; }
    public List<Integer> getOfferingFood() { return offeringFood; }
    public List<Boolean> getOfferingAvailable() { return offeringAvailable; }
    public List<String> getOfferingOccupiedBy() { return offeringOccupiedBy; }
    public List<String> getTurnOrderNicknames() { return turnOrderNicknames; }
    public List<Integer> getTurnOrderFood() { return turnOrderFood; }
    public List<String> getUpperRowCards() { return upperRowCards; }
    public List<String> getLowerRowCards() { return lowerRowCards; }
    public List<String> getUpperBuildings() { return upperBuildings; }
    public List<String> getLowerBuildings() { return lowerBuildings; }
}