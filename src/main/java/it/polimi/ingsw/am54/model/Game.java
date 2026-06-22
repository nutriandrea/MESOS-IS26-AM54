package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.SetUpGameState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.ingsw.am54.model.states.StartTurnState;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Core orchestrator of the game logic. Manages the state machine, turn order,
 * deck handling, and the overarching domain rules.
 */
public class Game implements IGame {

    private Board board;
    private Deck<TribeCard> deckTribu;
    private Deck<BuildingCard> deckBuildings;
    private List<Player> players;

    private int currentEra;
    private int roundCount;
    private Player currentPlayer;
    private GameState currentState;
    private int activePlayerCount = 0;

    private transient List<IGameObserver> observers = new ArrayList<>();

    public Game() {
        this.observers = new ArrayList<>();
        this.players = new ArrayList<>();
        this.currentState = new SetUpGameState();
    }

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
    }

    @Override
    public void pickCard(String nickname, String row, int cardIndex) throws Exception {
        currentState.pickCard(this, nickname, row, cardIndex);
    }

    @Override
    public void addObserver(IGameObserver observer) {
        if (!getSafeObservers().contains(observer)) {
            getSafeObservers().add(observer);
        }
    }

    @Override
    public void removeObserver(IGameObserver observer) {
        getSafeObservers().remove(observer);
    }

    public void setBoard(Board board) { this.board = board; }

    /**
     * Initializes the entire game pipeline, generating decks, configuring the board,
     * assigning starting resources, and transitioning to the first turn state.
     */
    public void setupGame() throws Exception {
        this.currentEra = 1;
        this.roundCount = 1;

        this.deckTribu = DeckFactory.createTribeDeck(
                "/data/CharacterCardsSetup.json",
                "/data/EventCardsSetup.json",
                players.size()
        );
        this.deckBuildings = DeckFactory.createBuildingDeck("/data/BuildingCardsSetup.json", players.size());

        this.board = new Board();
        this.board.initializeLayout(players.size());

        fillInitialTribeRows();
        fillInitialBuildingRow();

        List<Player> tempPlayers = new ArrayList<>(this.players);
        Collections.shuffle(tempPlayers);
        this.board.setupInitialTurnOrder(tempPlayers);
        distributeInitialFood(tempPlayers);

        this.setCurrentPlayer(tempPlayers.get(0));
        this.setState(new StartTurnState());
    }

    /**
     * Populates the upper and lower tribe rows according to the player count rules.
     */
    private void fillInitialTribeRows() {
        int numPlayers = players.size();
        int requiredCharactersLower = numPlayers + 1;

        while (board.getCharacterRowSize("lower") < requiredCharactersLower) {
            TribeCard card = deckTribu.drawCard();
            if (card != null) {
                card.reveal(board, "lower");
            } else break;
        }

        int requiredTotalUpper = numPlayers + 4;
        while ((board.getCharacterRowSize("upper") + board.getEventRowSize("upper")) < requiredTotalUpper) {
            TribeCard card = deckTribu.drawCard();
            if (card != null) {
                card.reveal(board, "upper");
            } else break;
        }
    }

    /**
     * Populates the building row dynamically based on the current era configuration.
     */
    private void fillInitialBuildingRow() {
        int numPlayers = players.size();
        int cardsToDraw = 0;

        try {
            InputStream is = getClass().getResourceAsStream("/data/EraCounts.json");
            JsonObject countsRoot = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
            JsonArray buildingCounts = countsRoot.getAsJsonObject("buildingCounts").getAsJsonArray(String.valueOf(numPlayers));

            if (buildingCounts != null) {
                cardsToDraw = buildingCounts.get(currentEra - 1).getAsInt();
            } else {
                throw new IllegalStateException("Missing or invalid 'buildingCounts' node.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read EraCounts.json: " + e.getMessage(), e);
        }

        for (int i = 0; i < cardsToDraw; i++) {
            BuildingCard card = deckBuildings.drawCard();
            if (card != null) {
                board.addBuildingCard(card, "upper");
            }
        }
    }

    /**
     * Distributes starting food resources based on the turn order position to balance the game.
     */
    private void distributeInitialFood(List<Player> orderedPlayers) {
        for (int i = 0; i < orderedPlayers.size(); i++) {
            Player player = orderedPlayers.get(i);
            int foodAmount = 0;

            if (i == 0) foodAmount = 2;
            else if (i == 1 || i == 2) foodAmount = 3;
            else if (i == 3 || i == 4) foodAmount = 4;

            player.modifyFood(foodAmount);
        }
    }

    /**
     * Transitions the game state. When moving to a new turn, it performs a cleanup
     * of abandoned totems (e.g., from disconnected players) and reintegrates them securely.
     */
    public void setState(GameState newState) {
        if (newState != null && newState.getClass().getSimpleName().equals("StartTurnState")) {
            if (this.board != null) {
                for (OfferingTile tile : board.getOfferingTrack()) {
                    Player p = tile.getOccupiedBy();
                    if (p != null) {
                        try {
                            tile.getClass().getMethod("freeTile").invoke(tile);
                        } catch (Exception e) {
                            try { tile.getClass().getMethod("freeSlot").invoke(tile); }
                            catch (Exception ex) { tile.occupy(null); }
                        }

                        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
                            if (slot.getOccupiedBy() == null) {
                                slot.occupy(p);
                                p.setHasPlacedTotem(false);
                                break;
                            }
                        }
                    }
                }
            }
            reintegrateWaitingPlayers();
        }

        this.currentState = newState;
        notifyStateChanged(newState);
    }

    /**
     * Ensures any player who reconnected while the turn was active is safely
     * seated back onto the turn order track before the new turn begins.
     */
    private void reintegrateWaitingPlayers() {
        if (this.board == null) return;

        for (Player p : this.players) {
            boolean isPlaying = false;

            for (TurnOrderSlot slot : board.getTurnOrderTile()) {
                if (p.equals(slot.getOccupiedBy())) {
                    isPlaying = true;
                    break;
                }
            }

            if (!isPlaying) {
                for (TurnOrderSlot slot : board.getTurnOrderTile()) {
                    if (slot.getOccupiedBy() == null) {
                        slot.occupy(p);
                        p.setHasPlacedTotem(false);
                        break;
                    }
                }
            }
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    /**
     * Identifies and assigns the turn to the next player in the turn order track
     * who has not yet taken their action.
     */
    public void nextPlayer() {
        List<TurnOrderSlot> turnTrack = board.getTurnOrderTile();
        int currentIndex = 0;
        boolean foundCurrent = false;

        while (currentIndex < turnTrack.size() && !foundCurrent) {
            if (currentPlayer.equals(turnTrack.get(currentIndex).getOccupiedBy())) {
                foundCurrent = true;
            } else {
                currentIndex++;
            }
        }

        int offset = 1;
        while (offset <= turnTrack.size()) {
            int nextIndex = (currentIndex + offset) % turnTrack.size();
            Player nextP = turnTrack.get(nextIndex).getOccupiedBy();

            if (nextP != null && !nextP.hasPlacedTotem()) {
                this.currentPlayer = nextP;
                notifyTurnChanged(currentPlayer);
                return;
            } else {
                offset++;
            }
        }
    }

    public void advanceEra() { this.currentEra++; }
    public void advanceRound() { this.roundCount++; }

    public void refillUpperTribeRow() {
        int targetSize = players.size() + 4;
        while ((board.getCharacterRowSize("upper") + board.getEventRowSize("upper")) < targetSize) {
            TribeCard card = deckTribu.drawCard();
            if (card == null) break;
            card.reveal(board, "upper");
        }
    }

    public void refillUpperBuildings() {
        int cardsToDraw = 0;
        try {
            InputStream is = getClass().getResourceAsStream("/data/EraCounts.json");
            JsonObject countsRoot = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
            JsonArray buildingCounts = countsRoot.getAsJsonObject("buildingCounts").getAsJsonArray(String.valueOf(players.size()));

            if (buildingCounts != null) {
                cardsToDraw = buildingCounts.get(currentEra - 1).getAsInt();
            }
        } catch (Exception e) {
            System.err.println("Error reading EraCounts: " + e.getMessage());
        }

        for (int i = 0; i < cardsToDraw; i++) {
            BuildingCard card = deckBuildings.drawCard();
            if (card != null) {
                board.addBuildingCard(card, "upper");
            }
        }
    }

    public Player getPlayerByNickname(String nickname) throws Exception {
        return this.players.stream()
                .filter(p -> p.getNickname().equalsIgnoreCase(nickname))
                .findFirst()
                .orElseThrow(() -> new Exception("Error: Player '" + nickname + "' not found."));
    }

    // --- NOTIFICATION METHODS ---

    private void notifyPlayerJoined(Player player) {
        for (IGameObserver obs : getSafeObservers()) obs.onPlayerJoined(this, player);
    }

    private void notifyTotemColorChosen(Player player, TotemColors color) {
        for (IGameObserver obs : getSafeObservers()) obs.onTotemColorChosen(this, player, color);
    }

    public void notifyTotemPlaced(Player player, int offerPosition) {
        for (IGameObserver obs : getSafeObservers()) obs.onTotemPlaced(this, player, offerPosition);
    }

    public void notifyCardPicked(Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {
        for (IGameObserver obs : getSafeObservers()) obs.onCardPicked(this, player, row, cardIndex, newFoodBalance, newMagicBalance);
    }

    private void notifyTurnChanged(Player player) {
        for (IGameObserver obs : getSafeObservers()) obs.onTurnChanged(this, player);
    }

    private void notifyStateChanged(GameState newState) {
        for (IGameObserver obs : getSafeObservers()) obs.onStateChanged(this, newState);
    }

    public void notifyTotemReturned(Player player, int position) {
        int finalFood = player.getFood();
        int finalAura = player.getAuraPoints();
        for (IGameObserver obs : getSafeObservers()) {
            obs.onTotemReturned(this, player, position, finalFood, finalAura);
        }
    }

    public void notifyEventResolved(EventCard eventCard) {
        for (IGameObserver obs : getSafeObservers()) {
            obs.onEventResolved(this, eventCard);
        }
    }

    @Override
    public void endTurn(String nickname) throws Exception {
        currentState.endTurn(this, nickname);
    }

    /**
     * Hard-removes a player from the game memory. If the lobby becomes empty
     * post-match, the game safely resets to the initial setup state.
     */
    public synchronized void removePlayerEntirely(String nickname) {
        players.removeIf(p -> p.getNickname().equalsIgnoreCase(nickname));
        if (players.isEmpty() && currentState.shouldResetToSetupOnEmpty()) {
            this.setState(new SetUpGameState());
        }
    }

    // --- STATE RESTORATION ---

    /**
     * Restores transient object references (like the currentPlayer and board slots)
     * after the Game state has been deserialized from a persistence file.
     */
    public void restoreReferences() {
        if (this.currentPlayer != null) {
            try {
                this.currentPlayer = getPlayerByNickname(this.currentPlayer.getNickname());
            } catch (Exception e) {}
        }

        if (this.board != null && this.board.getTurnOrderTile() != null) {
            for (it.polimi.ingsw.am54.model.TurnOrderSlot slot : this.board.getTurnOrderTile()) {
                if (slot.getOccupiedBy() != null) {
                    try {
                        slot.occupy(getPlayerByNickname(slot.getOccupiedBy().getNickname()));
                    } catch (Exception e) {}
                }
            }
        }

        if (this.board != null && this.board.getOfferingTrack() != null) {
            for (it.polimi.ingsw.am54.model.OfferingTile tile : this.board.getOfferingTrack()) {
                if (tile.getOccupiedBy() != null) {
                    try {
                        Player p = getPlayerByNickname(tile.getOccupiedBy().getNickname());
                        // restoreOccupant (not occupy): occupy() would reset the tile's
                        // remaining picks to full, handing the player back picks already used.
                        tile.restoreOccupant(p);
                        p.setCurrentTile(tile);
                    } catch (Exception e) {}
                }
            }
        }
    }

    // --- DYNAMIC TURN ORDER MANAGEMENT ---

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
            if (p.equals(slot.getOccupiedBy())) return true;
        }
        return false;
    }

    /**
     * Dynamically assigns a player returning from the offering track to the
     * first completely clean slot available in the turn order track.
     */
    public TurnOrderSlot assignPickerToTurnOrder(Player p) {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (p.equals(slot.getOccupiedBy())) return slot;
        }

        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (slot.getOccupiedBy() == null) {
                slot.occupy(p);
                return slot;
            }
        }
        return null;
    }

    /**
     * Safely moves a disconnected or skipped player to the very end of the
     * turn queue, ensuring active players retain priority.
     */
    public TurnOrderSlot moveToLastSlot(Player p) {
        List<TurnOrderSlot> slots = board.getTurnOrderTile();

        int freedIndex = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (p.equals(slots.get(i).getOccupiedBy())) {
                slots.get(i).freeSlot();
                freedIndex = i;
                break;
            }
        }

        if (freedIndex >= 0) {
            for (int i = freedIndex; i < slots.size() - 1; i++) {
                Player occupant = slots.get(i + 1).getOccupiedBy();
                slots.get(i + 1).freeSlot();
                if (occupant != null) {
                    slots.get(i).occupy(occupant);
                }
            }
        }

        for (int i = slots.size() - 1; i >= 0; i--) {
            if (slots.get(i).getOccupiedBy() == null) {
                slots.get(i).occupy(p);
                return slots.get(i);
            }
        }

        return null;
    }

    public boolean isPickingComplete() {
        for (OfferingTile tile : board.getOfferingTrack()) {
            Player occupant = tile.getOccupiedBy();
            if (occupant != null && !isInTurnOrder(occupant)) {
                return false;
            }
        }
        return true;
    }

    // --- ENDGAME SCORING & UTILITIES ---

    public boolean isTribeDeckEmpty() { return deckTribu.isEmpty(); }

    public boolean isEraTransition() {
        TribeCard nextCard = deckTribu.peek();
        if (nextCard == null) return false;
        return nextCard.getEra() > currentEra;
    }

    public int getPlayerInventorsBonus(Player p) {
        long inventors = p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.INVENTOR).count();
        long uniqueInventions = p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.INVENTOR)
                .mapToInt(c -> c.getLevel()).distinct().count();
        return (int) (inventors * uniqueInventions);
    }

    public int getPlayerArtistsBonus(Player p) {
        long artists = p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.ARTIST).count();
        return (int) (artists / 2) * 10;
    }

    public int getPlayerConstructorsBonus(Player p) {
        return p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.CONSTRUCTOR)
                .mapToInt(CharacterCard::getAura).sum();
    }

    public int getPlayerBuildingsBaseAura(Player p) {
        return p.getBuildings().stream().mapToInt(BuildingCard::getFinalAura).sum();
    }

    public int calculatePlayerScore(Player p) {
        return p.getAuraPoints() + getPlayerInventorsBonus(p) + getPlayerArtistsBonus(p) +
                getPlayerConstructorsBonus(p) + getPlayerBuildingsBaseAura(p);
    }

    /**
     * Determines the final winner(s) of the game. In case of a tie in Aura points,
     * the player with the most remaining Food breaks the tie.
     */
    public List<Player> determineWinners() {
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
            winners = winners.stream().filter(p -> p.getFood() == finalMaxFood).toList();
        }

        return winners;
    }

    public void notifyEndGame(List<Player> winners) {
        for (IGameObserver obs : getSafeObservers()) {
            obs.onEndGame(this, winners);
        }
    }

    private List<IGameObserver> getSafeObservers() {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        return observers;
    }

    // --- BASIC GETTERS & SETTERS ---
    public List<Player> getPlayers() { return players; }
    public int getPlayersNumber() { return players.size(); }
    public Board getBoard() { return this.board; }
    public Deck<TribeCard> getTribeDeck() { return deckTribu; }
    public Deck<BuildingCard> getBuildingDeck() { return deckBuildings; }
    public void setTribeDeck(Deck<TribeCard> deck) { this.deckTribu = deck; }
    public void setBuildingDeck(Deck<BuildingCard> deck) { this.deckBuildings = deck; }
    public int getCurrentEra() { return currentEra; }
    public int getRoundCount() { return roundCount; }
    public void setCurrentEra(int era) { this.currentEra = era; }
    public void setRoundCount(int round) { this.roundCount = round; }
    public Player getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        notifyTurnChanged(player);
    }
    public void setActivePlayerCount(int count) { this.activePlayerCount = count; }
    public int getActivePlayerCount() { return (activePlayerCount > 0) ? activePlayerCount : players.size(); }
}