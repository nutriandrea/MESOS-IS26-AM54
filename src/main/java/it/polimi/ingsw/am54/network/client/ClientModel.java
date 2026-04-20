package it.polimi.ingsw.am54.network.client;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

public class ClientModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    // --- STATO GLOBALE (Mesos) ---
    private int currentRound = 1;
    private String currentEra = "I";
    private final List<String> turnOrderTrack = new ArrayList<>(); // Ordine sulla tessera turno
    private final Map<Integer, String> offeringTrack = new TreeMap<>(); // Posizione (0,1,2..) -> Nickname

    // Le file di carte (ID delle carte)
    private final List<String> topRow = new ArrayList<>();
    private final List<String> bottomRow = new ArrayList<>();

    // --- STATO GIOCATORI ---
    private final List<String> players = new ArrayList<>();
    private final Map<String, Integer> playerFood = new HashMap<>();
    private final Map<String, Integer> playerPP = new HashMap<>();
    private String currentPlayer;

    public void addObserver(PropertyChangeListener l) { support.addPropertyChangeListener(l); }

    // --- METODI DI AGGIORNAMENTO (Sincronizzati con i tuoi Messages) ---

    public synchronized void updatePlayerJoined(String nickname) {
        players.add(nickname);
        turnOrderTrack.add(nickname);
        support.firePropertyChange("players", null, players);
    }

    public synchronized void updateTurn(String nickname) {
        this.currentPlayer = nickname;
        support.firePropertyChange("turn", null, nickname);
    }

    public synchronized void updateTotemPlacement(String nickname, int position) {
        turnOrderTrack.remove(nickname);
        offeringTrack.put(position, nickname);
        support.firePropertyChange("offeringTrack", null, offeringTrack);
    }

    // Riceve il delta: chi ha preso cosa e da dove (come nel tuo CardPickedUpdate)
    public synchronized void updateCardPicked(String nickname, String row, int index) {
        List<String> targetRow = row.equalsIgnoreCase("TOP") ? topRow : bottomRow;
        if (index >= 0 && index < targetRow.size()) {
            targetRow.set(index, null); // La carta viene rimossa dal tavolo
        }

        // Quando una carta viene presa, il totem torna in fondo al Turn Order
        offeringTrack.values().remove(nickname);
        if (!turnOrderTrack.contains(nickname)) turnOrderTrack.add(nickname);

        support.firePropertyChange("board", null, null);
    }

    public synchronized void updateStats(String nickname, int food, int pp) {
        playerFood.put(nickname, food);
        playerPP.put(nickname, pp);
        support.firePropertyChange("stats", null, nickname);
    }

    public synchronized void updateEra(String era) {
        this.currentEra = era;
        support.firePropertyChange("era", null, era);
    }

    // --- GETTER PER LA VIEW ---
    public Map<Integer, String> getOfferingTrack() { return offeringTrack; }
    public List<String> getTurnOrderTrack() { return turnOrderTrack; }
    public List<String> getTopRow() { return topRow; }
    public List<String> getBottomRow() { return bottomRow; }
}