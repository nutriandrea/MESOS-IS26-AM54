package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class Board {

    /**
     * TEST GIT constructor
     */
    public Board() {
    }

    /**
     * 
     */
    private List<Card> upperRow;

    /**
     * 
     */
    private List<Card> lowerRow;

    /**
     * 
     */
    private List<OfferingTile> offeringTrack;

    /**
     * 
     */
    private List<TurnOrderSlot> turnOrderTile;





    /**
     * @param int numPlayers 
     * @return
     */
    public void refillUpperRow(int numPlayers) {
        // TODO implement here
    }

    /**
     * @return
     */
    public void moveUpperToLower() {
        // TODO implement here
    }

    /**
     * Genera la board iniziale
     */
    public void generateBoard(int numPlayers) {
        this.upperRow = new ArrayList<>();
        this.lowerRow = new ArrayList<>();
        this.offeringTrack = new ArrayList<>();
        this.turnOrderTile = new ArrayList<>();
        
        // Inizializza le liste (verranno popolate da Game)
    }

    /**
     * @return
     */
    public void moveBuildings() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void removeCard() {
        // TODO implement here
    }

    /**
     * @return
     */
    public List<Card> getRow() {
        // TODO implement here
        return null;
    }

    /**
     * Piazza un Totem su una tessera Offerta
     */
    public void placeTotem(Player player, OfferingTile tile) {
        // TODO: Implementare la logica di piazzamento
        // Per ora, solo placeholder
    }

    /**
     * Prende carte da una fila
     * @param player Giocatore che prende le carte
     * @param count Numero di carte da prendere
     * @param row "upper" o "lower"
     * @return Lista di carte prese
     */
    public List<Card> takeCards(Player player, int count, String row) {
        List<Card> takenCards = new ArrayList<>();
        List<Card> sourceRow = row.equals("upper") ? upperRow : lowerRow;
        
        for (int i = 0; i < count && !sourceRow.isEmpty(); i++) {
            Card card = sourceRow.remove(0);
            if (card != null) {
                takenCards.add(card);
            }
        }
        
        return takenCards;
    }

    /**
     * Risolve tutti gli eventi nella fila inferiore
     */
    public void resolveEvents() {
        // TODO: Implementare la risoluzione degli eventi
        // Per ora, solo placeholder
    }

}