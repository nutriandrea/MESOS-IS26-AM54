package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class Game {

    /**
     * Default constructor
     */
    public Game() {
        this.deckFactory = new DeckFactory();
    }

    /**
     * 
     */
    private Board board;

    /**
     * 
     */
    private Stack<TribeCard> deckTribu;

    /**
     * 
     */
    private Stack<BuildingCard> deckBuildings;

    /**
     * 
     */
    private List<Player> players;



    /**
     * 
     */
    private int currentEra;

    /**
     * 
     */
    private int roundCount;

    /**
     * 
     */
    private GameStatus status;

    /**
     * 
     */
    private Player currentPlayer;

    /**
     * 
     */
    private DeckFactory deckFactory;

    // I campi offeringTiles, turnOrderSlots, upperRow, lowerRow sono gestiti da Board







    /**
     * @return
     */
    public int getPlayerNumber() {
        // TODO implement here
        return 0;
    }

    /**
     * @return
     */
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
        
        // 1. Preparazione tessere Offerta (delega a Board)
        prepareOfferingTiles();
        
        // 2. Preparazione Ordine di Turno (delega a Board)
        prepareTurnOrderSlots();
        
        // 3. Preparazione mazzo Tribù (delega a DeckFactory)
        this.deckTribu = deckFactory.createTribeDeck(
            "data/CharacterCardsSetup.txt", 
            "data/EventCardsSetup.txt"
        );
        
        // 4. Fila inferiore (giocatori + 1)
        prepareLowerRow();
        
        // 5. Fila superiore (giocatori + 4)
        prepareUpperRow();
        
        // 6. Preparazione mazzi Edificio (delega a DeckFactory)
        this.deckBuildings = deckFactory.createBuildingDeck("data/BuildingCardsSetup.txt");
    }

    private void prepareOfferingTiles() {
        // Carica le 7 tessere A-G (hardcoded per ora)
        // In un'implementazione reale, caricherebbe da file
        // Delega a Board
        // board.setOfferingTiles(...);
    }

    private void prepareTurnOrderSlots() {
        // Crea slot per ordine di turno in base al numero di giocatori
        // Delega a Board
        // board.createTurnOrderSlots(players.size());
    }



    private void prepareLowerRow() {
        // Pescare (giocatori + 1) carte dalla fila inferiore
        // Delega a Board
        // board.fillLowerRow(players.size() + 1, deckTribu);
    }

    private void prepareUpperRow() {
        // Pescare (giocatori + 4) carte dalla fila superiore
        // Delega a Board
        // board.fillUpperRow(players.size() + 4, deckTribu);
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

    /**
     * Funzione principale per avviare il gioco
     * @param players Lista dei giocatori
     */
    public void startGame(List<Player> players) {
        this.players = players;
        setupGame();
        // Distribuzione Cibo iniziale
        distributeInitialFood();
        // Dopo la preparazione, la partita è pronta per iniziare
        // Il primo round verrà avviato separatamente
    }

    /**
     * @param Player p 
     * @param TurnOrderSlot selectedSlot 
     * @return
     */
    public void placeTotemOrder(Player p, TurnOrderSlot selectedSlot) {
        // Delega a Board per piazzare il Totem
        // board.placeTotem(p, selectedSlot);
    }

    /**
     * @return
     */
    public void startRound() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void pickingPhase() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void resolveEvents() {
        // Delega a Board per risolvere gli eventi
        board.resolveEvents();
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

    /**
     * @return
     */
    public void nextTurn() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void handleEraTransition() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void calculateFinalScore() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void endGame() {
        // TODO implement here
    }

}