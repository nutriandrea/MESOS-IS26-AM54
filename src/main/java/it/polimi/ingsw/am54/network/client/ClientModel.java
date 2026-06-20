package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ============================================================================
 * ClientModel — Lightweight client-side model
 * ============================================================================
 *
 * ROLE:
 * Maintains a local game state synchronized with the server.
 * Receives Updates from ServerHandler (TCP) or RMIServerConnection (RMI)
 * and updates its own state accordingly.
 *
 * WHY IT EXISTS:
 * The server-side Model (Game.java) is heavy: contains game logic,
 * cards, effects, etc. The client does not need all of this.
 * ClientModel contains ONLY the data needed to display the game state
 * player names, cards on the table, colors, turns, etc.
 *
 * SYNCHRONIZATION:
 * All update methods are synchronized to ensure thread-safety.
 * ServerHandler runs on a separate thread and updates ClientModel
 * concurrently with respect to the UI thread.
 *
 * STRUCTURE:
 * 1. GLOBAL STATE: round, era, current state, current player
 * 2. BOARD: cards on the table, turn order, offering track
 * 3. PLAYERS: nickname, colors, resources (food, aura), card inventory
 * 4. EXTRA: winners, errors, disconnected players
 * ============================================================================
 */
public class ClientModel {

    //------------------------------------------------------------------------------
    //Attributes and methods to notify UI of changes
    private Runnable onModelUpdatedCallback;
    private Timer debounceTimer;

    private String lastSystemMessage;

    private final java.util.List<EventReport> pendingEventReports = new java.util.ArrayList<>();
    private java.util.function.Consumer<java.util.List<EventReport>> onEventResolvedCallback;

    private boolean isEndGamePending = false;
    private List<String> pendingWinners = null;
    private Map<String, Integer> pendingLastMatchScores = null;
    private int pendingLastMatchNumPlayers = 0;

    /**
     * Sets the callback invoked when event reports have been resolved.
     *
     * @param callback the callback to invoke
     */
    public void setOnEventResolvedCallback(java.util.function.Consumer<java.util.List<EventReport>> callback) {
        this.onEventResolvedCallback = callback;
    }

    /**
     * Clears all pending event reports and, if the end-game was waiting,
     * finalises the winners and scores.
     */
    public synchronized void clearPendingEvents() {
        this.pendingEventReports.clear();

        if (isEndGamePending) {
            this.winners = pendingWinners;
            this.lastMatchScores = pendingLastMatchScores;
            this.lastMatchNumPlayers = pendingLastMatchNumPlayers;
            this.isEndGamePending = false;
            notifyUI();
        }
    }

    /**
     * Sets the callback invoked whenever the model is updated (debounced).
     *
     * @param callback the callback to invoke
     */
    public void setOnModelUpdatedCallback(Runnable callback) {
        this.onModelUpdatedCallback = callback;
    }

