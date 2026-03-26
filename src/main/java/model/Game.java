package model;

import model.states.SetUpGameState;
import java.io.*;
import java.util.*;


public class Game implements IGame{


    //ATTRIBUTI:

    private Board board;
    private Deck<TribeCard> deckTribu;
    private Deck<BuildingCard> deckBuildings;
    private List<Player> players;

    private int currentEra;
    private int roundCount;
    private Player currentPlayer;

    private GameState currentState;

    private List<IGameObserver> observers;

    // I campi offeringTiles, turnOrderSlots, upperRow, lowerRow sono gestiti da Board
    public Game() {
        this.observers = new ArrayList<>();
        this.players = new ArrayList<>();
        this.currentState = new SetUpGameState();
    }

    private Game currentInstance;

    // ==========================================
    // METODI DELL'INTERFACCIA IGAME
    // ==========================================

    @Override
    public void joinGame(String nickname) throws Exception {
        currentState.joinGame(this, nickname);
        notifyObservers();
    }

    @Override
    public void chooseTotemColor(String nickname, String color) throws Exception {
        currentState.chooseTotemColor(this, nickname, color);
        notifyObservers();
    }

    @Override
    public void placeTotem(String nickname, int offerPosition) throws Exception {
        currentState.placeTotem(this, nickname, offerPosition);
        notifyObservers();
    }

    // @Override
    // public void pickCharacter(String nickname, int cardIndex) throws Exception {
    //     currentState.pickCharacter(this, nickname, cardIndex);
    //     notifyObservers();
    // }

    // @Override
    // public void buyBuilding(String nickname, int cardIndex) throws Exception {
    //     currentState.buyBuilding(this, nickname, cardIndex);
    //     notifyObservers();
    // }

    @Override
    public void pickCard(String nickname, String row, int cardIndex) throws Exception {
        currentState.pickCard(this, nickname, row, cardIndex);
        notifyObservers();
    }

    @Override
    public void addObserver(IGameObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Preparazione completa del gioco MESOS
     * Il numero di giocatori viene determinato dalla lista players
     */
    public void setupGame() {
        this.currentEra = 1;
        this.roundCount = 0;

        this.deckTribu = DeckFactory.createTribeDeck(
                "data/CharacterCardsSetup.json",
                "data/EventCardsSetup.json",
                players.size()
        );
        this.deckBuildings = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", players.size());

        this.board = new Board();
        this.board.generateBoard(this);

        List<Player> tempPlayers = new ArrayList<>(this.players);
        Collections.shuffle(tempPlayers);

        // 4. Passiamo la lista mescolata alla Board
        this.board.setupInitialTurnOrder(tempPlayers);

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

    public void setState(GameState newState) {
        this.currentState = newState;
        notifyObservers(); // Aggiorniamo la grafica al cambio di fase
    }

    public void nextPlayer() {
        if (players.isEmpty()) return;

        // Trova a che punto della lista è il giocatore attuale
        int currentIndex = players.indexOf(currentPlayer);

        // Calcola il prossimo indice (il % players.size() fa ricominciare il giro da 0 se siamo all'ultimo)
        int nextIndex = (currentIndex + 1) % players.size();

        // Aggiorna il giocatore corrente
        this.currentPlayer = players.get(nextIndex);

        System.out.println("Turno passato a: " + currentPlayer.getNickname());

        // Avvisa la grafica che è cambiato il player corrente!
        notifyObservers();
    }

    public Player getPlayerByNickname(String nickname) throws Exception {
        return this.players.stream()
                .filter(p -> p.getNickname().equalsIgnoreCase(nickname))
                .findFirst()
                .orElseThrow(() -> new Exception("Errore: Giocatore '" + nickname + "' non trovato."));
    }

    // METODO DELLA FUNCT INT IGAMEOBSERVER
    // Lo chiami internamente OGNI VOLTA che il gioco cambia stato.
    private void notifyObservers() {
        for (IGameObserver obs : observers) {
            obs.onGameUpdated(this); // Passa se stesso (il Game) all'Observer
        }
    }

    // ==========================================
    // GETTERS & SETTERS ESSENZIALI
    // ==========================================

    public List<Player> getPlayers() { return players; }
    public int getPlayersNumber() { return players.size(); }

    public Board getBoard() { return this.board; }
    public Deck<TribeCard> getTribeDeck() { return deckTribu; }
    public Deck<BuildingCard> getBuildingDeck() { return deckBuildings; }

    public int getCurrentEra() { return currentEra; }
    public int getRoundCount() { return roundCount; }

    public Player getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(Player player) { this.currentPlayer = player; }

    public Player getCurrentPicker() {
        List<OfferingTile> tiles = board.getOfferingTrack();
        
        for (OfferingTile tile : tiles) {
            Player occupant = tile.getOccupiedBy();
            if (occupant != null && !isInTurnOrder(occupant)) {
                return occupant;
            }
        }
        return null;
    }

    private boolean isInTurnOrder(Player p) {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (p.equals(slot.getOccupiedBy())) {
                return true;
            }
        }
        return false;
    }

    public void assignPickerToTurnOrder(Player p) {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (slot.getOccupiedBy() == null) {
                slot.occupy(p);
                return;
            }
        }
    }

