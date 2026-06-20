package it.polimi.ingsw.am54.model;

/**
 * Offering tile on the offering track, associated with a position and player.
 * @author AM54
 */
public class OfferingTile {

    private char id;
    private int cardsFromUpper;
    private int cardsFromLower;
    private int foodBonus;
    private Player occupiedBy;

    private int remainingUpperPicks;
    private int remainingLowerPicks;

    /**
     * Constructs an OfferingTile with the specified id, card draw amounts and food bonus.
     *
     * @param id              the tile identifier character
     * @param cardsFromUpper  the number of cards drawable from the upper row
     * @param cardsFromLower  the number of cards drawable from the lower row
     * @param foodBonus       the food bonus granted when occupying this tile
     */
    public OfferingTile(char id, int cardsFromUpper, int cardsFromLower, int foodBonus) {
        this.id = id;
        this.cardsFromUpper = cardsFromUpper;
        this.cardsFromLower = cardsFromLower;
        this.foodBonus = foodBonus;
        this.occupiedBy = null; // default (not initialized as occupied)
        this.remainingUpperPicks = cardsFromUpper;
        this.remainingLowerPicks = cardsFromLower;
    }


    /**
     * Returns the identifier character of this tile.
     *
     * @return the tile id
     */
    public char getId() {
        return id;
    }

    /**
     * Returns the number of cards that can be taken from the upper row.
     * Used by PersistenceManager to save the tile state.
     *
     * @return number of cards from the upper row
     */
    public int getCardsFromUpper() {
        return cardsFromUpper;
    }

    /**
     * Returns the number of cards that can be taken from the lower row.
     * Used by PersistenceManager to save the tile state.
     *
     * @return number of cards from the lower row
     */
    public int getCardsFromLower() {
        return cardsFromLower;
    }

    /**
     * Returns the food bonus granted when occupying this tile.
     *
     * @return the food bonus value
     */
    public int getFoodBonus() {
        return foodBonus;
    }

    /**
     * Returns the player currently occupying this tile.
     *
     * @return the occupying player, or {@code null} if the tile is free
     */
    public Player getOccupiedBy() {
        return occupiedBy;
    }

    /**
     * Checks whether this tile is available (not occupied by any player).
     *
     * @return {@code true} if the tile is free
     */
    public boolean isAvailable() {
        return occupiedBy == null;
    }

    /**
     * Occupies this tile with the specified player and resets the pick counters.
     *
     * @param p the player occupying the tile
     */
    public void occupy(Player p) {
        this.occupiedBy = p;
        this.remainingUpperPicks = this.cardsFromUpper;
        this.remainingLowerPicks = this.cardsFromLower;
    }

    /**
     * Frees this tile by removing the currently occupying player.
     */
    public void freeSlot() {
        this.occupiedBy = null;
    }
    /**
     * Resets the upper and lower pick counters to their initial values.
     */
    public void resetPicks() {
        this.remainingUpperPicks = this.cardsFromUpper;
        this.remainingLowerPicks = this.cardsFromLower;
    }

    /**
     * Consumes one upper row pick if available.
     *
     * @return {@code true} if a pick was successfully consumed
     */
    public boolean useUpperPick() {
        if (remainingUpperPicks > 0) {
            remainingUpperPicks--;
            return true;
        }
        return false;
    }

    /**
     * Consumes one lower row pick if available.
     *
     * @return {@code true} if a pick was successfully consumed
     */
    public boolean useLowerPick() {
        if (remainingLowerPicks > 0) {
            remainingLowerPicks--;
            return true;
        }
        return false;
    }

    /**
     * Checks whether any picks (upper or lower) remain on this tile.
     *
     * @return {@code true} if at least one pick is still available
     */
    public boolean hasPicksLeft() {
        return remainingUpperPicks > 0 || remainingLowerPicks > 0;
    }

    /**
     * Returns the number of remaining picks from the upper row.
     *
     * @return remaining upper picks
     */
    public int getRemainingUpperPicks() {
        return remainingUpperPicks;
    }

    /**
     * Returns the number of remaining picks from the lower row.
     *
     * @return remaining lower picks
     */
    public int getRemainingLowerPicks() {
        return remainingLowerPicks;
    }

}