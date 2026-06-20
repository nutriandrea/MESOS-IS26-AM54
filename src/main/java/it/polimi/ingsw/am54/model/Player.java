package it.polimi.ingsw.am54.model;

import java.util.ArrayList;

/**
 * Game player. Maintains the totem (color), resources (food, aura, magic points),
 * accumulated cards (tribe and buildings), permanent discounts for card purchases,
 * temporary event phase modifiers and status flags (totem placed, extra pick).
 * @author AM54
 */
public class Player {

    private String nickname;
    private TotemColors totemColor;

    // Base resources
    private int food;
    private int aura;
    private int magicPoints;

    // Data structures
    private ArrayList<CharacterCard> myTribe;
    private ArrayList<BuildingCard> myBuildings;

    // Permanent discounts (Card purchases)
    private int myBuildingDiscount;
    private int myFoodDiscount;

    // Temporary modifiers (Event Phase)
    private int eventCostFood;
    private int eventCostAura;
    private boolean shamanNoLoss;
    private boolean shamanDoubleAura;

    // Status flags
    private boolean extraPick;
    private boolean hasPlacedTotem;
    private transient OfferingTile currentTile;
    /**
     * Default constructor
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

    // --- BASE GETTERS AND SETTERS ---

    /**
     * Returns the player's nickname.
     *
     * @return the nickname
     */
    public String getNickname() { return nickname; }

    /**
     * Sets the player's nickname.
     *
     * @param nickname the nickname to set
     */
    public void setNickname(String nickname) { this.nickname = nickname; }

    /**
     * Returns the player's totem color.
     *
     * @return the totem color, or {@code null} if not yet chosen
     */
    public TotemColors getTotemColors() { return totemColor; }

    /**
     * Sets the player's totem color.
     *
     * @param color the totem color to set
     */
    public void setTotemColor(TotemColors color) { this.totemColor = color; }

    /**
     * Returns the player's current food amount.
     *
     * @return the food amount
     */
    public int getFood() { return food; }

    /**
     * Returns the player's food tokens (alias for {@link #getFood()}).
     *
     * @return the food amount
     */
    public int getFoodTokens() { return food; }

    /**
     * Returns the player's current aura points.
     *
     * @return the aura points
     */
    public int getAuraPoints() { return aura; }

    /**
     * Returns the player's current magic points.
     *
     * @return the magic points
     */
    public int getMagicPoints() { return magicPoints; }

    /**
     * Returns the list of character cards in the player's tribe.
     *
     * @return the player's tribe
     */
    public ArrayList<CharacterCard> getMyTribe() { return myTribe; }

    /**
     * Returns the list of building cards owned by the player.
     *
     * @return the player's buildings
     */
    public ArrayList<BuildingCard> getBuildings() { return myBuildings; }

    /**
     * Returns the offering tile currently occupied by the player.
     *
     * @return the current tile, or {@code null} if not on the offering track
     */
    public OfferingTile getCurrentTile() { return currentTile; }

    /**
     * Sets the offering tile currently occupied by the player.
     *
     * @param tile the tile to set
     */
    public void setCurrentTile(OfferingTile tile) { this.currentTile = tile; }

    /**
     * Returns whether the player has placed their totem on the offering track.
     *
     * @return {@code true} if the totem has been placed
     */
    public boolean hasPlacedTotem() { return hasPlacedTotem; }

    /**
     * Sets whether the player has placed their totem.
     *
     * @param value the placed status to set
     */
    public void setHasPlacedTotem(boolean value) { this.hasPlacedTotem = value; }

    /**
     * Returns whether the player has an extra pick available.
     *
     * @return {@code true} if an extra pick is available
     */
    public boolean hasExtraPick() { return extraPick; }

    /**
     * Sets whether the player has an extra pick available.
     *
     * @param extraPick the extra pick flag to set
     */
    public void setExtraPick(boolean extraPick) { this.extraPick = extraPick; }

    // --- RESOURCE MODIFIERS ---

    /**
     * Modifies the player's food by the specified amount.
     * Positive amounts add food; negative amounts subtract (floor at 0).
     *
     * @param amount the food delta (positive to add, negative to subtract)
     */
    public void modifyFood(int amount) {
        if (amount > 0) {
            this.food += amount;
        } else {
            this.food = Math.max(0, this.food - (-amount));
        }
    }

    /**
     * Modifies the player's aura points by the specified amount.
     *
     * @param amount the aura delta (can be negative)
     */
    public void modifyAuraPoints(int amount) {
        this.aura += amount;
    }

