package model;

import java.io.*;
import java.util.*;

public class Board {

    private List<Card> upperRow;
    private List<Card> lowerRow;
    private List<BuildingCard> buildingCardsUpperRow;
    private List<BuildingCard> buildingCardsLowerRow;
    private List<OfferingTile> offeringTrack;
    private List<TurnOrderSlot> turnOrderTile;

    public Board() {
        this.upperRow = new ArrayList<>();
        this.lowerRow = new ArrayList<>();
        this.buildingCardsUpperRow = new ArrayList<>();
        this.buildingCardsLowerRow = new ArrayList<>();
        this.offeringTrack = new ArrayList<>();
        this.turnOrderTile = new ArrayList<>();
    }

    /**
     * Genera la board iniziale comunicando con la classe Game
     */
    public void generateBoard() {
        // Prendo le informazioni direttamente dal Game manager
        int numPlayers = Game.getPlayersNumber();

        initializeOfferingTiles(numPlayers);
        initializeTurnOrderSlots(numPlayers);

        populateUpperRow();
        populateLowerRow();
        populateUpperRowBuildings();
    }

    /**
     * Inizializza le 7 tessere Offerta (A-G)
     */
    private void initializeOfferingTiles(int numPlayers) {
        offeringTrack.clear();

        // Controllo di sicurezza
        if (numPlayers < 2 || numPlayers > 5) return;

        // Definiamo esattamente quali tessere (A, B, C...) servono per ogni numero di giocatori
        char[][] requiredTiles = {
                {},                                  // 0
                {},                                  // 1
                {'B', 'C', 'E', 'F'},           // 2 giocatori
                {'B', 'C', 'D', 'E', 'F'},      // 3 giocatori
                {'B', 'C', 'D', 'E', 'F', 'G'},      // 4 giocatori
                {'A', 'B', 'C', 'D', 'E', 'F', 'G'}  // 5 giocatori
        };

        // Scorriamo SOLO le lettere necessarie per questa partita
        for (char tileId : requiredTiles[numPlayers]) {
            // Creiamo la tessera passandole la lettera come ID.
            offeringTrack.add(new OfferingTile(tileId));
        }
    }

    private void initializeTurnOrderSlots(int numPlayers) {
        turnOrderTile.clear();
        if (numPlayers < 2 || numPlayers > 5) return;

        for (int i = 1; i <= numPlayers; i++) {
            TurnOrderSlot slot = new TurnOrderSlot();
            slot.createTurnOrderSlot(i, numPlayers);
            turnOrderTile.add(slot);
        }
    }

    public void populateUpperRow() {
        int numPlayers = Game.getPlayersNumber();
        int count = numPlayers + 4;

        upperRow.clear();
        Deck<TribeCard> tribeDeck = Game.getTribeDeck();

        for (int i = 0; i < count; i++) {
            Card card = tribeDeck.drawCard(); // Chiamata alla classe Deck
            if (card != null) {
                upperRow.add(card);
            }
        }
    }

    public void populateLowerRow() {
        int numPlayers = Game.getPlayersNumber();
        int count = numPlayers + 1;

        lowerRow.clear();
        Deck<TribeCard> tribeDeck = Game.getTribeDeck();

        for (int i = 0; i < count; i++) {
            Card card = tribeDeck.drawCard();
            if (card != null) {
                lowerRow.add(card);
            }
        }
    }

public void populateUpperRowBuildings() {
        int numPlayers = Game.getPlayersNumber();
        int currentEra = Game.getCurrentEra();

        // Controllo di sicurezza sul numero di giocatori per evitare ArrayIndexOutOfBoundsException
        if (numPlayers < 2 || numPlayers > 5) return;

        // Tabella dei conteggi [giocatori][era-1]
        int[][] eraCounts = {
                {},                     // 0
                {},                     // 1
                {1, 2, 3},              // 2 giocatori → Era I, II, III
                {2, 2, 4},              // 3 giocatori
                {2, 3, 4},              // 4 giocatori
                {2, 3, 5}               // 5 giocatori
        };

        // Assicuriamoci che l'era sia valida (es. da 1 a 3)
        if (currentEra < 1 || currentEra > 3) return;

        int cardsToDraw = eraCounts[numPlayers][currentEra - 1];
        if (cardsToDraw <= 0) return;

        buildingCardsUpperRow.clear();
        Deck<BuildingCard> buildingDeck = Game.getBuildingDeck();

        for (int i = 0; i < cardsToDraw; i++) {
            BuildingCard card = buildingDeck.drawCard();
            if (card != null) {
                buildingCardsUpperRow.add(card);
            }
        }
    }

    public void moveUpperToLower() {
        lowerRow.addAll(upperRow);
        upperRow.clear();
    }

    public void moveBuildings() {
        buildingCardsLowerRow.addAll(buildingCardsUpperRow);
        buildingCardsUpperRow.clear();
    }

    public void refillUpperRow() {
        int numPlayers = Game.getPlayersNumber();
        int cardsToAdd = (numPlayers + 4) - upperRow.size();

        Deck<TribeCard> tribeDeck = Game.getTribeDeck();
        for (int i = 0; i < cardsToAdd; i++) {
            Card card = tribeDeck.drawCard();
            if (card != null) {
                upperRow.add(card);
            }
        }
    }

    /**
     * Rimuove una carta dalla fila specificata
     */
    public void removeCard(String row) {
        /**
         * Rimuove una carta dalla fila specificata
         */
    }

    /**
     * Ottiene la fila specificata
     */
    public List<Card> getRow(String row) {
        if (row.equalsIgnoreCase("upper")) {
            return new ArrayList<>(upperRow);
        } else {
            return new ArrayList<>(lowerRow);
        }
    }

    /**
     * Piazza un Totem su una tessera Offerta
     */
    public void placeTotem(Player player, OfferingTile tile) {
            tile.occupy(player);
        }

    /**
     * Prende carte da una fila
     */
    public void takeCards() {
       /*TODO*/
        }

public void resolveEvents() {
    /**
     * TODO
     */
    }

public List<OfferingTile> getOfferingTrack() { return offeringTrack; }
public List<TurnOrderSlot> getTurnOrderTile() { return turnOrderTile;}
public List<Card> getUpperRow() { return upperRow; }
public List<Card> getLowerRow() { return lowerRow; }
public List<BuildingCard> getBuildingCardsUpperRow() { return buildingCardsUpperRow; }
public List<BuildingCard> getBuildingCardsLowerRow() { return buildingCardsLowerRow; }

}
