package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import java.util.*;

public class ClientModel {

    // --- 1. STATO GLOBALE ---
    private int currentRound = 1;
    private int currentEra = 1;
    private String currentState = "Lobby";
    private String currentPlayer;

    // --- 2. TABELLONE (BOARD) ---
    private final List<String> turnOrderTrack = new ArrayList<>();
    private final Map<Integer, String> offeringTrack = new TreeMap<>();

    // Carte sul tavolo (Salvi gli ID delle carte o i Nomi per caricare le immagini)
    private final List<String> characterUpperRow = new ArrayList<>();
    private final List<String> characterLowerRow = new ArrayList<>();
    private final List<String> eventUpperRow = new ArrayList<>();
    private final List<String> eventLowerRow = new ArrayList<>();
    private final List<String> upperBuildingRow = new ArrayList<>();
    private final List<String> lowerBuildingRow = new ArrayList<>();

    // --- 3. STATO GIOCATORI (MAPPE indicizzate per Nickname) ---
    private final List<String> players = new ArrayList<>();
    private final Map<String, TotemColors> playerColors = new HashMap<>();

    // Risorse e Sconti
    private final Map<String, Integer> playerFood = new HashMap<>();
    private final Map<String, Integer> playerAura = new HashMap<>();

    // Inventario Carte
    private final Map<String, List<String>> playerTribe = new HashMap<>();
    private final Map<String, List<String>> playerBuildings = new HashMap<>();

    // --- 4. EXTRA ---
    private List<String> winners = new ArrayList<>();
    private String lastErrorMessage;

    // --- METODI DI AGGIORNAMENTO (Sincronizzati con ServerHandler) ---
    public synchronized void updatePlayerJoined(String nickname) {
        if (!players.contains(nickname)) {
            players.add(nickname);
            turnOrderTrack.add(nickname);
        }
    }

    public synchronized void updateTurn(String nickname) {
        this.currentPlayer = nickname;
    }

    public synchronized void updateTotemPlacement(String nickname, int position) {
        turnOrderTrack.remove(nickname);
        offeringTrack.put(position, nickname);
    }

    public synchronized void updateCardPicked(String nickname, String row, int index, int newFoodBalance) {
        boolean isUpper = row.equalsIgnoreCase("upper");
        if(isUpper){
            if(index < characterUpperRow.size()) {
                //tolgo da char upper row e aggiungo a tribe
                String temp = characterUpperRow.get(index);
                characterUpperRow.set(index, null);
                playerTribe.get(nickname).add(temp);
            } else {
                int buildingIndex = index - characterUpperRow.size();
                String temp = upperBuildingRow.get(buildingIndex);
                upperBuildingRow.set(buildingIndex, null);
                playerFood.put(nickname, newFoodBalance);
                playerBuildings.get(nickname).add(temp);
            }
        } else {
            if(index < characterLowerRow.size()) {
                //tolgo da char lower row e aggiungo a tribe
                String temp = characterLowerRow.get(index);
                characterLowerRow.set(index, null);
                playerTribe.get(nickname).add(temp);
            } else {
                int buildingIndex = index - characterLowerRow.size();
                String temp = lowerBuildingRow.get(buildingIndex);
                lowerBuildingRow.set(buildingIndex, null);
                playerFood.put(nickname, newFoodBalance);
                playerBuildings.get(nickname).add(temp);
            }
        }

        // Il totem torna nel turn order dopo aver preso la carta
        offeringTrack.values().remove(nickname);
        if (!turnOrderTrack.contains(nickname)) {
            turnOrderTrack.add(nickname);
        }

    }

    public synchronized void updateState(String stateName) {
        this.currentState = stateName;
    }

    public synchronized void updateTotemColor(String nickname, TotemColors color) {
        playerColors.put(nickname, color);
    }

    public synchronized void updateEndGame(List<String> winners) {
        this.winners = winners;
    }

    public synchronized void updateError(String message) {
        this.lastErrorMessage = message;
    }

    public synchronized void updateStats(String nickname, int food, int pp) {
        playerFood.put(nickname, food);
        playerAura.put(nickname, pp);
    }

    public synchronized void updateEra(int era) {
        this.currentEra = era;
    }

    // --- GETTER ---
    public synchronized String getCurrentState() { return currentState; }
    public synchronized Map<String, TotemColors> getPlayerColors() { return playerColors; }
    public synchronized List<String> getWinners() { return winners; }
    public synchronized String getLastErrorMessage() { return lastErrorMessage; }
    public synchronized Map<Integer, String> getOfferingTrack() { return offeringTrack; }
    public synchronized List<String> getTurnOrderTrack() { return turnOrderTrack; }
    public synchronized List<String> getCharacterUpperRow() { return characterUpperRow; }
    public synchronized List<String> getCharacterLowerRow() { return characterLowerRow; }
    public synchronized List<String> getEventUpperRow() { return eventUpperRow; }
    public synchronized List<String> getEventLowerRow() { return eventLowerRow; }
    public synchronized List<String> getUpperBuildingRow() { return upperBuildingRow; }
    public synchronized List<String> getLowerBuildingRow() { return lowerBuildingRow; }
    public synchronized String getCurrentPlayer() { return currentPlayer; }
    public synchronized List<String> getPlayers() { return players; }
}