    /**
     * Modifies the player's magic points by the specified amount.
     *
     * @param amount the magic points delta (can be negative)
     */
    public void modifyMagicPoints(int amount) {
        this.magicPoints += amount;
    }

    /**
     * Adds a character card to the player's tribe.
     *
     * @param c the character card to add
     */
    public void modifyMyTribe(CharacterCard c) {
        myTribe.add(c);
    }

    // --- PERMANENT DISCOUNTS ---

    /**
     * Returns the player's permanent building discount.
     *
     * @return the building discount amount
     */
    public int getMyBuildingDiscount() { return myBuildingDiscount; }

    /**
     * Modifies the player's permanent building discount by the specified amount.
     *
     * @param amount the discount delta
     */
    public void modifyMyBuildingDiscount(int amount) { this.myBuildingDiscount += amount; }

    /**
     * Returns the player's permanent food discount.
     *
     * @return the food discount amount
     */
    public int getMyFoodDiscount() { return myFoodDiscount; }

    /**
     * Modifies the player's permanent food discount by the specified amount.
     *
     * @param amount the discount delta
     */
    public void modifyMyFoodDiscount(int amount) { this.myFoodDiscount += amount; }

    // --- TEMPORARY MODIFIERS (EVENTS) ---

    /**
     * Returns the temporary food cost modifier for the current event phase.
     *
     * @return the event food cost modifier
     */
    public int getEventCostFood() { return eventCostFood; }

    /**
     * Sets the temporary food cost modifier for the current event phase.
     *
     * @param eventCost the event food cost modifier
     */
    public void setEventCostFood(int eventCost) { this.eventCostFood = eventCost; }

    /**
     * Returns the temporary aura cost modifier for the current event phase.
     *
     * @return the event aura cost modifier
     */
    public int getEventCostAura() { return eventCostAura; }

    /**
     * Sets the temporary aura cost modifier for the current event phase.
     *
     * @param eventCost the event aura cost modifier
     */
    public void setEventCostAura(int eventCost) { this.eventCostAura = eventCost; }

    /**
     * Returns whether the player is protected from aura loss by the Shaman effect.
     *
     * @return {@code true} if Shaman no-loss is active
     */
    public boolean hasShamanNoLoss() { return shamanNoLoss; }

    /**
     * Sets whether the player is protected from aura loss by the Shaman effect.
     *
     * @param shamanNoLoss the Shaman no-loss flag
     */
    public void setShamanNoLoss(boolean shamanNoLoss) { this.shamanNoLoss = shamanNoLoss; }

    /**
     * Returns whether the player has the Shaman double aura effect active.
     *
     * @return {@code true} if Shaman double aura is active
     */
    public boolean hasShamanDoubleAura() { return shamanDoubleAura; }

    /**
     * Sets whether the player has the Shaman double aura effect active.
     *
     * @param shamanDoubleAura the Shaman double aura flag
     */
    public void setShamanDoubleAura(boolean shamanDoubleAura) { this.shamanDoubleAura = shamanDoubleAura; }

    /**
     * Resets all temporary event phase modifiers to zero.
     * Must be called at the end of each Event resolution.
     */
    public void resetEventAttributes() {
        this.eventCostAura = 0;
        this.eventCostFood = 0;
        this.shamanNoLoss = false;
        this.shamanDoubleAura = false;
    }

    // Final modifier for buildings with onEndGame activation
    private int endGameBuildingsAura = 0;

    /**
     * Returns the end-game aura bonus from buildings with endgame activation.
     *
     * @return the end-game buildings aura
     */
    public int getEndGameBuildingsAura() {
        return endGameBuildingsAura;
    }

    /**
     * Sets the end-game aura bonus from buildings with endgame activation.
     *
     * @param value the end-game buildings aura
     */
    public void setEndGameBuildingsAura(int value) {
        this.endGameBuildingsAura = value;
    }

    /**
     * Returns the Aura accumulated up to the end of round 10 before the final buildings activation.
     */
    public int getBaseAuraBeforeEndGame() {
        return this.aura - this.endGameBuildingsAura;
    }

    /**
     * Compares this player to another object for equality.
     * Two players are equal if their nicknames match (case-insensitive).
     *
     * @param o the object to compare
     * @return {@code true} if the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return nickname != null && nickname.equalsIgnoreCase(player.getNickname());
    }

    /**
     * Returns the hash code of this player, based on the lowercase nickname.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return nickname != null ? nickname.toLowerCase().hashCode() : 0;
    }
}