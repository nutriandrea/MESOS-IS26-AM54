package it.polimi.ingsw.am54.model;

import java.util.ArrayList;

/**
 * Represents a participant in the game.
 * Holds the player's personal state, including resources, inventory (tribe and buildings),
 * and temporary modifiers applied during specific game phases (like Event resolution).
 */
public class Player {

    private String nickname;
    private TotemColors totemColor;

    // Core resources
    private int food;
    private int aura;
    private int magicPoints;

    // Player inventory
    private ArrayList<CharacterCard> myTribe;
    private ArrayList<BuildingCard> myBuildings;

    // Permanent discounts (applied when acquiring cards)
    private int myBuildingDiscount;
    private int myFoodDiscount;

    // Temporary modifiers (active only during the Event phase)
    private int eventCostFood;
    private int eventCostAura;
    private boolean shamanNoLoss;
    private boolean shamanDoubleAura;

    // State flags
    private boolean extraPick;
    private boolean hasPlacedTotem;
    private transient OfferingTile currentTile;

    /**
     * Default constructor initializing a fresh player state.
     */
    public Player() {
        this.food = 0;
        this.aura = 0;
        this.magicPoints = 0;
        this.myTribe = new ArrayList<>();
        this.myBuildings = new ArrayList<>();
        this.hasPlacedTotem = false;
        this.shamanNoLoss = false;
        this.shamanDoubleAura = false;
    }

    // --- BASIC GETTERS & SETTERS ---

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public TotemColors getTotemColors() { return totemColor; }
    public void setTotemColor(TotemColors color) { this.totemColor = color; }

    public int getFood() { return food; }

    /**
     * Alias for getFood() maintained for backward compatibility.
     * @return The current amount of food.
     */
    public int getFoodTokens() { return food; }

    public int getAuraPoints() { return aura; }
    public int getMagicPoints() { return magicPoints; }

    public ArrayList<CharacterCard> getMyTribe() { return myTribe; }
    public ArrayList<BuildingCard> getBuildings() { return myBuildings; }

    public OfferingTile getCurrentTile() { return currentTile; }
    public void setCurrentTile(OfferingTile tile) { this.currentTile = tile; }

    public boolean hasPlacedTotem() { return hasPlacedTotem; }
    public void setHasPlacedTotem(boolean value) { this.hasPlacedTotem = value; }

    public boolean hasExtraPick() { return extraPick; }
    public void setExtraPick(boolean extraPick) { this.extraPick = extraPick; }

    // --- RESOURCE MODIFIERS ---

    /**
     * Modifies the player's food amount. Ensures food never drops below zero.
     * @param amount The value to add (positive) or subtract (negative).
     */
    public void modifyFood(int amount) {
        if (amount > 0) {
            this.food += amount;
        } else {
            this.food = Math.max(0, this.food - (-amount));
        }
    }

    public void modifyAuraPoints(int amount) {
        this.aura += amount;
    }

    public void modifyMagicPoints(int amount) {
        this.magicPoints += amount;
    }

    public void modifyMyTribe(CharacterCard c) {
        myTribe.add(c);
    }

    // --- PERMANENT DISCOUNTS ---

    public int getMyBuildingDiscount() { return myBuildingDiscount; }
    public void modifyMyBuildingDiscount(int amount) { this.myBuildingDiscount += amount; }

    public int getMyFoodDiscount() { return myFoodDiscount; }

    // --- TEMPORARY MODIFIERS (EVENTS) ---

    public int getEventCostFood() { return eventCostFood; }
    public void setEventCostFood(int eventCost) { this.eventCostFood = eventCost; }

    public int getEventCostAura() { return eventCostAura; }
    public void setEventCostAura(int eventCost) { this.eventCostAura = eventCost; }

    public boolean hasShamanNoLoss() { return shamanNoLoss; }
    public void setShamanNoLoss(boolean shamanNoLoss) { this.shamanNoLoss = shamanNoLoss; }

    public boolean hasShamanDoubleAura() { return shamanDoubleAura; }
    public void setShamanDoubleAura(boolean shamanDoubleAura) { this.shamanDoubleAura = shamanDoubleAura; }

    /**
     * Resets all temporary modifiers used during the Event phase.
     * Must be called immediately after an Event is fully resolved.
     */
    public void resetEventAttributes() {
        this.eventCostAura = 0;
        this.eventCostFood = 0;
        this.shamanNoLoss = false;
        this.shamanDoubleAura = false;
    }

    // --- ENDGAME MODIFIERS ---

    private int endGameBuildingsAura = 0;

    public int getEndGameBuildingsAura() {
        return endGameBuildingsAura;
    }

    public void setEndGameBuildingsAura(int value) {
        this.endGameBuildingsAura = value;
    }

    /**
     * Calculates the total Aura accumulated up to the end of the final round,
     * prior to the activation of end-game scoring buildings.
     */
    public int getBaseAuraBeforeEndGame() {
        return this.aura - this.endGameBuildingsAura;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return nickname != null && nickname.equalsIgnoreCase(player.getNickname());
    }

    @Override
    public int hashCode() {
        return nickname != null ? nickname.toLowerCase().hashCode() : 0;
    }
}