package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import java.util.*;

/**
 * Rappresenta lo stato logico del gioco lato Client.
 * Viene aggiornato dal ServerHandler in ricezione dei messaggi di Update.
 */
public class ClientModel {

    // --- STATO GLOBALE ---
    private int currentRound = 1;
    private String currentEra = "I";
    private String currentState = "Lobby"; // Stato iniziale di default
    private final List<String> turnOrderTrack = new ArrayList<>();
    private final Map<Integer, String> offeringTrack = new TreeMap<>();

    // Tabellone (ID delle carte o rappresentazione testuale)
    private final List<String> topRow = new ArrayList<>();
    private final List<String> bottomRow = new ArrayList<>();

    // --- STATO GIOCATORI ---
    private final List<String> players = new ArrayList<>();
    private final Map<String, Integer> playerFood = new HashMap<>();
    private final Map<String, Integer> playerPP = new HashMap<>();
    private final Map<String, TotemColors> playerColors = new HashMap<>();
    private String currentPlayer;

    // --- STATO FINALE E NOTIFICHE ---
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

    public synchronized void updateCardPicked(String nickname, String row, int index) {
        List<String> targetRow = row.equalsIgnoreCase("TOP") ? topRow : bottomRow;
        if (index >= 0 && index < targetRow.size()) {
            targetRow.set(index, null); // Rimuove la carta dal tavolo
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
        playerPP.put(nickname, pp);
    }

    public synchronized void updateEra(String era) {
        this.currentEra = era;
    }

    // --- GETTER ---
    public synchronized String getCurrentState() { return currentState; }
    public synchronized Map<String, TotemColors> getPlayerColors() { return playerColors; }
    public synchronized List<String> getWinners() { return winners; }
    public synchronized String getLastErrorMessage() { return lastErrorMessage; }
    public synchronized Map<Integer, String> getOfferingTrack() { return offeringTrack; }
    public synchronized List<String> getTurnOrderTrack() { return turnOrderTrack; }
    public synchronized List<String> getTopRow() { return topRow; }
    public synchronized List<String> getBottomRow() { return bottomRow; }
    public synchronized String getCurrentPlayer() { return currentPlayer; }
    public synchronized List<String> getPlayers() { return players; }
}