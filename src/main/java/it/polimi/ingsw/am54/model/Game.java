package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.SetUpGameState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import it.polimi.ingsw.am54.model.states.StartTurnState;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Central game class that manages the entire lifecycle of a game:
 * game states, turns, ages/eras, card effects, observer notifications
 * and end-game logic with score calculation.
 * <p>
 * Coordinates the Board, the decks (tribe and buildings) and the player list,
 * delegating validation and execution of actions to the current state
 * via the <b>State</b> pattern. State transitions are managed
 * by calling {@link #setState(GameState)}.
 * </p>
 * <p>
 * Implements the {@link IGame} interface to expose game actions
 * to the network/controller layer, and uses the <b>Observer</b> pattern
 * via {@link IGameObserver} to notify events to the view.
 * </p>
 *
 * @see IGame
 * @see GameState
 * @see Board
 * @see Player
 * @see IGameObserver
 * @author AM54
 */
public class Game implements IGame{


    //ATTRIBUTES:

    /** Game board that manages the turn track, offerings and cards on the table. */
    private Board board;

    /** Tribe card deck (characters + events) from which cards are drawn to populate the rows. */
    private Deck<TribeCard> tribeDeck;

    /** Building card deck from which purchasable buildings are drawn. */
    private Deck<BuildingCard> deckBuildings;

    /** List of players participating in the game. */
    private List<Player> players;

    /** Current game era (1-3). Advances when the tribe deck runs out of current era cards. */
    private int currentEra;

    /** Counter of rounds played (1-10). Determines game progression. */
    private int roundCount;

    /** Player currently taking their turn. */
    private Player currentPlayer;

    /** Current game state (State pattern). Determines which actions are allowed. */
    private GameState currentState;

    /** Number of active players in the current round (excludes disconnected players). */
    private int activePlayerCount = 0;

    /** Thread-safe list of observers for asynchronous notifications to view/network. */
    private transient List<IGameObserver> observers = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new game with empty lists and initial state {@link SetUpGameState}.
     * Players can join immediately after creation.
     */
    public Game() {
        this.observers = new CopyOnWriteArrayList<>();
        this.players = new ArrayList<>();
        this.currentState = new SetUpGameState();
    }

    // ==========================================
    // IGAME INTERFACE METHODS
    // ==========================================

    /**
     * Allows a new player to join the game.
     * Validation and addition are delegated to the current state.
     *
     * @param nickname the name of the player to add
     * @throws GameException if the nickname is already taken, the game has already started,
     *                       or the name is not valid according to the current state's rules
     */
    @Override
    public void joinGame(String nickname) throws GameException {
        currentState.joinGame(this, nickname);
        notifyPlayerJoined(getPlayerByNickname(nickname));
    }

    /**
     * Assigns a totem color to the specified player.
     * The color choice is permanent for the entire game.
     *
     * @param nickname the player's name
     * @param color    the chosen color (e.g. "RED", "BLUE")
     * @throws GameException if the color is not available or the game phase does not allow it
     */
    @Override
    public void chooseTotemColor(String nickname, String color) throws GameException {
        currentState.chooseTotemColor(this, nickname, color);
        notifyTotemColorChosen(getPlayerByNickname(nickname), TotemColors.valueOf(color.toUpperCase()));
    }

    /**
     * Places the player's totem on an offering tile on the track.
     * The placement determines the draw order and bonuses obtainable in the round.
     *
     * @param nickname       the player's name
     * @param offerPosition  the position of the offering tile (numeric index)
     * @throws GameException if the position is invalid, the tile is already occupied,
     *                       or the player has already placed the totem
     */
    @Override
    public void placeTotem(String nickname, int offerPosition) throws GameException {
        currentState.placeTotem(this, nickname, offerPosition);
    }

    /**
     * Allows the player to draw a card from one of the table rows.
     * It can be a character, event or building card.
     *
     * @param nickname  the player's name
     * @param row       the row to draw from ("upper" or "lower")
     * @param cardIndex the index of the card in the row
     * @throws GameException if the card is not purchasable, food is insufficient,
     *                       or the player is not allowed to draw at this moment
     */
    @Override
    public void pickCard(String nickname, String row, int cardIndex) throws GameException {
        currentState.pickCard(this, nickname, row, cardIndex);
    }

    /**
     * Registers an observer to receive updates on the game state.
     * If the observer is already registered, it is not duplicated.
     *
     * @param observer the observer to add
     */
    @Override
    public void addObserver(IGameObserver observer) {
        if (!getSafeObservers().contains(observer)) {
            getSafeObservers().add(observer);
        }
    }

    /**
     * Removes an observer from the list, stopping notifications.
     *
     * @param observer the observer to remove
     */
    @Override
    public void removeObserver(IGameObserver observer) {
        getSafeObservers().remove(observer);
    }

    // ==========================================
    // SETUP PHASE (Refactored)
    // ==========================================

    /**
     * Sets the game Board (used mainly in tests).
     *
     * @param board the Board instance to associate
     */
    public void setBoard(Board board) { this.board = board; }

    /**
     * Executes the complete initial game setup:
     * <ol>
     *   <li>Initializes the decks (tribe and buildings) via {@link DeckFactory}</li>
     *   <li>Creates and initializes the Board with turn track and offerings</li>
     *   <li>Populates the initial rows of tribe and building cards</li>
     *   <li>Calculates random turn order and distributes initial food</li>
     *   <li>Sets the first player and transitions to {@link StartTurnState}</li>
     * </ol>
     *
     * @throws GameException if an error occurs during setup
     */
    public void setupGame() throws GameException {
        this.currentEra = 1;
        this.roundCount = 1;

        // 1. Deck Creation (Delegated to Factory)
        this.tribeDeck = DeckFactory.createTribeDeck(
                "data/CharacterCardsSetup.json",
                "data/EventCardsSetup.json",
                players.size()
        );
        this.deckBuildings = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", players.size());

        // 2. Board initialization
        this.board = new Board();
        this.board.initializeLayout(players.size());

        // 3. Initial card population
        fillInitialTribeRows();
        fillInitialBuildingRow();

        // 4. Random turn order calculation and initial Food distribution
        List<Player> tempPlayers = new ArrayList<>(this.players);
        Collections.shuffle(tempPlayers);
        this.board.setupInitialTurnOrder(tempPlayers);
        distributeInitialFood(tempPlayers);

        // 5. Set the first player before changing state
        this.setCurrentPlayer(tempPlayers.get(0));

        // 6. Transition to StartTurn state
        this.setState(new StartTurnState());
    }

    /**
     * Initially populates the tribe card rows (characters and events).
     * <ul>
     *   <li>Lower row: number of players + 1 characters</li>
     *   <li>Upper row: number of players + 4 characters and events</li>
     * </ul>
     */
    private void fillInitialTribeRows() {
        int numPlayers = players.size();

        int requiredCharactersLower = numPlayers + 1;
        while (board.getCharacterRowSize("lower") < requiredCharactersLower) {
            TribeCard card = tribeDeck.drawCard();
            if (card != null) {
                card.reveal(board, "lower");
            } else break;
        }

        int requiredTotalUpper = numPlayers + 4;
        while ((board.getCharacterRowSize("upper") + board.getEventRowSize("upper")) < requiredTotalUpper) {
            TribeCard card = tribeDeck.drawCard();
            if (card != null) {
                card.reveal(board, "upper");
            } else break;
        }
    }

    /**
     * Populates the upper building row by reading the configuration
     * from the {@code data/EraCounts.json} file based on the number of players and the current era.
     */
    private void fillInitialBuildingRow() {
        int numPlayers = players.size();
        int cardsToDraw = 0;

        try {
            JsonObject countsRoot = JsonParser.parseReader(new FileReader("data/EraCounts.json")).getAsJsonObject();
            JsonArray buildingCounts = countsRoot.getAsJsonObject("buildingCounts").getAsJsonArray(String.valueOf(numPlayers));

            if (buildingCounts != null) {
                cardsToDraw = buildingCounts.get(currentEra - 1).getAsInt();
            } else {
                throw new IllegalStateException("Node 'buildingCounts' missing or invalid.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read file EraCounts.json: " + e.getMessage(), e);
        }

        for (int i = 0; i < cardsToDraw; i++) {
            BuildingCard card = deckBuildings.drawCard();
            if (card != null) {
                board.addBuildingCard(card, "upper");
            }
        }
    }


    /**
     * Distributes initial food to players based on turn order.
     * <ul>
     *   <li>1st player: 2 food</li>
     *   <li>2nd-3rd player: 3 food</li>
     *   <li>4th-5th player: 4 food</li>
     * </ul>
     *
     * @param orderedPlayers list of players ordered according to the established turn order
     */
    private void distributeInitialFood(List<Player> orderedPlayers) {
        for (int i = 0; i < orderedPlayers.size(); i++) {
            Player player = orderedPlayers.get(i);
            int foodAmount;

            if (i == 0) {
                foodAmount = 2;
            } else if (i == 1 || i == 2) {
                foodAmount = 3;
            } else {
                foodAmount = 4;
            }

            player.modifyFood(foodAmount);
        }
    }

    /**
     * Transitions the game to a new state, notifying all observers.
     *
     * @param newState the new game state (e.g. {@link it.polimi.ingsw.am54.model.states.StartTurnState},
     *                 {@link it.polimi.ingsw.am54.model.states.PickingState}, etc.)
     */
    public void setState(GameState newState) {
        this.currentState = newState;
        notifyStateChanged(newState);
    }

    /**
     * Returns the current game state.
     *
     * @return the currently active state
     */
    public GameState getCurrentState() {
        return currentState;
    }

    /**
     * Advances to the next player on the turn track who has not yet
     * placed the totem. The search starts from the current player and cycles
     * through the occupied slots.
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

    /**
     * Increments the current era counter.
     */
    public void advanceEra(){this.currentEra++;}

    /**
     * Increments the round counter.
     */
    public void advanceRound(){this.roundCount++;}


    // ==========================================
    // END OF ROUND REFILL METHODS
    // ==========================================

    /**
     * Refills the upper tribe card row until reaching
     * the target of {@code players + 4} cards (characters + events).
     * Cards are drawn from the tribe deck and revealed in the upper row.
     */
    public void refillUpperTribeRow() {
        int targetSize = players.size() + 4;

        while ((board.getCharacterRowSize("upper") + board.getEventRowSize("upper")) < targetSize) {
            TribeCard card = tribeDeck.drawCard();
            if (card == null) break;
            card.reveal(board, "upper");
        }
    }

    /**
     * Refills the upper building row by reading the correct quantity
     * from the {@code data/EraCounts.json} file based on the era and number of players.
     * Cards are drawn from the building deck.
     */
    public void refillUpperBuildings() {
        int cardsToDraw = 0;
        try {
            JsonObject countsRoot = JsonParser.parseReader(new FileReader("data/EraCounts.json")).getAsJsonObject();
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

    /**
     * Searches for a player in the list by nickname (case-insensitive search).
     *
     * @param nickname the name of the player to find
     * @return the found player
     * @throws GameException if no player with that nickname is present in the game
     */
    public Player getPlayerByNickname(String nickname) throws GameException {
        return this.players.stream()
                .filter(p -> p.getNickname().equalsIgnoreCase(nickname))
                .findFirst()
                .orElseThrow(() -> new GameException("Error: Player '" + nickname + "' not found."));
    }

    /**
     * Notifies all observers that a new player has joined.
     *
     * @param player the player who joined
     */
    private void notifyPlayerJoined(Player player) {
        for (IGameObserver obs : getSafeObservers()) obs.onPlayerJoined(this, player);
    }

    /**
     * Notifies all observers that a player has chosen a totem color.
     *
     * @param player the player who chose
     * @param color  the chosen color
     */
    private void notifyTotemColorChosen(Player player, TotemColors color) {
        for (IGameObserver obs : getSafeObservers()) obs.onTotemColorChosen(this, player, color);
    }

    /**
     * Notifies all observers that a player has placed the totem
     * on an offering tile.
     *
     * @param player         the player who placed
     * @param offerPosition  the tile position
     */
    public void notifyTotemPlaced(Player player, int offerPosition) {
        for (IGameObserver obs : getSafeObservers()) obs.onTotemPlaced(this, player, offerPosition);
    }

    /**
     * Notifies all observers that a player has picked a card.
     *
     * @param player          the player who picked
     * @param row             the row from which the card was picked
     * @param cardIndex       the card index
     * @param newFoodBalance  the player's new food balance
     * @param newMagicBalance the new magic points balance
     */
    public void notifyCardPicked(Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {
        for (IGameObserver obs : getSafeObservers()) obs.onCardPicked(this, player, row, cardIndex, newFoodBalance, newMagicBalance);
    }

    /**
     * Notifies all observers that the turn has passed to a new player.
     *
     * @param player the player now taking their turn
     */
    private void notifyTurnChanged(Player player) {
        for (IGameObserver obs : getSafeObservers()) obs.onTurnChanged(this, player);
    }

    /**
     * Notifies all observers that the game state has changed.
     *
     * @param newState the new state
     */
    private void notifyStateChanged(GameState newState) {
        for (IGameObserver obs : getSafeObservers()) obs.onStateChanged(this, newState);
    }

    /**
     * Notifies all observers that a totem has been returned from the offering track,
     * including the player's final food and aura balances.
     *
     * @param player   the player returning the totem
     * @param position the position in the turn order
     */
    public void notifyTotemReturned(Player player, int position) {
        int finalFood = player.getFood();
        int finalAura = player.getAuraPoints();

        for (IGameObserver obs : getSafeObservers()) {
            obs.onTotemReturned(this, player, position, finalFood, finalAura);
        }
    }

    /**
     * Notifies all observers that an event card has been resolved.
     *
     * @param eventCard the resolved event card
     */
    public void notifyEventResolved(EventCard eventCard) {
        for (IGameObserver obs : getSafeObservers()) {
            obs.onEventResolved(this, eventCard);
        }
    }

    /**
     * Ends the turn of the specified player.
     * End-of-turn logic is delegated to the current state.
     *
     * @param nickname the name of the player ending their turn
     * @throws GameException if it is not the player's turn or the action is not allowed
     */
    @Override
    public void endTurn(String nickname) throws GameException {
        currentState.endTurn(this, nickname);
    }

    /**
     * Permanently removes a player from the game (e.g. for permanent disconnection).
     * If all players leave and the current state allows it,
     * the game is reset to the initial setup state.
     *
     * @param nickname the name of the player to remove
     */
    public synchronized void removePlayerEntirely(String nickname) {
        players.removeIf(p -> p.getNickname().equalsIgnoreCase(nickname));

        if (players.isEmpty() && currentState.shouldResetToSetupOnEmpty()) {
            this.setState(new SetUpGameState());
        }
    }


    // ==========================================
    // GETTERS & SETTERS ESSENZIALI
    // ==========================================

    /**
     * Returns the complete list of players in the game.
     *
     * @return list of players
     */
    public List<Player> getPlayers() { return players; }

    /**
     * Returns the total number of registered players.
     *
     * @return number of players
     */
    public int getPlayersNumber() { return players.size(); }

    /**
     * Returns the game Board.
     *
     * @return the current Board
     */
    public Board getBoard() { return this.board; }

    /**
     * Returns the tribe card deck.
     *
     * @return the tribe deck
     */
    public Deck<TribeCard> getTribeDeck() { return tribeDeck; }

    /**
     * Returns the building card deck.
     *
     * @return the building deck
     */
    public Deck<BuildingCard> getBuildingDeck() { return deckBuildings; }

    /**
     * Sets the tribe card deck (used for testing or restoration).
     *
     * @param deck the new tribe deck
     */
    public void setTribeDeck(Deck<TribeCard> deck) { this.tribeDeck = deck; }

    /**
     * Sets the building card deck (used for testing or restoration).
     *
     * @param deck the new building deck
     */
    public void setBuildingDeck(Deck<BuildingCard> deck) { this.deckBuildings = deck; }

    /**
     * Returns the current era.
     *
     * @return the era (1-3)
     */
    public int getCurrentEra() { return currentEra; }

    /**
     * Returns the current round number.
     *
     * @return the round (1-10)
     */
    public int getRoundCount() { return roundCount; }

    /**
     * Sets the current era.
     * Used by the Server to restore the game state after a crash.
     *
     * @param era the era to set (1-3)
     */
    public void setCurrentEra(int era) { this.currentEra = era; }

    /**
     * Sets the current round number.
     * Used by the Server to restore the game state after a crash.
     *
     * @param round the round number to set
     */
    public void setRoundCount(int round) { this.roundCount = round; }

    /**
     * Returns the player currently taking their turn.
     *
     * @return the current player
     */
    public Player getCurrentPlayer() { return currentPlayer; }

    /**
     * Sets the current player and notifies the turn change to all observers.
     *
     * @param player the player who becomes current
     */
    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        notifyTurnChanged(player);
    }

    /**
     * Resolves the "Gson Clones" problem.
     * When deserializing JSON, Gson creates distinct copies for objects referenced multiple times.
     * This method relinks the pointers (currentPlayer, board slots)
     * to the ONLY official instances contained in the 'players' list.
     */
    public void restoreReferences() {
        if (this.currentPlayer != null) {
            try {
                this.currentPlayer = getPlayerByNickname(this.currentPlayer.getNickname());
            } catch (Exception e) {
                // Ignored: if the player no longer exists, currentPlayer keeps the old reference
            }
        }

        if (this.board != null && this.board.getTurnOrderTile() != null) {
            for (TurnOrderSlot slot : this.board.getTurnOrderTile()) {
                if (slot.getOccupiedBy() != null) {
                    try {
                        slot.occupy(getPlayerByNickname(slot.getOccupiedBy().getNickname()));
                    } catch (Exception e) {
                        // Ignored
                    }
                }
            }
        }

        if (this.board != null && this.board.getOfferingTrack() != null) {
            for (OfferingTile tile : this.board.getOfferingTrack()) {
                if (tile.getOccupiedBy() != null) {
                    try {
                        Player p = getPlayerByNickname(tile.getOccupiedBy().getNickname());
                        tile.occupy(p);
                        p.setCurrentTile(tile);
                    } catch (Exception e) {
                        // Ignored
                    }
                }
            }
        }
    }

    /**
     * Sets the number of active players for the current round.
     * Used by the Controller to inform the Model of how many players
     * are actually participating (excluding disconnected ones).
     *
     * @param count the number of active players
     */
    public void setActivePlayerCount(int count) { this.activePlayerCount = count; }

    /**
     * Returns the number of active players for the current round.
     * If not set (0), uses the total number of players.
     *
     * @return number of active players
     */
    public int getActivePlayerCount() {
        return (activePlayerCount > 0) ? activePlayerCount : players.size();
    }

    /**
     * Returns the player currently in the "picking" phase (picker),
     * i.e. the first player on the offering track who has not
     * yet been moved to the turn track.
     *
     * @return the player who still needs to pick, or {@code null} if everyone has picked
     */
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

    /**
     * Checks if a player is present in the turn track.
     *
     * @param p the player to search for
     * @return {@code true} if the player occupies a turn track slot
     */
    private boolean isInTurnOrder(Player p) {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (p.equals(slot.getOccupiedBy())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Assigns a player to the first free slot in the turn track.
     * Used when a player completes picking and returns to the turn order.
     *
     * @param p the player to assign
     * @return the assigned slot, or {@code null} if there are no free slots
     */
    public TurnOrderSlot assignPickerToTurnOrder(Player p) {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (slot.getOccupiedBy() == null) {
                slot.occupy(p);
                return slot;
            }
        }
        return null;
    }

    /**
     * Moves a player to the last slot of the turn track,
     * compacting the intermediate slots to the left.
     * Used to handle disconnected players or pickers
     * that need to be repositioned at the end of the order.
     *
     * @param p the player to move
     * @return the slot where the player was placed, or {@code null} if the last slot is occupied
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
                if (occupant != null) {
                    slots.get(i + 1).freeSlot();
                    slots.get(i).occupy(occupant);
                }
            }
        }

        int lastIdx = slots.size() - 1;
        TurnOrderSlot lastSlot = slots.get(lastIdx);
        if (lastSlot.getOccupiedBy() == null) {
            lastSlot.occupy(p);
            return lastSlot;
        }

        return null;
    }

    /**
     * Checks if all players who placed their totem on the offering
     * track have completed the picking phase.
     * The condition is satisfied when every occupied tile has its
     * player moved to the turn track.
     *
     * @return {@code true} if all pickers have picked
     */
    public boolean isPickingComplete() {
        for (OfferingTile tile : board.getOfferingTrack()) {
            Player occupant = tile.getOccupiedBy();
            if (occupant != null && !isInTurnOrder(occupant)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the tribe card deck is empty.
     *
     * @return {@code true} if there are no more cards to draw
     */
    public boolean isTribeDeckEmpty() {
        return tribeDeck.isEmpty();
    }

    /**
     * Checks if the next card in the tribe deck belongs to an era
     * after the current one, indicating that an era advancement is needed.
     *
     * @return {@code true} if the next card is from a higher era
     */
    public boolean isEraTransition() {
        TribeCard nextCard = tribeDeck.peek();
        if (nextCard == null) return false;
        return nextCard.getEra() > currentEra;
    }

    /**
     * Calculates the inventor bonus for the specified player.
     * The bonus is the product of the number of inventors in the tribe
     * and the number of distinct levels among the owned inventors.
     *
     * @param p the player whose bonus to calculate
     * @return the inventor bonus score
     */
    public int getPlayerInventorsBonus(Player p) {
        long inventors = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR).count();
        long uniqueInventions = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR)
                .mapToInt(CharacterCard::getLevel)
                .distinct().count();
        return (int) (inventors * uniqueInventions);
    }

    /**
     * Calculates the artist bonus for the specified player.
     * Each pair of artists in the tribe is worth 10 points.
     *
     * @param p the player whose bonus to calculate
     * @return the artist bonus score (every 2 artists = 10 points)
     */
    public int getPlayerArtistsBonus(Player p) {
        long artists = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.ARTIST).count();
        return (int) (artists / 2) * 10;
    }

    /**
     * Calculates the constructor bonus for the specified player.
     * The bonus is equal to the sum of aura points of all constructors in the tribe.
     *
     * @param p the player whose bonus to calculate
     * @return the constructor bonus score
     */
    public int getPlayerConstructorsBonus(Player p) {
        return p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.CONSTRUCTOR)
                .mapToInt(CharacterCard::getAura).sum();
    }

    /**
     * Calculates the player's base building score,
     * summing the {@code finalAura} value of each owned building.
     *
     * @param p the player whose score to calculate
     * @return the sum of the buildings' base aura points
     */
    public int getPlayerBuildingsBaseAura(Player p) {
        return p.getBuildings().stream()
                .mapToInt(BuildingCard::getFinalAura).sum();
    }

    /**
     * Calculates a player's total score by summing:
     * <ol>
     *   <li>Personal aura points (including end-game modifiers)</li>
     *   <li>Inventor bonus</li>
     *   <li>Artist bonus</li>
     *   <li>Constructor bonus</li>
     *   <li>Base points of building cards</li>
     * </ol>
     *
     * @param p the player whose score to calculate
     * @return the calculated total score
     */
    public int calculatePlayerScore(Player p) {
        int score = 0;
        score += p.getAuraPoints();
        score += getPlayerInventorsBonus(p);
        score += getPlayerArtistsBonus(p);
        score += getPlayerConstructorsBonus(p);
        score += getPlayerBuildingsBaseAura(p);

        return score;
    }

    /**
     * Determines the list of winners of the game.
     * The winner is the player with the highest score.
     * In case of a tie, the tiebreaker is based on remaining food.
     * <p>
     * <b>Note:</b> This method assumes that {@code EndGameState} has already
     * applied all end-game effects before being called.
     * </p>
     *
     * @return list of winners (may contain multiple players in case of unresolved tie)
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
            winners = winners.stream()
                    .filter(p -> p.getFood() == finalMaxFood)
                    .toList();
        }

        return winners;
    }

    /**
     * Notifies all observers of the end of the game and the winners.
     *
     * @param winners the list of winning players
     */
    public void notifyEndGame(List<Player> winners) {
        for (IGameObserver obs : getSafeObservers()) {
            obs.onEndGame(this, winners);
        }
    }

    /**
     * Returns the list of observers safely,
     * initializing it if necessary (prevents NullPointerException after Gson deserialization).
     *
     * @return the thread-safe list of observers
     */
    private List<IGameObserver> getSafeObservers() {
        if (observers == null) {
            observers = new CopyOnWriteArrayList<>();
        }
        return observers;
    }
}
