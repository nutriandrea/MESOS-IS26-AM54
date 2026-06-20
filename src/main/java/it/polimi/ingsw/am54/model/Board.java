package it.polimi.ingsw.am54.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.util.*;

/**
 * Game board that manages the turn track, the offering track,
 * building cards and character cards. It handles initializing the layout
 * based on the number of players, managing the movement of cards between the
 * upper and lower rows, and coordinating totem placement.
 * @author AM54
 */
public class Board {

    private List<CharacterCard> characterUpperRow;
    private List<CharacterCard> characterLowerRow;
    private List<EventCard> eventUpperRow;
    private List<EventCard> eventLowerRow;
    private List<BuildingCard> buildingCardsUpperRow;
    private List<BuildingCard> buildingCardsLowerRow;

    private List<OfferingTile> offeringTrack;
    private List<TurnOrderSlot> turnOrderTile;

    /**
     * Constructs an empty board with all rows and tracks initialized to empty lists.
     */
    public Board() {
        this.characterUpperRow = new ArrayList<>();
        this.characterLowerRow = new ArrayList<>();
        this.eventUpperRow = new ArrayList<>();
        this.eventLowerRow = new ArrayList<>();
        this.buildingCardsUpperRow = new ArrayList<>();
        this.buildingCardsLowerRow = new ArrayList<>();
        this.offeringTrack = new ArrayList<>();
        this.turnOrderTile = new ArrayList<>();
    }

    /**
     * Initializes the board layout (offering tiles and turn order track)
     * based on the number of players.
     *
     * @param numPlayers the number of players (2-5)
     */
    public void initializeLayout(int numPlayers) {
        initializeOfferingTiles(numPlayers);
        initializeTurnOrderTile(numPlayers);
    }

    // Modified: no longer takes "Game", only "numPlayers"
    private void initializeOfferingTiles(int numPlayers) {
        offeringTrack.clear();

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Critical error: Invalid number of players (" + numPlayers + ")");
        }

        List<Character> neededTiles = new ArrayList<>();

