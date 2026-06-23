package it.polimi.ingsw.am54.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.util.*;

/**
 * Represents the physical layout of the game, managing the various card rows,
 * the offering track, and the turn order tile.
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
     * Initializes the main structural elements of the board.
     * @param numPlayers The number of players, dictating the board's configuration.
     */
    public void initializeLayout(int numPlayers) {
        initializeOfferingTiles(numPlayers);
        initializeTurnOrderTile(numPlayers);
    }

    /**
     * Loads the specific offering tiles required for the current player count
     * from the JSON configuration.
     */
    private void initializeOfferingTiles(int numPlayers) {
        offeringTrack.clear();

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Critical error: Invalid player count (" + numPlayers + ")");
        }

        List<Character> neededTiles = new ArrayList<>();

        try {
            java.io.InputStream is = getClass().getResourceAsStream("/data/OfferingTilesSetup.json");
            JsonObject rootNode = JsonParser.parseReader(new java.io.InputStreamReader(is)).getAsJsonObject();
            String playerKey = String.valueOf(numPlayers);
            JsonArray arrayTessere = rootNode.getAsJsonObject("requiredTiles").getAsJsonArray(playerKey);

            if (arrayTessere != null) {
                for (JsonElement elem : arrayTessere) {
                    neededTiles.add(elem.getAsString().charAt(0));
                }
            }

            JsonArray arrayTiles = rootNode.getAsJsonArray("tiles");

            if (arrayTiles != null) {
                for (JsonElement elem : arrayTiles) {
                    JsonObject node = elem.getAsJsonObject();
                    char idLetto = node.get("id").getAsString().charAt(0);

                    if (neededTiles.contains(idLetto)) {
                        int upper = node.get("cardsFromUpper").getAsInt();
                        int lower = node.get("cardsFromLower").getAsInt();
                        int food  = node.get("foodBonus").getAsInt();

                        OfferingTile tile = new OfferingTile(idLetto, upper, lower, food);
                        offeringTrack.add(tile);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fatal error: Could not initialize Offering Tiles from JSON configuration.", e);
        }

        offeringTrack.sort(Comparator.comparing(OfferingTile::getId));
    }

    public void addBuildingCard(BuildingCard card, String row) {
        if (row.equalsIgnoreCase("upper")) {
            buildingCardsUpperRow.add(card);
        } else {
            buildingCardsLowerRow.add(card);
        }
    }

    public int getEventRowSize(String row) {
        return row.equalsIgnoreCase("upper") ? eventUpperRow.size() : eventLowerRow.size();
    }

    /**
     * Configures the turn order slots and their associated food bonuses.
     */
    public void initializeTurnOrderTile(int numPlayers) {
        this.turnOrderTile.clear();

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Critical error: Invalid player count (" + numPlayers + ")");
        }

        try {
            java.io.InputStream is = getClass().getResourceAsStream("/data/TurnOrderConfig.json");
            JsonObject rootNode = JsonParser.parseReader(new java.io.InputStreamReader(is)).getAsJsonObject();
            JsonArray bonusArray = rootNode.getAsJsonObject("bonusFood").getAsJsonArray(String.valueOf(numPlayers));

            if (bonusArray != null) {
                for (int i = 0; i < bonusArray.size(); i++) {
                    int position = i + 1;
                    int bonus = bonusArray.get(i).getAsInt();

                    TurnOrderSlot slot = new TurnOrderSlot(position, bonus);
                    this.turnOrderTile.add(slot);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fatal error during Turn Order initialization", e);
        }
    }

    public void addCharacterCard(CharacterCard card, String row) {
        if (row.equalsIgnoreCase("upper")) {
            characterUpperRow.add(card);
        } else {
            characterLowerRow.add(card);
        }
    }

    public void addEventCard(EventCard card, String row) {
        eventUpperRow.add(card);
    }

    /**
     * Posiziona i giocatori negli slot dell'ordine di turno secondo l'ordine (già
     * mescolato) ricevuto, assegnando l'i-esimo giocatore all'i-esimo slot.
     *
     * @param randomPlayers i giocatori nell'ordine di turno iniziale
     */
    public void setupInitialTurnOrder(List<Player> randomPlayers) {
        for (int i = 0; i < randomPlayers.size(); i++) {
            Player p = randomPlayers.get(i);
            TurnOrderSlot slot = this.turnOrderTile.get(i);
            slot.occupy(p);
        }
    }

    /**
     * Shifts remaining characters and events from the upper row to the lower row,
     * typically performed at the end of a round.
     */
    public void moveUpperToLower() {
        characterLowerRow.addAll(characterUpperRow);
        characterUpperRow.clear();

        eventLowerRow.addAll(eventUpperRow);
        eventUpperRow.clear();
    }

    /**
     * Shifts buildings from the upper row to the lower row.
     */
    public void moveBuildings() {
        buildingCardsLowerRow.clear();
        buildingCardsLowerRow.addAll(buildingCardsUpperRow);
        buildingCardsUpperRow.clear();
    }

    /**
     * Places a player's totem on an offering tile and frees their previous slot
     * in the turn order track to dynamically recalculate turn order later.
     */
    public void addTotemToPosition(Player player, OfferingTile tile) {
        tile.occupy(player);

        for (TurnOrderSlot slot : turnOrderTile) {
            if (player.equals(slot.getOccupiedBy())) {
                slot.freeSlot();
                break;
            }
        }
    }

    /**
     * Rimuove e restituisce la carta Personaggio all'indice indicato nella fila data.
     *
     * @param row   la fila ("upper" o "lower")
     * @param index l'indice della carta
     * @return la carta rimossa, o {@code null} se l'indice non è valido
     */
    public CharacterCard takeCharacterCard(String row, int index) {
        List<CharacterCard> targetRow = row.equalsIgnoreCase("upper") ? characterUpperRow : characterLowerRow;
        if (index < 0 || index >= targetRow.size()) return null;
        return targetRow.remove(index);
    }

    /**
     * Rimuove e restituisce la carta Edificio all'indice indicato nella fila data.
     *
     * @param row   la fila ("upper" o "lower")
     * @param index l'indice della carta (relativo agli edifici)
     * @return la carta rimossa, o {@code null} se l'indice non è valido
     */
    public BuildingCard takeBuildingCard(String row, int index) {
        List<BuildingCard> targetRow = row.equalsIgnoreCase("upper") ? buildingCardsUpperRow : buildingCardsLowerRow;
        if (index < 0 || index >= targetRow.size()) return null;
        return targetRow.remove(index);
    }

    public int getCharacterRowSize(String row) {
        return row.equalsIgnoreCase("upper") ? characterUpperRow.size() : characterLowerRow.size();
    }

    public int getBuildingRowSize(String row) {
        return row.equalsIgnoreCase("upper") ? buildingCardsUpperRow.size() : buildingCardsLowerRow.size();
    }

    /**
     * Extracts the next event to be resolved from the lower row.
     * Sustenance events (FARMER) are always resolved last.
     */
    public EventCard popNextEvent() {
        if (eventLowerRow.isEmpty()) return null;

        EventCard nextEvent = eventLowerRow.stream()
                .min((a, b) -> {
                    boolean aIsFarmer = a.isSustenanceEvent();
                    boolean bIsFarmer = b.isSustenanceEvent();

                    if (aIsFarmer != bIsFarmer) {
                        return aIsFarmer ? 1 : -1;
                    }
                    return Integer.compare(a.getEra(), b.getEra());
                })
                .orElse(null);

        if (nextEvent != null) {
            eventLowerRow.remove(nextEvent);
        }

        return nextEvent;
    }

    /**
     * Extracts the next visible event from the board, prioritizing the lower row.
     */
    public EventCard popNextVisibleEvent() {
        EventCard next = popNextEvent();

        if (next == null && !eventUpperRow.isEmpty()) {
            next = eventUpperRow.stream()
                    .min((a, b) -> {
                        boolean aIsFarmer = a.getName() != null && a.getName().toUpperCase().contains("FARMER");
                        boolean bIsFarmer = b.getName() != null && b.getName().toUpperCase().contains("FARMER");

                        if (aIsFarmer != bIsFarmer) return aIsFarmer ? 1 : -1;
                        return Integer.compare(a.getEra(), b.getEra());
                    })
                    .orElse(null);

            if (next != null) {
                eventUpperRow.remove(next);
            }
        }

        return next;
    }

    /**
     * Verifies if the totem placement phase is complete.
     * Safely handles edge cases involving disconnected players (ghosts)
     * by checking the actual turn order physical state.
     * * @param expectedCount The number of active players.
     * @return true if all present players have placed their totems.
     */
    public boolean allTotemsPlaced(int expectedCount) {
        int placedCount = 0;
        for (OfferingTile tile : offeringTrack) {
            if (!tile.isAvailable()) placedCount++;
        }

        if (placedCount >= expectedCount) return true;

        boolean someoneStillNeedsToPlace = false;
        for (TurnOrderSlot slot : turnOrderTile) {
            Player p = slot.getOccupiedBy();
            if (p != null && !p.hasPlacedTotem()) {
                someoneStillNeedsToPlace = true;
                break;
            }
        }

        return !someoneStillNeedsToPlace;
    }

    /**
     * Restituisce il primo giocatore (in ordine di tracciato) che ha un totem su una
     * tessera offerta: è il primo a dover pescare nella fase di Picking.
     *
     * @return il primo occupante del tracciato offerte, o {@code null} se nessuno
     */
    public Player getFirstPlayerFromOfferingTrack() {
        for (OfferingTile tile : offeringTrack) {
            if (!tile.isAvailable()) {
                return tile.getOccupiedBy();
            }
        }
        return null;
    }

    /**
     * Removes a disconnected player from the turn order and shifts all
     * subsequent players up by one slot to maintain a contiguous queue.
     */
    public void removePlayerFromTurnOrderAndShiftUp(Player disconnectedPlayer) {
        int emptyIndex = -1;

        for (int i = 0; i < turnOrderTile.size(); i++) {
            if (turnOrderTile.get(i).getOccupiedBy() != null &&
                    turnOrderTile.get(i).getOccupiedBy().getNickname().equals(disconnectedPlayer.getNickname())) {
                turnOrderTile.get(i).freeSlot();
                emptyIndex = i;
                break;
            }
        }

        if (emptyIndex != -1) {
            for (int i = emptyIndex; i < turnOrderTile.size() - 1; i++) {
                Player nextPlayer = turnOrderTile.get(i + 1).getOccupiedBy();
                turnOrderTile.get(i).freeSlot();
                if (nextPlayer != null) {
                    turnOrderTile.get(i + 1).freeSlot();
                    turnOrderTile.get(i).occupy(nextPlayer);
                }
            }
        }
    }

    public void discardLowerRowBuildings() {
        buildingCardsLowerRow.clear();
    }

    public void clearLowerRow() {
        characterLowerRow.clear();
    }

    // --- GETTERS ---
    public List<OfferingTile> getOfferingTrack() { return offeringTrack; }
    public List<TurnOrderSlot> getTurnOrderTile() { return turnOrderTile;}
    public List<BuildingCard> getBuildingCardsUpperRow() { return buildingCardsUpperRow; }
    public List<BuildingCard> getBuildingCardsLowerRow() { return buildingCardsLowerRow; }
    public List<CharacterCard> getCharacterUpperRow() { return characterUpperRow; }
    public List<CharacterCard> getCharacterLowerRow() { return characterLowerRow; }
    public List<EventCard> getEventUpperRow() { return eventUpperRow; }
    public List<EventCard> getEventLowerRow() { return eventLowerRow; }
}