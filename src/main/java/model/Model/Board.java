package model.Model;

import java.io.*;
import java.util.*;


public class Board {

    /**
     * TEST GIT constructor
     */
    public Board() {
    }

    
    private List<Card> upperRow;
    
    private List<Card> buildingCardsUpperRow;
    
    private List<Card> buildingCardsLowerRow;

    
    private List<Card> lowerRow;

    
    private List<OfferingTile> offeringTrack;

    
    private List<TurnOrderSlot> turnOrderTile;



    /**
     * @param int numPlayers 
     * @return
     */
    public void refillUpperRow(int numPlayers) {
        // TODO implement here
    }

    
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
        this.populateUpperRow(new Deck<>(), numPlayers); 
        this.populateDownRow(new Deck<>(), numPlayers); 

        this.populateUpperRowBuildings(new Deck<>(), numPlayers);
    }
    
    /**
     * Popola la fila superiore con carte Tribù
     */
private void populateRow(List<Card> row, Deck<TribeCard> deck, int extra) {
    int count = game.numPlayers;
    for (int i = 0; i < count + extra; i++) {
        Card card = deck.drawCard();
        if (card != null) {
            row.add(card);
        }
    }
}

public void populateUpperRow(Deck<TribeCard> tribeDeck) {
    populateRow(upperRow, tribeDeck, 4);
}

public void populateDownRow(Deck<TribeCard> tribeDeck) {
    populateRow(lowerRow, tribeDeck, 2);
}
    
    /**
     * Popola la fila superiore con carte Edificio
     */
public void populateBuildingsUpperRow(Deck<BuildingCard> buildingDeck) {
    int players = game.getNumPlayers();
    int currentEra = game.getPresentEra();

    // Tabella dei conteggi [giocatori][era-1]
    int[][] eraCounts = {
        {},                     // indice 0 non usato
        {},                     // indice 1 non usato
        {1, 2, 3},              // 2 giocatori → Era I, II, III
        {2, 2, 4},              // 3 giocatori
        {2, 3, 4},              // 4 giocatori
        {2, 3, 5}               // 5 giocatori
    };

    int cardsToDraw = eraCounts[players][currentEra - 1];
    if (cardsToDraw <= 0) return;

    for (int i = 0; i < cardsToDraw; i++) {
        BuildingCard card = buildingDeck.drawCard();
        if (card != null) {
            buildingCardsUpperRow.add(card);  // aggiunge direttamente alla lista
        }
    }
}

    public void moveUpperToLower() {
    // Sposta tutte le carte dall'upper row alla lower row
    lowerRow.addAll(upperRow);

    // Svuota l'upper row
    upperRow.clear();
}    }
    
public void moveBuildings() {

    // Sposta tutte le carte dall'upper row alla lower row
    buildingCardsLowerRow.addAll(buildingCardsUpperRow);

    // Svuota l'upper row
    buildingCardsUpperRow.clear();
}

    
    public void removeCard() {
        // TODO implement here
    }

    
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