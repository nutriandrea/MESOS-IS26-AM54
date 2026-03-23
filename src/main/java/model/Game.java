package model;

import java.io.*;
import java.util.*;


public class Game implements IGame{

    // I campi offeringTiles, turnOrderSlots, upperRow, lowerRow sono gestiti da Board
    public Game() {
        this.observers = new ArrayList<>();
        this.players = new ArrayList<>();
    }

    private Game currentInstance;
    private Board board;
    private Deck<TribeCard> deckTribu;
    private Deck<BuildingCard> deckBuildings;
    private List<Player> players;
    private int currentEra;
    private int roundCount;
    private GameStatus status;
    private Player currentPlayer;
    // LA LISTA DEGLI ISCRITTI (Fondamentale per il pattern)
    private List<IGameObserver> observers;

    // ==========================================
    // METODI DELL'INTERFACCIA IGAME
    // ==========================================

    @Override
    public void joinGame(String nickname) throws Exception {
        // TODO

        notifyObservers();
    }

    @Override
    public void chooseTotemColor(String nickname, String color) throws Exception {
        // TODO

        notifyObservers();
    }

    @Override
    public void placeTotem(String nickname, int offerPosition) throws Exception {
        // TODO

        notifyObservers();
    }

    @Override
    public void pickCharacter(String nickname, int cardIndex) throws Exception {
        // TODO

        notifyObservers();
    }

    @Override
    public void buyBuilding(String nickname, int cardIndex) throws Exception {
        // TODO

        notifyObservers();
    }

    @Override
    public void addObserver(IGameObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }


    // METODO DELLA FUNCT INT IGAMEOBSERVER
    // Lo chiami internamente OGNI VOLTA che il gioco cambia stato.
    private void notifyObservers() {
        for (IGameObserver obs : observers) {
            obs.onGameUpdated(this); // Passa se stesso (il Game) all'Observer
        }
    }

    
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
        this.board.generateBoard(this);
        
        this.deckTribu = DeckFactory.createTribeDeck(
            "data/CharacterCardsSetup.json",
            "data/EventCardsSetup.json",
            players.size()
        );

        this.deckBuildings = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", players.size());
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

    public void takeCards(Player player, int count, String row) {
        // TODO
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
    public  int getPlayersNumber() {
        if (currentInstance != null && currentInstance.players != null) {
            return currentInstance.players.size();
        }
        return 0;
    }
    
    public  int getCurrentEra() {
        if (currentInstance != null) {
            return currentInstance.currentEra;
        }
        return 1;
    }
    
    public  Deck<TribeCard> getTribeDeck() {
        if (currentInstance != null) {
            return currentInstance.deckTribu;
        }
        return null;
    }
    
    public  Deck<BuildingCard> getBuildingDeck() {
        if (currentInstance != null) {
            return currentInstance.deckBuildings;
        }
        return null;
    }

    public List<Player> getPlayers() {
        return players;
    }

}