        try {
            JsonObject rootNode = JsonParser.parseReader(new FileReader("data/OfferingTilesSetup.json")).getAsJsonObject();
            String playerKey = String.valueOf(numPlayers);
            JsonArray tileArray = rootNode.getAsJsonObject("requiredTiles").getAsJsonArray(playerKey);

            if (tileArray != null) {
                for (JsonElement elem : tileArray) {
                    neededTiles.add(elem.getAsString().charAt(0));
                }
            }

            JsonArray tilesConfig = rootNode.getAsJsonArray("tiles");

            if (tilesConfig != null) {
                for (JsonElement elem : tilesConfig) {
                    JsonObject node = elem.getAsJsonObject();
                    char bedId = node.get("id").getAsString().charAt(0);
                    if (neededTiles.contains(bedId)) {
                        int upper = node.get("cardsFromUpper").getAsInt();
                        int lower = node.get("cardsFromLower").getAsInt();
                        int food  = node.get("foodBonus").getAsInt();

                        OfferingTile tile = new OfferingTile(bedId, upper, lower, food);
                        offeringTrack.add(tile);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fatal error: Could not initialize Offering Tiles from JSON configuration.", e);
        }

        offeringTrack.sort(Comparator.comparing(OfferingTile::getId));
    }

    /**
     * Adds a building card to the specified row.
     *
     * @param card the building card to add
     * @param row  the target row ("upper" or "lower")
     */
    public void addBuildingCard(BuildingCard card, String row) {
        if (row.equalsIgnoreCase("upper")) {
            buildingCardsUpperRow.add(card);
        } else {
            buildingCardsLowerRow.add(card);
        }
    }

    /**
     * Returns the number of event cards in the specified row.
     *
     * @param row the row to check ("upper" or "lower")
     * @return the number of event cards in that row
     */
    public int getEventRowSize(String row) {
        return row.equalsIgnoreCase("upper") ? eventUpperRow.size() : eventLowerRow.size();
    }


    /**
     * Initializes the turn order track with slots read from the JSON configuration,
     * assigning position and food bonus to each slot.
     *
     * @param numPlayers the number of players (2-5)
     * @throws IllegalArgumentException if the number of players is not between 2 and 5
     */
    public void initializeTurnOrderTile(int numPlayers) {
        // Clear the list if there was already something
        this.turnOrderTile.clear();

        // safety checks
        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Critical error: Invalid number of players (" + numPlayers + ")");
        }

        try {

            //reads the file
            JsonObject rootNode = JsonParser.parseReader(new FileReader("data/TurnOrderConfig.json")).getAsJsonObject();
            JsonArray bonusArray = rootNode.getAsJsonObject("bonusFood").getAsJsonArray(String.valueOf(numPlayers));

            // if the array exists, create the slots
            if (bonusArray != null) {

                for (int i = 0; i < bonusArray.size(); i++) {
                    int position = i + 1; // the "human" position starts from 1
                    int bonus = bonusArray.get(i).getAsInt(); // The food to give (-1, 0, 1, 2...)

                    // create the slot passing it the already prepared data
                    TurnOrderSlot slot = new TurnOrderSlot(position, bonus);
                    this.turnOrderTile.add(slot);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fatal error during Turn Order initialization", e);
        }
    }


    /**
     * Adds a character card to the specified row.
     *
     * @param card the character card to add
     * @param row  the target row ("upper" or "lower")
     */
    public void addCharacterCard(CharacterCard card, String row) {
        if (row.equalsIgnoreCase("upper")) {
            characterUpperRow.add(card);
        } else {
            characterLowerRow.add(card);
        }
    }

    /**
     * Adds an event card to the upper event row.
     *
     * @param card the event card to add
     * @param row  the target row (currently always added to the upper row)
     */
    public void addEventCard(EventCard card, String row) {
        eventUpperRow.add(card);
    }

    /**
     * Assigns each player to a turn order slot based on their position in the list.
     *
     * @param randomPlayers the list of players in random turn order
     */
    public void setupInitialTurnOrder(List<Player> randomPlayers) {
        for (int i = 0; i < randomPlayers.size(); i++) {
            Player p = randomPlayers.get(i);
            TurnOrderSlot slot = this.turnOrderTile.get(i);

            // Place the player in the slot
            slot.occupy(p);

        }
    }

    /**
     * Moves all remaining character and event cards from the upper row to the lower row.
     */
    public void moveUpperToLower() {
        // slide down all remaining characters
        characterLowerRow.addAll(characterUpperRow);
        characterUpperRow.clear();

        // slide down all remaining events
        eventLowerRow.addAll(eventUpperRow);
        eventUpperRow.clear();
    }

    /**
     * Replaces the lower building row with the contents of the upper building row,
     * then clears the upper row.
     */
    public void moveBuildings() {
        buildingCardsLowerRow.clear();
        buildingCardsLowerRow.addAll(buildingCardsUpperRow);
        buildingCardsUpperRow.clear();
    }

    /**
     * Refills the upper tribe card row by drawing from the tribe deck
     * until the total number of characters and events reaches {@code players + 4}.
     *
     * @param game the game instance providing the deck and player count
     */
    public void refillUpperRow(Game game) {
        int numPlayers = game.getPlayersNumber();
        int targetSize = numPlayers + 4;

        Deck<TribeCard> tribeDeck = game.getTribeDeck();

        // The loop checks the sum of characters and events in the upper row
        // Until we reach numPlayers + 4, we continue drawing
        while ((characterUpperRow.size() + eventUpperRow.size()) < targetSize) {

            TribeCard card = tribeDeck.drawCard();

            // Safety: if the deck is empty, we stop
            if (card == null) { break; }

            // the card reveals itself and will automatically go to characterUpperRow or eventUpperRow.
            // In either case, the total sum will increase by 1
            card.reveal(this, "upper");
        }
    }

    /**
     * Returns a combined list of all character and event cards in the specified row.
     *
     * @param row the row to retrieve ("upper" or "lower")
     * @return a new list containing all cards from that row, or an empty list if the row is invalid
     */
    public List<Card> getRow(String row) {
        // Create a new empty list that will contain the combined cards
        List<Card> combinedRow = new ArrayList<>();

        if (row.equalsIgnoreCase("upper")) {
            // Merge Characters and Events from the upper row
            combinedRow.addAll(characterUpperRow);
            combinedRow.addAll(eventUpperRow);
        } else if (row.equalsIgnoreCase("lower")) {
            // Merge Characters and Events from the lower row
            combinedRow.addAll(characterLowerRow);
            combinedRow.addAll(eventLowerRow);
        }

        // Return the entire list
        return combinedRow;
    }


    /**
     * Places the player's totem on the specified offering tile.
     *
     * @param player the player placing the totem
     * @param tile   the offering tile to occupy
     */
    public void addTotemToPosition(Player player, OfferingTile tile) {
        tile.occupy(player);
    }

    /**
     * Removes and returns a character card from the specified row at the given index.
     *
     * @param row   the row to draw from ("upper" or "lower")
     * @param index the index of the card in the row
     * @return the removed character card, or {@code null} if the index is out of bounds
     */
    public CharacterCard takeCharacterCard(String row, int index) {
        List<CharacterCard> targetRow = row.equalsIgnoreCase("upper")
                ? characterUpperRow
                : characterLowerRow;

        if (index < 0 || index >= targetRow.size()) {
            return null;
        }

        return targetRow.remove(index);
    }

    /**
     * Removes and returns a building card from the specified row at the given index.
     *
     * @param row   the row to draw from ("upper" or "lower")
     * @param index the index of the card in the row
     * @return the removed building card, or {@code null} if the index is out of bounds
     */
    public BuildingCard takeBuildingCard(String row, int index) {
        List<BuildingCard> targetRow = row.equalsIgnoreCase("upper")
                ? buildingCardsUpperRow
                : buildingCardsLowerRow;

        if (index < 0 || index >= targetRow.size()) {
            return null;
        }

        return targetRow.remove(index);
    }



    /**
     * Returns the number of character cards in the specified row.
     *
     * @param row the row to check ("upper" or "lower")
     * @return the number of character cards in that row
     */
    public int getCharacterRowSize(String row) {
        return row.equalsIgnoreCase("upper")
                ? characterUpperRow.size()
                : characterLowerRow.size();
    }

    /**
     * Returns the number of building cards in the specified row.
     *
     * @param row the row to check ("upper" or "lower")
     * @return the number of building cards in that row
     */
    public int getBuildingRowSize(String row) {
        return row.equalsIgnoreCase("upper")
                ? buildingCardsUpperRow.size()
                : buildingCardsLowerRow.size();
    }


    /**
     * Removes and returns the next event card to resolve from the lower row.
     * Sustenance (FARMER) events are resolved last; among non-sustenance events
     * the one with the lowest era is resolved first.
     *
     * @return the next event card, or {@code null} if the lower row is empty
     */
    public EventCard popNextEvent() {
        if (eventLowerRow.isEmpty()) {
            return null; // No more events
        }

        // Find the event to resolve based on rules:
        // Sustenance last, then lowest Era first
        EventCard nextEvent = eventLowerRow.stream()
                .min((a, b) -> {
                    boolean aIsFarmer = a.isSustenanceEvent();
                    boolean bIsFarmer = b.isSustenanceEvent();

                    if (aIsFarmer != bIsFarmer) {
                        return aIsFarmer ? 1 : -1; // Farmer goes last
                    }
                    return Integer.compare(a.getEra(), b.getEra());
                })
                .orElse(null);

        if (nextEvent != null) {
            eventLowerRow.remove(nextEvent); // Physically removes the card from the table
        }

        return nextEvent;
    }

    /**
     * Removes and returns the next visible event card to resolve.
     * Searches the lower row first, then the upper row if the lower is empty.
     * Sustenance (FARMER) events are resolved last; among non-sustenance events
     * the one with the lowest era is resolved first.
     *
     * @return the next event card, or {@code null} if no events are available
     */
    public EventCard popNextVisibleEvent() {
        // First look in the lower row (standard rule)
        EventCard next = popNextEvent(); // The method created above for the lower row

        // If the lower row is empty, look in the upper row
        if (next == null && !eventUpperRow.isEmpty()) {
            next = eventUpperRow.stream()
                    .min((a, b) -> {
                        boolean aIsFarmer = a.getName() != null && a.getName().toUpperCase().contains("FARMER");
                        boolean bIsFarmer = b.getName() != null && b.getName().toUpperCase().contains("FARMER");

                        // First priority: Sustenance events (go last)
                        if (aIsFarmer != bIsFarmer) {
                            return aIsFarmer ? 1 : -1;
                        }
                        // Second priority: Ascending Era order
                        return Integer.compare(a.getEra(), b.getEra());
                    })
                    .orElse(null);

            // If we found an event, physically remove it from the table
            if (next != null) {
                eventUpperRow.remove(next);
            }
        }

        return next;
    }

    /**
     * Checks if all active players have placed their totem.
     * @param expectedCount number of players that must place (active, not total)
     */
    public boolean allTotemsPlaced(int expectedCount) {
        int placedCount = 0;
        for (OfferingTile tile : offeringTrack) {
            if (!tile.isAvailable()) {
                placedCount++;
            }
        }
        return placedCount == expectedCount;
    }

    /**
     * Returns the leftmost player occupying a tile on the offering track.
     *
     * @return the first player from the offering track, or {@code null} if all tiles are free
     */
    public Player getFirstPlayerFromOfferingTrack() {
        for (OfferingTile tile : offeringTrack) {
            if (!tile.isAvailable()) {
                return tile.getOccupiedBy();
            }
        }
        return null;
    }

    // =====================================================================
    // DISCONNECTION MANAGEMENT AND TURN ORDER SHIFT
    // =====================================================================

    /**
     * Removes the player from the Turn Order and shifts up (towards slot 0)
     * all totems of the players who were below him.
     */
    public void removePlayerFromTurnOrderAndShiftUp(Player disconnectedPlayer) {
        int emptyIndex = -1;

        // 1. Find the disconnected player and free their slot
        for (int i = 0; i < turnOrderTile.size(); i++) {
            if (turnOrderTile.get(i).getOccupiedBy() != null &&
                    turnOrderTile.get(i).getOccupiedBy().getNickname().equals(disconnectedPlayer.getNickname())) {
                turnOrderTile.get(i).freeSlot();
                emptyIndex = i;
                break;
            }
        }

        // 2. Shift all players below by ONE position up
        if (emptyIndex != -1) {
            for (int i = emptyIndex; i < turnOrderTile.size() - 1; i++) {
                Player nextPlayer = turnOrderTile.get(i + 1).getOccupiedBy();
                turnOrderTile.get(i).freeSlot(); // Ensures current slot is clean
                if (nextPlayer != null) {
                    turnOrderTile.get(i + 1).freeSlot();
                    turnOrderTile.get(i).occupy(nextPlayer);
                }
            }
        }
    }

    /**
     * Reinserts a reconnected player's totem into the first
     * available free position from the top.
     */
    public void addPlayerToFirstAvailableTurnOrderSlot(Player reconnectedPlayer) {
        for (TurnOrderSlot slot : turnOrderTile) {
            if (slot.getOccupiedBy() == null) {
                slot.occupy(reconnectedPlayer);
                break;
            }
        }
    }

    /**
     * Discards all building cards from the lower row.
     * Used when entering Era III.
     */
    public void discardLowerRowBuildings() {
        buildingCardsLowerRow.clear();
    }

    /**
     * Clears all character cards from the lower row.
     * Events in the lower row are not affected.
     */
    public void clearLowerRow() {
        characterLowerRow.clear();
    }


    /**
     * Returns the offering track (list of offering tiles).
     *
     * @return the offering track
     */
    public List<OfferingTile> getOfferingTrack() { return offeringTrack; }

    /**
     * Returns the turn order track (list of turn order slots).
     *
     * @return the turn order track
     */
    public List<TurnOrderSlot> getTurnOrderTile() { return turnOrderTile;}

    /**
     * Returns the building cards in the upper row.
     *
     * @return list of building cards in the upper row
     */
    public List<BuildingCard> getBuildingCardsUpperRow() { return buildingCardsUpperRow; }

    /**
     * Returns the building cards in the lower row.
     *
     * @return list of building cards in the lower row
     */
    public List<BuildingCard> getBuildingCardsLowerRow() { return buildingCardsLowerRow; }

    /**
     * Returns the character cards in the upper row.
     *
     * @return list of character cards in the upper row
     */
    public List<CharacterCard> getCharacterUpperRow() { return characterUpperRow; }

    /**
     * Returns the character cards in the lower row.
     *
     * @return list of character cards in the lower row
     */
    public List<CharacterCard> getCharacterLowerRow() { return characterLowerRow; }

    /**
     * Returns the event cards in the upper row.
     *
     * @return list of event cards in the upper row
     */
    public List<EventCard> getEventUpperRow() { return eventUpperRow; }

    /**
     * Returns the event cards in the lower row.
     *
     * @return list of event cards in the lower row
     */
    public List<EventCard> getEventLowerRow() { return eventLowerRow; }
}