    private void notifyUI() {
        if (onModelUpdatedCallback != null) {
            // If there's already an update queued, cancel it
            if (debounceTimer != null) {
                debounceTimer.cancel();
            }
            // Create a new one that will wait 100ms before redrawing
            debounceTimer = new Timer(true); // "true" means it will not block the game from closing
            debounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    onModelUpdatedCallback.run();
                }
            }, 100);
        }
    }
    //------------------------------------------------------------------------------

    // --- 1. GLOBAL STATE ---
    /** Current round (starts from 1 after setup) */
    private int currentRound = 1;

    /** Current era (starts from 1) */
    private int currentEra = 1;

    /** Current game state (e.g. "Lobby", "SetUpGameState", "PickingState") */
    private String currentState = "Lobby";

    /** Nickname of the player whose turn is active */
    private String currentPlayer;

    // --- 2. BOARD ---
    /** Turn order: list of nicknames in turn order */
    private final List<String> turnOrderTrack = new ArrayList<>();

    /** Offering tiles: map position -> nickname of the player who occupied it */
    private final Map<Integer, String> offeringTrack = new TreeMap<>();

    /** Character cards in the upper row */
    private final List<String> characterUpperRow = new ArrayList<>();

    /** Character cards in the lower row */
    private final List<String> characterLowerRow = new ArrayList<>();

    /** Event cards in the upper row */
    private final List<String> eventUpperRow = new ArrayList<>();

    /** Event cards in the lower row */
    private final List<String> eventLowerRow = new ArrayList<>();

    /** Building cards in the upper row */
    private final List<String> upperBuildingRow = new ArrayList<>();

    /** Building cards in the lower row */
    private final List<String> lowerBuildingRow = new ArrayList<>();

    // --- 3. PLAYER STATE (MAPS indexed by Nickname) ---
    /** List of all players in the match */
    private final List<String> players = new ArrayList<>();

    /** Map nickname -> totem color */
    private final Map<String, TotemColors> playerColors = new HashMap<>();

    /** Map nickname -> available food */
    private final Map<String, Integer> playerFood = new HashMap<>();

    /** Map nickname -> aura points */
    private final Map<String, Integer> playerAura = new HashMap<>();

    private final Map<String, Integer> playerMagic = new HashMap<>();

    /** Map nickname -> list of owned tribe cards (characters) */
    private final Map<String, List<String>> playerTribe = new HashMap<>();

    /** Map nickname -> list of owned building cards */
    private final Map<String, List<String>> playerBuildings = new HashMap<>();

    // --- 4. EXTRA ---
    /** List of winners (populated at the end of the match) */
    private List<String> winners = new ArrayList<>();

    /** Map nickname -> final score of the last match */
    private Map<String, Integer> lastMatchScores = Map.of();

    /** Number of players in the last match */
    private int lastMatchNumPlayers = 0;

    //--- 5. LEADERBOARD ---
    /** Player's position in the leaderboard for the specified number of players */
    private int leaderboardPosition = -1;

    /** Full list of leaderboard entries */
    private List<LeaderboardEntry> leaderboardEntries = List.of();

    /** Number of players for which the leaderboard was requested */
    private int leaderboardNumPlayers = 0;

    /** Last error message received */
    private String lastErrorMessage;

    private boolean isReconnecting = false;
    private Runnable onReconnectingChangedCallback;
    private Runnable onConnectionLostCallback;

    /**
     * Set of nicknames of temporarily disconnected players.
     * Populated when receiving PlayerDisconnectedUpdate.
     * Emptied when receiving PlayerReconnectedUpdate or PlayerJoinedUpdate.
     *
     * The UI can use this set to show a visual indicator
     * (e.g. gray icon) next to the disconnected player's name.
     */
    private final Set<String> disconnectedPlayers = new HashSet<>();

    // =====================================================================
    // UPDATE METHODS (called by ServerHandler / RMIServerConnection)
    // All synchronized for thread-safety
    // =====================================================================

    /**
     * Updated when a new player joins the game.
     *
     * WHAT IT DOES:
     * - Adds the player to the players list
     * - Adds the player to the turnOrderTrack
     * - Removes the player from the disconnected set (if present)
     *
     * NOTE: if a player reconnects, this method is called
     * for them too, so the disconnected set is automatically cleared.
     */
    public synchronized void updatePlayerJoined(String nickname) {
        if (!players.contains(nickname)) {
            players.add(nickname);
            turnOrderTrack.add(nickname);
        }
        disconnectedPlayers.remove(nickname);

        this.lastSystemMessage = "👋 NEW PLAYER: " + nickname.toUpperCase() + " has entered the lobby!";
        notifyUI();
    }

    /**
     * Updated when a player is marked as disconnected.
     *
     * WHAT IT DOES:
     * - Adds the nickname to the disconnectedPlayers set
     *
     * THE UI SHOULD:
     * - Show a visual indicator (e.g. "offline" icon)
     * - Disable interactions with the disconnected player
     * - Continue showing their state (cards, resources, position)
     */
    public synchronized void updatePlayerDisconnected(String nickname) {
        disconnectedPlayers.add(nickname);
        // ADDED: System message for disconnections
        this.lastSystemMessage = "⚠️ WARNING: " + nickname.toUpperCase() + " has disconnected!";
        notifyUI();
    }

    /**
     * Updated when a player reconnects.
     *
     * WHAT IT DOES:
     * - Removes the nickname from the disconnectedPlayers set
     *
     * THE UI SHOULD:
     * - Remove the disconnection indicator
     * - Re-enable interactions with the player
     */
    public synchronized void updatePlayerReconnected(String nickname) {
        disconnectedPlayers.remove(nickname);
        // ADDED: System message for reconnections
        this.lastSystemMessage = "🟢 WELCOME BACK: " + nickname.toUpperCase() + " has rejoined the game!";
        notifyUI();
    }

    /** Checks if a player is currently disconnected */
    public synchronized boolean isPlayerDisconnected(String nickname) {
        return disconnectedPlayers.contains(nickname);
    }

    /** Returns a copy of the disconnected players set */
    public synchronized Set<String> getDisconnectedPlayers() {
        return new HashSet<>(disconnectedPlayers);
    }


    /** Updated when the active player changes */
    public synchronized void updateTurn(String nickname) {
        this.currentPlayer = nickname;
        notifyUI();
    }

    /**
     * Updated when a player places the totem on an offering tile.
     *
     * WHAT IT DOES:
     * - Replaces the player in turnOrderTrack with "EMPTY" so others don't shift
     * - Adds the player to the offeringTrack at the specified position
     */
    public synchronized void updateTotemPlacement(String nickname, int position) {
        // DEFINITIVE ANTI-SHIFT FIX:
        // Instead of using removeIf (which shortens the list), look for the player
        // and replace them with "EMPTY", preserving the index and slot formatting.
        for (int i = 0; i < turnOrderTrack.size(); i++) {
            String s = turnOrderTrack.get(i);
            String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;

            if (name.equalsIgnoreCase(nickname)) {
                if (s.contains(" -> ")) {
                    // Keeps "[Slot X | Food: +Y] -> " and adds "EMPTY"
                    String prefix = s.substring(0, s.lastIndexOf("->") + 2);
                    turnOrderTrack.set(i, prefix + " VUOTO");
                } else {
                    turnOrderTrack.set(i, "VUOTO");
                }
                break;
            }
        }

        String oldTile = offeringTrack.get(position);
        if (oldTile != null) {
            offeringTrack.put(position, oldTile.replace("VUOTO", nickname));
        }
        notifyUI();
    }

    /**
     * Updated when a player draws a card.
     *
     * COMPLEX LOGIC:
     * 1. Determines if the card is in the upper or lower row
     * 2. Finds the correct index in the right row
     * 3. Removes the card from the board (sets to null)
     * 4. Adds the card to the player's tribe or buildings
     * 5. Updates the player's food (if they drew a building)
     * 6. Puts the player back in turnOrderTrack (totem becomes available)
     * 7. Removes the player from the offeringTrack
     *
     * @param nickname the player who drew
     * @param row "upper" or "lower"
     * @param index index of the card in the row
     * @param newFoodBalance new food balance of the player
     * @param newMagicBalance new magic balance of the player
     * @param updatedOffering the full offering track snapshot after the pick
     */

    public synchronized void updateCardPicked(String nickname, String row, int index, int newFoodBalance, int newMagicBalance, java.util.Map<Integer, String> updatedOffering) {
        boolean isUpper = row.equalsIgnoreCase("upper");
        if (isUpper) {
            if (index < characterUpperRow.size()) {
                String temp = characterUpperRow.get(index);
                characterUpperRow.remove(index);
                playerTribe.computeIfAbsent(nickname, k -> new java.util.ArrayList<>()).add(temp);
            } else {
                int buildingIndex = index - characterUpperRow.size();
                if (buildingIndex < upperBuildingRow.size()) {
                    String temp = upperBuildingRow.get(buildingIndex);
                    upperBuildingRow.remove(buildingIndex);
                    playerBuildings.computeIfAbsent(nickname, k -> new java.util.ArrayList<>()).add(temp);
                }
            }
        } else {
            if (index < characterLowerRow.size()) {
                String temp = characterLowerRow.get(index);
                characterLowerRow.remove(index);
                playerTribe.computeIfAbsent(nickname, k -> new java.util.ArrayList<>()).add(temp);
            } else {
                int buildingIndex = index - characterLowerRow.size();
                if (buildingIndex < lowerBuildingRow.size()) {
                    String temp = lowerBuildingRow.get(buildingIndex);
                    lowerBuildingRow.remove(buildingIndex);
                    playerBuildings.computeIfAbsent(nickname, k -> new java.util.ArrayList<>()).add(temp);
                }
            }
        }

        playerFood.put(nickname, newFoodBalance);
        playerMagic.put(nickname, newMagicBalance); // Instant update of the Magic counter!

        this.offeringTrack.clear();
        this.offeringTrack.putAll(updatedOffering);
        notifyUI();
    }

    /**
     * Updated when a player's totem is returned from the offering track back
     * to the turn order. Removes the totem from the offering track and
     * re-inserts the player into the first empty turn-order slot.
     *
     * @param nickname        the returning player's nickname
     * @param position        the offering tile position being vacated
     * @param newFoodBalance  the player's updated food balance
     * @param newAuraBalance  the player's updated aura balance
     */
    public synchronized void updateTotemReturned(String nickname, int position, int newFoodBalance, int newAuraBalance) {
        // Removes the totem from offerings comparing by actual name
        offeringTrack.entrySet().removeIf(entry -> {
            String occupier = entry.getValue();
            String name = occupier.contains(" -> ") ? occupier.split(" -> ")[occupier.split(" -> ").length - 1].trim() : occupier;
            return name.equalsIgnoreCase(nickname);
        });

        // Checks exact equality before putting it back in the turn order
        boolean alreadyInTurnOrder = turnOrderTrack.stream().anyMatch(s -> {
            String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;
            return name.equalsIgnoreCase(nickname);
        });

        if (!alreadyInTurnOrder) {
            boolean placed = false;
            // FIX RETURN: Find the first EMPTY slot and fill it with the name,
            // keeping the list at the correct length and the layout fixed!
            for (int i = 0; i < turnOrderTrack.size(); i++) {
                String s = turnOrderTrack.get(i);
                String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;

                if (name.equalsIgnoreCase("VUOTO") || name.equalsIgnoreCase("NULL")) {
                    if (s.contains(" -> ")) {
                        String prefix = s.substring(0, s.lastIndexOf("->") + 2);
                        turnOrderTrack.set(i, prefix + " " + nickname);
                    } else {
                        turnOrderTrack.set(i, nickname);
                    }
                    placed = true;
                    break;
                }
            }
            // Safety fallback in the extremely rare case there are no empty slots
            if (!placed) {
                turnOrderTrack.add(nickname);
            }
        }

        playerFood.put(nickname, newFoodBalance);
        playerAura.put(nickname, newAuraBalance);
        notifyUI();
    }

    /**
     * Processes an event-resolution snapshot, computing per-player resource
     * deltas and rebuilding the board in one shot.
     *
     * @param msg the event update message containing the full snapshot
     */
    public synchronized void updateEventSnapshot(EventUpdate msg) {
        EventReport report = new EventReport();
        report.eventName = msg.getEventName() != null ? msg.getEventName() : "Risoluzione Eventi";

        // Updates Resources of ALL Players simultaneously (YOUR LOGIC INTACT)
        msg.getPlayersStats().forEach((nick, stats) -> {

            // <-- CAPTURE DATA BEFORE OVERWRITE -->
            PlayerDelta pd = new PlayerDelta();
            pd.nickname = nick;
            pd.oldFood = playerFood.getOrDefault(nick, 0);
            pd.oldAura = playerAura.getOrDefault(nick, 0);
            pd.oldMagic = playerMagic.getOrDefault(nick, 0);
            pd.diffFood = stats.food - pd.oldFood;
            pd.diffAura = stats.aura - pd.oldAura;
            pd.diffMagic = stats.magicPoints - pd.oldMagic;
            report.playerDeltas.add(pd);
            // <--------------------------------------------->

            playerFood.put(nick, stats.food);
            playerAura.put(nick, stats.aura);
            playerMagic.put(nick, stats.magicPoints);
            playerTribe.put(nick, new java.util.ArrayList<>(stats.tribe));
            playerBuildings.put(nick, new java.util.ArrayList<>(stats.buildings));
        });

        // Stores the event and notifies the GUI
        pendingEventReports.add(report);
        if (onEventResolvedCallback != null) {
            onEventResolvedCallback.accept(new java.util.ArrayList<>(pendingEventReports));
        }

        // Scrap and Rebuild the Board in one shot (YOUR LOGIC INTACT)
        characterUpperRow.clear(); characterUpperRow.addAll(msg.getCharUpper());
        characterLowerRow.clear(); characterLowerRow.addAll(msg.getCharLower());
        eventUpperRow.clear();     eventUpperRow.addAll(msg.getEventUpper());
        eventLowerRow.clear();     eventLowerRow.addAll(msg.getEventLower());
        upperBuildingRow.clear();  upperBuildingRow.addAll(msg.getBuildUpper());
        lowerBuildingRow.clear();  lowerBuildingRow.addAll(msg.getBuildLower());
        notifyUI();
    }

    /**
     * Replaces the entire client-side game state from a full state snapshot.
     * Clears all existing data before reloading the new state.
     *
     * @param msg the state-changed update containing the complete state
     */
    public synchronized void updateState(StateChangedUpdate msg) {
        this.currentState = msg.getStateName();
        this.currentEra = msg.getCurrentEra();
        this.currentRound = msg.getCurrentRound();

        // 1. TOTAL CLEANUP: We must empty EVERYTHING before refilling.
        // If we don't clear these maps, disconnected players' data remain as "ghosts".
        this.players.clear();
        this.playerColors.clear();
        this.playerFood.clear();
        this.playerAura.clear();
        this.playerMagic.clear();
        this.playerTribe.clear();
        this.playerBuildings.clear();

        // 2. Reload clean data sent by the server
        this.players.addAll(msg.getPlayersStats().keySet());

        msg.getPlayersStats().forEach((nick, stats) -> {
            playerColors.put(nick, stats.color);
            playerFood.put(nick, stats.food);
            playerAura.put(nick, stats.aura);
            playerMagic.put(nick, stats.magicPoints);
            playerTribe.put(nick, new ArrayList<>(stats.tribe));
            playerBuildings.put(nick, new ArrayList<>(stats.buildings));
        });

        // 3. Sync remaining game structures
        turnOrderTrack.clear();
        turnOrderTrack.addAll(msg.getTurnOrderTrack());

        offeringTrack.clear();
        offeringTrack.putAll(msg.getOfferingTrack());

        // 4. Synchronize game board
        characterUpperRow.clear(); characterUpperRow.addAll(msg.getCharUpper());
        characterLowerRow.clear(); characterLowerRow.addAll(msg.getCharLower());
        eventUpperRow.clear();     eventUpperRow.addAll(msg.getEventUpper());
        eventLowerRow.clear();     eventLowerRow.addAll(msg.getEventLower());
        upperBuildingRow.clear();  upperBuildingRow.addAll(msg.getBuildUpper());
        lowerBuildingRow.clear();  lowerBuildingRow.addAll(msg.getBuildLower());
        notifyUI();
    }

    /** Updated when a player chooses the totem color */
    public synchronized void updateTotemColor(String nickname, TotemColors color) {
        playerColors.put(nickname, color);
        notifyUI();
    }

    /** Updated when the match ends */
    public synchronized void updateEndGame(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
        if (!pendingEventReports.isEmpty()) {
            this.isEndGamePending = true;
            this.pendingWinners = winners;
            this.pendingLastMatchScores = allScores;
            this.pendingLastMatchNumPlayers = numPlayers;
        } else {
            this.winners = winners;
            this.lastMatchScores = allScores;
            this.lastMatchNumPlayers = numPlayers;
            notifyUI();
        }
    }

    // ==========================================
    // STATIC BONUS CALCULATION (FROM GAME.JAVA)
    // ==========================================

    /**
     * Calculates the artist bonus for a player (10 points per pair of artists).
     *
     * @param pName the player's nickname
     * @return the computed artist bonus
     */
    public int getArtistBonus(String pName) {
        int artistCount = 0;
        List<String> tribe = this.playerTribe.getOrDefault(pName, new ArrayList<>());
        for (String card : tribe) {
            String[] parts = card.split("\\|");
            if (parts.length > 3 && parts[3].equalsIgnoreCase("ARTIST")) {
                artistCount++;
            }
        }
        return (artistCount / 2) * 10;
    }

    /**
     * Calculates the inventor bonus for a player (inventor count times unique invention levels).
     *
     * @param pName the player's nickname
     * @return the computed inventor bonus
     */
    public int getInventorBonus(String pName) {
        int inventorCount = 0;
        java.util.Set<Integer> uniqueInventions = new java.util.HashSet<>();
        List<String> tribe = this.playerTribe.getOrDefault(pName, new ArrayList<>());
        for (String card : tribe) {
            String[] parts = card.split("\\|");
            if (parts.length > 4 && parts[3].equalsIgnoreCase("INVENTOR")) {
                inventorCount++;
                try { uniqueInventions.add(Integer.parseInt(parts[4])); } catch (Exception ignored) {}
            }
        }
        return inventorCount * uniqueInventions.size();
    }

    /**
     * Calculates the constructor bonus for a player (sum of constructor aura values).
     *
     * @param pName the player's nickname
     * @return the computed constructor bonus
     */
    public int getConstructorBonus(String pName) {
        int constructorAura = 0;
        List<String> tribe = this.playerTribe.getOrDefault(pName, new ArrayList<>());
        for (String card : tribe) {
            String[] parts = card.split("\\|");
            if (parts.length > 5 && parts[3].equalsIgnoreCase("CONSTRUCTOR")) {
                try { constructorAura += Integer.parseInt(parts[5]); } catch (Exception ignored) {}
            }
        }
        return constructorAura;
    }

    /**
     * Calculates the base aura contributed by a player's buildings.
     *
     * @param pName the player's nickname
     * @return the sum of building base aura values
     */
    public int getBuildingBaseAura(String pName) {
        int buildingBaseAura = 0;
        List<String> buildings = this.playerBuildings.getOrDefault(pName, new ArrayList<>());
        for (String bld : buildings) {
            String[] parts = bld.split("\\|");
            if (parts.length > 5) {
                try { buildingBaseAura += Integer.parseInt(parts[5]); } catch (Exception ignored) {}
            }
        }
        return buildingBaseAura;
    }

    /**
     * Updated when a LeaderboardUpdate is received from the server.
     * Contains the player's position and the complete leaderboard.
     */
    public synchronized void updateLeaderboard(int playerPosition, List<LeaderboardEntry> entries, int numPlayers) {
        this.leaderboardPosition = playerPosition;
        this.leaderboardEntries = entries;
        this.leaderboardNumPlayers = numPlayers;
        notifyUI();
    }

    /** Updated when an error message is received */
    public synchronized void updateError(String message) {
        this.lastErrorMessage = message;
        notifyUI();
    }

    /** Updated when the game is suspended (only 1 active player) */
    public synchronized void updateGameSuspended() {
        this.lastSystemMessage = "⏸️ GAME SUSPENDED: waiting for reconnections...";
        notifyUI();
    }

    /** Updated when the game is resumed */
    public synchronized void updateGameResumed() {
        this.lastSystemMessage = "▶️ GAME RESUMED: at least 2 active players!";
        notifyUI();
    }

    /** Clears the last error message. */
    public synchronized void clearError() {
        this.lastErrorMessage = null;
    }

    /** Clears the last system message. */
    public synchronized void clearSystemMessage() { this.lastSystemMessage = null; }

    /** @return {@code true} if the client is currently attempting to reconnect */
    public boolean isReconnecting() { return isReconnecting; }

    /**
     * Sets the reconnecting status and notifies the registered callback.
     *
     * @param status the new reconnecting status
     */
    public void setReconnectingStatus(boolean status) {
        this.isReconnecting = status;
        if (onReconnectingChangedCallback != null) {
            onReconnectingChangedCallback.run();
        }
    }

    /**
     * Sets the callback invoked when the reconnecting status changes.
     *
     * @param callback the callback to invoke
     */
    public void setOnReconnectingChangedCallback(Runnable callback) {
        this.onReconnectingChangedCallback = callback;
    }

    /**
     * Sets the callback invoked when the connection is lost.
     *
     * @param callback the callback to invoke
     */
    public void setOnConnectionLostCallback(Runnable callback) {
        this.onConnectionLostCallback = callback;
    }

    /** Triggers the connection-lost callback if one is registered. */
    public void triggerConnectionLost() {
        if (onConnectionLostCallback != null) {
            onConnectionLostCallback.run();
        }
    }

    /* =====================================================================
     * COMPLETE STATE LOADING (for reconnection)
     * ===================================================================== */

    /**
     * Loads the complete game state from StateChangedUpdate.
     * Called when a player reconnects.
     * TODO: implement using StateChangedUpdate to rebuild the complete state.
     *
     * @param update the StateChangedUpdate received from the server
     */
    // TODO: implement loadFullState with StateChangedUpdate
    // public synchronized void loadFullState(StateChangedUpdate update) { ... }
    
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(ClientModel.class.getName());

    // --- GETTER ---
    /** @return the last system message, or {@code null} */
    public synchronized String getLastSystemMessage() { return lastSystemMessage; }
    /** @return the current game state name */
    public synchronized String getCurrentState() { return currentState; }
    /** @return a map of nickname to totem color */
    public synchronized Map<String, TotemColors> getPlayerColors() { return playerColors; }
    /** @return the list of winner nicknames */
    public synchronized List<String> getWinners() { return winners; }
    /** @return a map of nickname to final score for the last match */
    public synchronized Map<String, Integer> getLastMatchScores() { return lastMatchScores; }
    /** @return the number of players in the last match */
    public synchronized int getLastMatchNumPlayers() { return lastMatchNumPlayers; }
    /** @return the player's leaderboard position (1-based, -1 if unranked) */
    public synchronized int getLeaderboardPosition() { return leaderboardPosition; }
    /** @return the full list of leaderboard entries */
    public synchronized List<LeaderboardEntry> getLeaderboardEntries() { return leaderboardEntries; }
    /** @return the number-of-players filter for the current leaderboard */
    public synchronized int getLeaderboardNumPlayers() { return leaderboardNumPlayers; }
    /** @return the last error message, or {@code null} */
    public synchronized String getLastErrorMessage() { return lastErrorMessage; }
    /** @return a map of offering tile position to occupier description */
    public synchronized Map<Integer, String> getOfferingTrack() { return offeringTrack; }
    /** @return the list of turn-order track entries */
    public synchronized List<String> getTurnOrderTrack() { return turnOrderTrack; }
    /** @return the upper row character cards */
    public synchronized List<String> getCharacterUpperRow() { return characterUpperRow; }
    /** @return the lower row character cards */
    public synchronized List<String> getCharacterLowerRow() { return characterLowerRow; }
    /** @return the upper row event cards */
    public synchronized List<String> getEventUpperRow() { return eventUpperRow; }
    /** @return the lower row event cards */
    public synchronized List<String> getEventLowerRow() { return eventLowerRow; }
    /** @return the upper row building cards */
    public synchronized List<String> getUpperBuildingRow() { return upperBuildingRow; }
    /** @return the lower row building cards */
    public synchronized List<String> getLowerBuildingRow() { return lowerBuildingRow; }
    /** @return the nickname of the current player, or {@code null} */
    public synchronized String getCurrentPlayer() { return currentPlayer; }
    /** @return the list of all player nicknames */
    public synchronized List<String> getPlayers() { return players; }
    /** @return the current era */
    public synchronized int getCurrentEra() { return currentEra; }
    /** @return a map of nickname to food */
    public synchronized Map<String, Integer> getPlayerFood() { return playerFood; }
    /** @return a map of nickname to aura points */
    public synchronized Map<String, Integer> getPlayerAura() { return playerAura; }
    /** @return a map of nickname to magic points */
    public synchronized Map<String, Integer> getPlayerMagic() { return playerMagic; }
    /** @return a map of nickname to tribe card list */
    public synchronized Map<String, List<String>> getPlayerTribe() { return playerTribe; }
    /** @return a map of nickname to building card list */
    public synchronized Map<String, List<String>> getPlayerBuildings() { return playerBuildings; }
    /** @return the current round number */
    public synchronized int getRoundCount() { return currentRound; }
}
