package model;

import java.io.*;
import java.util.*;


public class Game {

    /**
     * Default constructor
     */
    public Game() {
        currentInstance = this;
    }

    private static Game currentInstance;

    private Board board;


    private Deck<TribeCard> deckTribu;

    
    private Deck<BuildingCard> deckBuildings;

    
    private List<Player> players;

    
    private int currentEra;

    
    private int roundCount;

    
    private GameStatus status;

    
    private Player currentPlayer;



    // I campi offeringTiles, turnOrderSlots, upperRow, lowerRow sono gestiti da Board


    
    public int getPlayerNumber() {
        // TODO implement here
        return 0;
    }

    public Board getBoard(){
        return this.board;
    }

    
    public Player getCurrentPlayer() {
        // TODO implement here
        return null;
    }

    /**
     * Preparazione completa del gioco MESOS
     * Il numero di giocatori viene determinato dalla lista players
     */
    public void setupGame() {
        this.currentEra = 1;
        this.roundCount = 0;
        this.status = GameStatus.START;
        
        int playerCount = players.size();
        
        // Inizializza board (delega a Board)
        this.board = new Board();
        this.board.generateBoard(playerCount);
        
        // 3. Preparazione mazzo Tribù (delega a DeckFactory)
        this.deckTribu = DeckFactory.createTribeDeck(
            "data/CharacterCardsSetup.txt", 
            "data/EventCardsSetup.txt", 
            players.size()
        );

        // 6. Preparazione mazzi Edificio (delega a DeckFactory)
        this.deckBuildings = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.txt", players.size());
        distributeInitialFood();
    }
    /**
     * Distribuisce il cibo iniziale ai giocatori
     */
    private void distributeInitialFood() {
        // 1°: 2 Cibo
        // 2°-3°: 3 Cibo
        // 4°-5°: 4 Cibo
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            int foodAmount = 0;
            
            if (i == 0) {
                foodAmount = 2;
            } else if (i == 1 || i == 2) {
                foodAmount = 3;
            } else if (i == 3 || i == 4) {
                foodAmount = 4;
            }
            
            player.modifyFood(foodAmount);
        }
    }


    public void placeTotemOrder(Player p, TurnOrderSlot selectedSlot) {
        // Delega a Board per piazzare il Totem
        // board.placeTotem(p, selectedSlot);
    }

    
    public void startRound() {
        // TODO implement here
    }

    
    public void pickingPhase() {
        // TODO implement here
    }

    
    public void resolveEvent(EventCard event) {
        // TODO implement here
    }

    /**
     * Prende carte da una fila e le aggiunge alla tribù del giocatore
     */
    public void takeCards(Player player, int count, String row) {
        // Delega a Board per prendere le carte
        List<Card> cards = board.takeCards(player, count, row);
        // Aggiungi le carte alla tribù del giocatore
        for (Card card : cards) {
            player.modifyMyTribe(card);
        }
    }

    
    public void nextTurn() {
        // TODO implement here
    }

    
    public void handleEraTransition() {
        // TODO implement here
    }

    
    public void calculateFinalScore() {
        // TODO implement here
    }

    
    public void endGame() {
        // TODO implement here
    }

    // Metodi statici per accesso da Board
    public static int getPlayersNumber() {
        if (currentInstance != null && currentInstance.players != null) {
            return currentInstance.players.size();
        }
        return 0;
    }
    
    public static int getCurrentEra() {
        if (currentInstance != null) {
            return currentInstance.currentEra;
        }
        return 1;
    }
    
    public static Deck<TribeCard> getTribeDeck() {
        if (currentInstance != null) {
            return currentInstance.deckTribu;
        }
        return null;
    }
    
    public static Deck<BuildingCard> getBuildingDeck() {
        if (currentInstance != null) {
            return currentInstance.deckBuildings;
        }
        return null;
    }

}