    public boolean isPickingComplete() {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (slot.getOccupiedBy() == null) {
                return false;
            }
        }
        return true;
    }


    public boolean isTribeDeckEmpty() {
        return deckTribu.isEmpty();
    }

    // True se la prossima carta del deck e' di un'era successiva
    public boolean isEraTransition() {
        TribeCard nextCard = deckTribu.peek();
        if (nextCard == null) return false;
        return nextCard.getEra() > currentEra;
    }

    public void advanceRound() {
        roundCount++;
        if (isEraTransition()) {
            currentEra++;
        }
    }

    public void applyEndGameBuildingEffects() {
        for (Player p : players) {
            for (BuildingCard b : p.getBuildings()) {
                for (IEffect<BuildingCard> effect : b.getEffects()) {
                    if (effect.isEndgameEffect()) {
                        effect.apply(p, this, b);
                    }
                }
            }
        }
    }

    public int calculatePlayerScore(Player p) {
        int score = 0;

        score += p.getAuraPoints();

        long inventors = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR).count();
        long uniqueInventions = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR)
                .mapToInt(c -> c.getLevel())
                .distinct().count();
        score += (int) (inventors * uniqueInventions);

        long artists = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.ARTIST).count();
        score += (int) (artists / 2) * 10;

        score += p.getBuildings().stream()
                .mapToInt(BuildingCard::getFinalAura).sum();

        return score;
    }

    public List<Player> determineWinners() {
        applyEndGameBuildingEffects();

        List<Player> winners = new ArrayList<>();
        int maxScore = 0;

        for (Player p : players) {
            int score = calculatePlayerScore(p);
            if (score > maxScore) {
                maxScore = score;
                winners.clear();
                winners.add(p);
            } else if (score == maxScore) {
                winners.add(p);
            }
        }

        if (winners.size() > 1) {
            int maxFood = 0;
            for (Player p : winners) {
                maxFood = Math.max(maxFood, p.getFood());
            }
            final int finalMaxFood = maxFood;
            winners = winners.stream()
                    .filter(p -> p.getFood() == finalMaxFood)
                    .toList();
        }

        return winners;
    }

    public void notifyEndGame(List<Player> winners) {
        System.out.println("=== FINE PARTITA ===");
        if (winners.size() == 1) {
            System.out.println("Vincitore: " + winners.get(0).getNickname());
        } else {
            System.out.println("Vittoria condivisa tra: " + 
                winners.stream().map(Player::getNickname).reduce((a, b) -> a + ", " + b).orElse(""));
        }
        notifyObservers();
    }

    /*public void takeCards(Player player, int count, String row) {
        // TODO
    }*/

    
    /*public void nextTurn() {
        // TODO implement here
    }*/

    
   /* public void endGame() {
        // TODO implement here
    }*/

}