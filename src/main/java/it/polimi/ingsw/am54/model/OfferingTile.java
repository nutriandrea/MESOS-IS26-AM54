package it.polimi.ingsw.am54.model;

public class OfferingTile {

    private char id;
    private int cardsFromUpper;
    private int cardsFromLower;
    private int foodBonus;
    private Player occupiedBy;

    private int remainingUpperPicks;
    private int remainingLowerPicks;

    public OfferingTile(char id, int cardsFromUpper, int cardsFromLower, int foodBonus) {
        this.id = id;
        this.cardsFromUpper = cardsFromUpper;
        this.cardsFromLower = cardsFromLower;
        this.foodBonus = foodBonus;
        this.occupiedBy = null; // default (do not initialize occupied)
        this.remainingUpperPicks = cardsFromUpper;
        this.remainingLowerPicks = cardsFromLower;
    }


    public char getId() {
        return id;
    }

    /**
     * Returns the number of cards that can be taken from the upper row.
     * Used by the PersistenceManager to save the state of the tile.
     *
     * @return number of cards from the upper row
     */
    public int getCardsFromUpper() {
        return cardsFromUpper;
    }

    /**
     * Returns the number of cards that can be taken from the lower row.
     * Used by the PersistenceManager to save the state of the tile.
     *
     * @return number of cards from the lower row
     */
    public int getCardsFromLower() {
        return cardsFromLower;
    }

    public int getFoodBonus() {
        return foodBonus;
    }

    public Player getOccupiedBy() {
        return occupiedBy;
    }

    public boolean isAvailable() {
        return occupiedBy == null;
    }

    /**
     * Assigns the tile to a player (totem placement) and RESTORES the available
     * picks to their full value. Not to be used during crash recovery: in that case
     * {@link #restoreOccupant(Player)} is needed to avoid resetting the already spent picks.
     *
     * @param p the player who occupies the tile
     */
    public void occupy(Player p) {
        this.occupiedBy = p;
        this.remainingUpperPicks = this.cardsFromUpper;
        this.remainingLowerPicks = this.cardsFromLower;
    }

    // it seems to be bad practice to use occupy but with null as a reference
    public void freeSlot() {
        this.occupiedBy = null;
    }

    /**
     * Re-links the occupant reference WITHOUT touching the remaining pick counters.
     * Used during JSON recovery: occupy() would reset the picks to full and wipe
     * the progress the player had already made before the crash.
     */
    public void restoreOccupant(Player p) {
        this.occupiedBy = p;
    }
    public void resetPicks() {
        this.remainingUpperPicks = this.cardsFromUpper;
        this.remainingLowerPicks = this.cardsFromLower;
    }

    public boolean useUpperPick() {
        if (remainingUpperPicks > 0) {
            remainingUpperPicks--;
            return true;
        }
        return false;
    }

    public boolean useLowerPick() {
        if (remainingLowerPicks > 0) {
            remainingLowerPicks--;
            return true;
        }
        return false;
    }

    public int getRemainingUpperPicks() {
        return remainingUpperPicks;
    }

    public int getRemainingLowerPicks() {
        return remainingLowerPicks;
    }

}