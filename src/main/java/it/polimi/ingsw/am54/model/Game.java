package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.SetUpGameState;

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

    // ==========================================
    // METODI DELL'INTERFACCIA IGAME
    // ==========================================

    @Override
    public void joinGame(String nickname) throws Exception {
        currentState.joinGame(this, nickname);
        notifyPlayerJoined(getPlayerByNickname(nickname));
    }

    @Override
    public void chooseTotemColor(String nickname, String color) throws Exception {
        currentState.chooseTotemColor(this, nickname, color);
        notifyTotemColorChosen(getPlayerByNickname(nickname), TotemColors.valueOf(color.toUpperCase()));
    }

    @Override
    public void placeTotem(String nickname, int offerPosition) throws Exception {
        currentState.placeTotem(this, nickname, offerPosition);
        notifyTotemPlaced(getPlayerByNickname(nickname), offerPosition);
    }

    @Override
    public void pickCard(String nickname, String row, int cardIndex) throws Exception {
        currentState.pickCard(this, nickname, row, cardIndex);
        notifyCardPicked(getPlayerByNickname(nickname), row, cardIndex);
    }

    @Override
    public void addObserver(IGameObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    //preparazione del gioco
    public void setupGame() throws Exception {
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

        distributeInitialFood(tempPlayers);
    }


    private void distributeInitialFood(List<Player> orderedPlayers) {
        // 1°: 2 Cibo
        // 2°-3°: 3 Cibo
        // 4°-5°: 4 Cibo
        for (int i = 0; i < orderedPlayers.size(); i++) {
            Player player = orderedPlayers.get(i);
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
        notifyStateChanged(newState);
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void nextPlayer() {
        List<TurnOrderSlot> turnTrack = board.getTurnOrderTile();

        // troviamo in quale slot si trova il giocatore corrente
        int currentIndex = 0;
        boolean foundCurrent = false;

        while (currentIndex < turnTrack.size() && !foundCurrent) {
            if (currentPlayer.equals(turnTrack.get(currentIndex).getOccupiedBy())) {
                foundCurrent = true;
            } else {
                currentIndex++;
            }
        }

        // passiamo al prossimo slot occupato
        boolean nextFound = false;
        int offset = 1;

        while (offset <= turnTrack.size() && !nextFound) {
            int nextIndex = (currentIndex + offset) % turnTrack.size();
            Player nextP = turnTrack.get(nextIndex).getOccupiedBy();

            // Se in questo slot c'è seduto qualcuno, e non ha ancora piazzato il totem
            if (nextP != null && !nextP.hasPlacedTotem()) {
                this.currentPlayer = nextP; // aggiorniamo il giocatore corrente
                nextFound = true; // condizione soddisfatta
            } else {
                offset++; //posto successivo
            }
        }
        // Se abbiamo effettivamente trovato e cambiato il giocatore, avvisiamo la grafica
        if (nextFound) {
            //nessun print, sarà la view dopo essere notificata a farlo
            notifyTurnChanged(currentPlayer);
        }
    }

    public void advanceEra(){this.currentEra++;}
    public void advanceRound(){this.roundCount++;}



    public Player getPlayerByNickname(String nickname) throws Exception {
        return this.players.stream()
                .filter(p -> p.getNickname().equalsIgnoreCase(nickname))
                .findFirst()
                .orElseThrow(() -> new Exception("Errore: Giocatore '" + nickname + "' non trovato."));
    }

    private void notifyPlayerJoined(Player player) {
        for (IGameObserver obs : observers) obs.onPlayerJoined(this, player);
    }

    private void notifyTotemColorChosen(Player player, TotemColors color) {
        for (IGameObserver obs : observers) obs.onTotemColorChosen(this, player, color);
    }

    private void notifyTotemPlaced(Player player, int offerPosition) {
        for (IGameObserver obs : observers) obs.onTotemPlaced(this, player, offerPosition);
    }

    private void notifyCardPicked(Player player, String row, int cardIndex) {
        for (IGameObserver obs : observers) obs.onCardPicked(this, player, row, cardIndex);
    }

    private void notifyTurnChanged(Player player) {
        for (IGameObserver obs : observers) obs.onTurnChanged(this, player);
    }

    private void notifyStateChanged(GameState newState) {
        for (IGameObserver obs : observers) obs.onStateChanged(this, newState);
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

    public TurnOrderSlot assignPickerToTurnOrder(Player p) {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (slot.getOccupiedBy() == null) {
                slot.occupy(p);
                return slot;
            }
        }
        return null;
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

    // True se la prossima carta del deck è di un'era successiva
    public boolean isEraTransition() {
        TribeCard nextCard = deckTribu.peek();
        if (nextCard == null) return false;
        return nextCard.getEra() > currentEra;
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

        long CONSTRUCTORs = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.CONSTRUCTOR)
                .mapToInt(CharacterCard::getAura).sum();
        score += (int) CONSTRUCTORs;

        score += p.getBuildings().stream()
                .mapToInt(BuildingCard::getFinalAura).sum();

        return score;
    }

    public List<Player> determineWinners() {
        applyEndGameBuildingEffects();

        List<Player> winners = new ArrayList<>();
        int maxScore = -999;

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
        // anche qui il model si limita ad avvisare tutti gli osservatori (le view)
        for (IGameObserver obs : observers) {
            obs.onEndGame(this, winners);
        }
    }
}