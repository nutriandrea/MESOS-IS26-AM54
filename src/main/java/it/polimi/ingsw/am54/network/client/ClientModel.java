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
 * Maintain a local game state synchronized with the server.
 * Receives Updates from ServerHandler (TCP) or RMIServerConnection (RMI)
 * and updates its state accordingly.
 *
 * WHY IT EXISTS:
 * The server-side Model (Game.java) is heavy: it contains game logic,
 * cards, effects, etc. The client doesn't need all of this.
 * ClientModel contains ONLY the data necessary to display the game
 * state: player names, cards on the board, colors, turns, etc.
 *
 * SYNCHRONIZATION:
 * All update methods are synchronized to ensure thread-safety.
 * ServerHandler runs on a separate thread and updates ClientModel
 * concurrently with the UI thread.
 *
 * STRUCTURE:
 * 1. GLOBAL STATE: round, era, current state, turn player
 * 2. BOARD: cards on table, turn order, offering track
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

    public void setOnEventResolvedCallback(java.util.function.Consumer<java.util.List<EventReport>> callback) {
        this.onEventResolvedCallback = callback;
    }

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

    public void setOnModelUpdatedCallback(Runnable callback) {
        this.onModelUpdatedCallback = callback;
    }

    private void notifyUI() {
        if (onModelUpdatedCallback != null) {
            // If there is already an update queued, we cancel it
            if (debounceTimer != null) {
                debounceTimer.cancel();
            }
            // We create a new one that will wait 100 milliseconds before redrawing
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

    /** Current state of the game (e.g. "Lobby", "SetUpGameState", "PickingState") */
    private String currentState = "Lobby";

    /** Nickname of the active turn player */
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
    /** List of all players in the game */
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
    /** List of winners (populated at the end of the game) */
    private List<String> winners = new ArrayList<>();

    /** Map nickname -> final score of the last match */
    private Map<String, Integer> lastMatchScores = Map.of();

    /** Number of players in the last match */
    private int lastMatchNumPlayers = 0;

    //--- 5. LEADERBOARD ---
    /** Player's leaderboard position for the specified number of players */
    private int leaderboardPosition = -1;

    /** Complete list of leaderboard entries */
    private List<LeaderboardEntry> leaderboardEntries = List.of();

    /** Number of players for which the leaderboard was requested */
    private int leaderboardNumPlayers = 0;

    /** Last received error message */
    private String lastErrorMessage;

    private boolean isReconnecting = false;
    private Runnable onReconnectingChangedCallback;
    private Runnable onConnectionLostCallback;

    /**
     * Set of nicknames of temporarily disconnected players.
     * Populated when PlayerDisconnectedUpdate is received.
     * Emptied when PlayerReconnectedUpdate or PlayerJoinedUpdate is received.
     *
     * The UI can use this set to show a visual indicator
     * (e.g., grey icon) next to the disconnected player's name.
     */
    private final Set<String> disconnectedPlayers = new HashSet<>();

    // =====================================================================
    // UPDATE METHODS (called by ServerHandler / RMIServerConnection)
    // All synchronized for thread-safety
    // =====================================================================

    /**
     * Updates the model when a player joins: adds them to the list and turn order,
     * removes them from disconnected (useful in case of reconnection) and sets a
     * welcome system message.
     *
     * @param nickname the joined player
     */
    public synchronized void updatePlayerJoined(String nickname) {
        if (!players.contains(nickname)) {
            players.add(nickname);
            turnOrderTrack.add(nickname);
        }
        disconnectedPlayers.remove(nickname);

        this.lastSystemMessage = "👋 NUOVO GIOCATORE: " + nickname.toUpperCase() + " è entrato nella lobby!";
        notifyUI();
    }

    /**
     * Marks a player as disconnected (remains visible with their state) and sets a
     * warning system message.
     *
     * @param nickname the disconnected player
     */
    public synchronized void updatePlayerDisconnected(String nickname) {
        disconnectedPlayers.add(nickname);
        // ADDITION: System message for drops
        this.lastSystemMessage = "⚠️ ATTENZIONE: " + nickname.toUpperCase() + " si è disconnesso!";
        notifyUI();
    }

    /**
     * Brings a player back to the "online" state by removing them from disconnected
     * and sets a re-entry system message.
     *
     * @param nickname the reconnected player
     */
    public synchronized void updatePlayerReconnected(String nickname) {
        disconnectedPlayers.remove(nickname);
        // ADDITION: System message for re-entry
        this.lastSystemMessage = "🟢 BENTORNATO: " + nickname.toUpperCase() + " è tornato in partita!";
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


    /**
     * Updates the currently active player.
     *
     * @param nickname the new active player (or {@code null} between phases)
     */
    public synchronized void updateTurn(String nickname) {
        this.currentPlayer = nickname;
        notifyUI();
    }

    /**
     * Reflects a totem placement: frees the player's position in the turn order
     * (marking it "VUOTO" so others don't shift) and places them on the indicated
     * offering tile.
     *
     * @param nickname the player who placed the totem
     * @param position the offering tile index
     */
    public synchronized void updateTotemPlacement(String nickname, int position) {
        for (int i = 0; i < turnOrderTrack.size(); i++) {
            String s = turnOrderTrack.get(i);
            String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;

            if (name.equalsIgnoreCase(nickname)) {
                if (s.contains(" -> ")) {
                    // Keeps "[Slot X | Food: +Y] -> " and adds "VUOTO"
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
     * Reflects picking a card: removes the card from the correct row (character or
     * building based on index), adds it to the player's tribe/buildings, updates
     * food and magic, and replaces the offering track with the one updated by the server.
     *
     * @param nickname        the player who picked
     * @param row             the row ("upper" or "lower")
     * @param index           the card index (characters followed by buildings)
     * @param newFoodBalance  the new food balance
     * @param newMagicBalance the new magic balance
     * @param updatedOffering the updated offering track
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
     * Reflects the return of a player's totem from the offering tile to the turn
     * order: frees their tile (marking it "VUOTO" without losing bonus/ID), reinserts
     * them in the first truly available slot (skipping "ghosts") and updates food and aura.
     *
     * @param nickname       the player whose totem returns
     * @param newFoodBalance the new food balance
     * @param newAuraBalance the new aura balance
     */
    public synchronized void updateTotemReturned(String nickname, int newFoodBalance, int newAuraBalance) {
        // 1. FIX: Instead of physically deleting the tile from the map (making it disappear in the CLI),
        // we look for it and replace the occupant's name with "VUOTO", keeping bonuses and ID intact!
        int targetKey = -1;
        String targetPrefix = "";

        for (Map.Entry<Integer, String> entry : offeringTrack.entrySet()) {
            String occupier = entry.getValue();
            String name = occupier.contains(" -> ") ? occupier.split(" -> ")[occupier.split(" -> ").length - 1].trim() : occupier;
            if (name.equalsIgnoreCase(nickname)) {
                targetKey = entry.getKey();
                targetPrefix = occupier.contains(" -> ") ? occupier.substring(0, occupier.lastIndexOf("->") + 2) : "";
                break; // Found the totem, we stop the loop
            }
        }

        // If we found it, we safely update the map
        if (targetKey != -1) {
            if (!targetPrefix.isEmpty()) {
                offeringTrack.put(targetKey, targetPrefix + " VUOTO");
            } else {
                offeringTrack.put(targetKey, "VUOTO");
            }
        }

        // 2. GRAPHIC/LOGIC FIX: We preemptively remove the player from their old
        // "ghost" position in the turn order to force recalculation.
        for (int i = 0; i < turnOrderTrack.size(); i++) {
            String s = turnOrderTrack.get(i);
            String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;
            if (name.equalsIgnoreCase(nickname)) {
                if (s.contains(" -> ")) {
                    String prefix = s.substring(0, s.lastIndexOf("->") + 2);
                    turnOrderTrack.set(i, prefix + " VUOTO");
                } else {
                    turnOrderTrack.set(i, "VUOTO");
                }
            }
        }

        // 3. We insert the player in the FIRST truly available slot starting from the top.
        // A slot is available if it is VUOTO, NULL, or occupied by a "Ghost".
        boolean placed = false;
        for (int i = 0; i < turnOrderTrack.size(); i++) {
            String s = turnOrderTrack.get(i);
            String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;

            // We determine if the current occupant of the slot is actually a ghost
            boolean isGhost = disconnectedPlayers.contains(name);
            if (!isGhost && !name.equalsIgnoreCase("VUOTO") && !name.equalsIgnoreCase("NULL")) {
                for (String occupier : offeringTrack.values()) {
                    String offName = occupier.contains(" -> ") ? occupier.split(" -> ")[occupier.split(" -> ").length - 1].trim() : occupier;
                    if (offName.equalsIgnoreCase(name)) {
                        isGhost = true;
                        break;
                    }
                }
            }

            // If the slot is free or a ghost, we overwrite it with the active player!
            if (name.equalsIgnoreCase("VUOTO") || name.equalsIgnoreCase("NULL") || isGhost) {
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

        // Safety fallback
        if (!placed) {
            turnOrderTrack.add(nickname);
        }

        playerFood.put(nickname, newFoodBalance);
        playerAura.put(nickname, newAuraBalance);
        notifyUI();
    }

    /**
     * Applies the snapshot of a resolved event: for each player calculates the
     * resource delta (for the on-screen summary) and updates food, aura, magic,
     * tribe and buildings, then rebuilds the board rows and notifies the UI.
     *
     * @param msg the event update received from the server
     */
    public synchronized void updateEventSnapshot(EventUpdate msg) {
        EventReport report = new EventReport();
        report.eventName = msg.getEventName() != null ? msg.getEventName() : "Risoluzione Eventi";

        // Updates Resources of ALL Players simultaneously (YOUR LOGIC INTACT)
        msg.getPlayersStats().forEach((nick, stats) -> {

            // <-- CAPTURE DATA BEFORE OVERWRITING -->
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

        // Wipes and Rebuilds the Board in one go (YOUR LOGIC INTACT)
        characterUpperRow.clear(); characterUpperRow.addAll(msg.getCharUpper());
        characterLowerRow.clear(); characterLowerRow.addAll(msg.getCharLower());
        eventUpperRow.clear();     eventUpperRow.addAll(msg.getEventUpper());
        eventLowerRow.clear();     eventLowerRow.addAll(msg.getEventLower());
        upperBuildingRow.clear();  upperBuildingRow.addAll(msg.getBuildUpper());
        lowerBuildingRow.clear();  lowerBuildingRow.addAll(msg.getBuildLower());
        notifyUI();
    }

    /**
     * Applies the COMPLETE state snapshot received at each phase change or upon
     * reconnection: clears all local structures (to avoid leaving "ghosts" of
     * disconnected players) and repopulates them with state, player statistics,
     * turn order, offering track and board rows.
     *
     * @param msg the state snapshot received from the server
     */
    public synchronized void updateState(StateChangedUpdate msg) {
        this.currentState = msg.getStateName();
        this.currentEra = msg.getCurrentEra();
        this.currentRound = msg.getCurrentRound();

        // 1. TOTAL CLEANUP: We must empty EVERYTHING before filling.
        // If we don't empty these maps, disconnected players' data remain as "ghosts".
        this.players.clear();
        this.playerColors.clear();
        this.playerFood.clear();
        this.playerAura.clear();
        this.playerMagic.clear();
        this.playerTribe.clear();
        this.playerBuildings.clear();

        // 2. Reloads the clean data sent by the server
        this.players.addAll(msg.getPlayersStats().keySet());

        msg.getPlayersStats().forEach((nick, stats) -> {
            playerColors.put(nick, stats.color);
            playerFood.put(nick, stats.food);
            playerAura.put(nick, stats.aura);
            playerMagic.put(nick, stats.magicPoints);
            playerTribe.put(nick, new ArrayList<>(stats.tribe));
            playerBuildings.put(nick, new ArrayList<>(stats.buildings));
        });

        // 3. Synchronization of remaining game structures
        turnOrderTrack.clear();
        turnOrderTrack.addAll(msg.getTurnOrderTrack());

        offeringTrack.clear();
        offeringTrack.putAll(msg.getOfferingTrack());

        // 4. Synchronizes game board
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

    /**
     * Registers the final result (winners and scores). The data is assigned immediately
     * so it's never empty; if there are still event reports to show, opening the
     * endgame screen is postponed until they are consumed.
     *
     * @param winners    the winners
     * @param allScores  the final scores of all players
     * @param numPlayers the number of players in the match
     */
    public synchronized void updateEndGame(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
        // ABSOLUTE FIX: Immediate and unconditional assignment of results.
        // This way model.getWinners() will NEVER be empty when the CLI goes to print it.
        this.winners = winners;
        this.lastMatchScores = allScores;
        this.lastMatchNumPlayers = numPlayers;

        if (!pendingEventReports.isEmpty() && onEventResolvedCallback != null) {
            this.isEndGamePending = true;
            this.pendingWinners = winners;
            this.pendingLastMatchScores = allScores;
            this.pendingLastMatchNumPlayers = numPlayers;
        } else {
            notifyUI();
        }
    }

    // ==========================================
    // STATIC BONUS CALCULATION (FROM GAME.JAVA)
    // ==========================================

    /**
     * Recalculates client-side a player's Artists bonus (10 aura per pair),
     * reading the serialized cards of their tribe. Used to show the breakdown
     * of the final score.
     *
     * @param pName the player's nickname
     * @return the bonus aura from artists
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
     * Recalculates client-side the Inventors bonus (number of inventors per distinct
     * inventions), reading the serialized cards of the tribe.
     *
     * @param pName the player's nickname
     * @return the bonus aura from inventors
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
     * Recalculates client-side the Constructors aura (sum of their base aura),
     * reading the serialized cards of the tribe.
     *
     * @param pName the player's nickname
     * @return the bonus aura from constructors
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
     * Recalculates client-side the Buildings base aura (sum of the final auras),
     * reading the player's serialized cards.
     *
     * @param pName the player's nickname
     * @return the base aura from buildings
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
     * Stores the leaderboard received from the server (player position and complete
     * list) for display.
     *
     * @param playerPosition the player's position in the leaderboard
     * @param entries        the leaderboard entries
     * @param numPlayers     the player number filter used
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
        this.lastSystemMessage = "⏸️ GIOCO SOSPESO: in attesa di riconnessioni...";
        notifyUI();
    }

    /** Updated when the game is resumed */
    public synchronized void updateGameResumed() {
        this.lastSystemMessage = "▶️ GIOCO RIPRESO: almeno 2 giocatori attivi!";
        notifyUI();
    }

    public synchronized void clearError() {
        this.lastErrorMessage = null;
    }

    public synchronized void clearSystemMessage() { this.lastSystemMessage = null; }

    public boolean isReconnecting() { return isReconnecting; }

    public void setReconnectingStatus(boolean status) {
        this.isReconnecting = status;
        if (onReconnectingChangedCallback != null) {
            onReconnectingChangedCallback.run();
        }
    }

    public void setOnReconnectingChangedCallback(Runnable callback) {
        this.onReconnectingChangedCallback = callback;
    }

    public void setOnConnectionLostCallback(Runnable callback) {
        this.onConnectionLostCallback = callback;
    }

    /**
     * Signals connection loss with the server: sets a system message and invokes
     * any callback (e.g., to trigger UI-side auto-reconnection).
     */
    public void triggerConnectionLost() {
        // ADDITION: Forces a system message for CLI (and GUI)
        this.lastSystemMessage = "❌ ATTENZIONE: CONNESSIONE CON IL SERVER PERSA! Tentativo di riconnessione in corso...";
        notifyUI();

        if (onConnectionLostCallback != null) {
            onConnectionLostCallback.run();
        }
    }

    public synchronized void triggerSystemMessage(String msg) {
        this.lastSystemMessage = msg;
        notifyUI();
    }

    // --- GETTER ---
    public synchronized String getLastSystemMessage() { return lastSystemMessage; }
    public synchronized String getCurrentState() { return currentState; }
    public synchronized Map<String, TotemColors> getPlayerColors() { return playerColors; }
    public synchronized List<String> getWinners() { return winners; }
    public synchronized Map<String, Integer> getLastMatchScores() { return lastMatchScores; }
    public synchronized int getLastMatchNumPlayers() { return lastMatchNumPlayers; }
    public synchronized int getLeaderboardPosition() { return leaderboardPosition; }
    public synchronized List<LeaderboardEntry> getLeaderboardEntries() { return leaderboardEntries; }
    public synchronized int getLeaderboardNumPlayers() { return leaderboardNumPlayers; }
    public synchronized String getLastErrorMessage() { return lastErrorMessage; }
    public synchronized Map<Integer, String> getOfferingTrack() { return offeringTrack; }
    public synchronized List<String> getTurnOrderTrack() { return turnOrderTrack; }
    public synchronized List<String> getCharacterUpperRow() { return characterUpperRow; }
    public synchronized List<String> getCharacterLowerRow() { return characterLowerRow; }
    public synchronized List<String> getEventUpperRow() { return eventUpperRow; }
    public synchronized List<String> getEventLowerRow() { return eventLowerRow; }
    public synchronized List<String> getUpperBuildingRow() { return upperBuildingRow; }
    public synchronized List<String> getLowerBuildingRow() { return lowerBuildingRow; }
    public synchronized String getCurrentPlayer() { return currentPlayer; }
    public synchronized List<String> getPlayers() { return players; }
    public synchronized int getCurrentEra() { return currentEra; }
    public synchronized Map<String, Integer> getPlayerFood() { return playerFood; }
    public synchronized Map<String, Integer> getPlayerAura() { return playerAura; }
    public synchronized Map<String, Integer> getPlayerMagic() { return playerMagic; }
    public synchronized Map<String, List<String>> getPlayerTribe() { return playerTribe; }
    public synchronized Map<String, List<String>> getPlayerBuildings() { return playerBuildings; }
    public synchronized int getRoundCount() { return currentRound; }
}