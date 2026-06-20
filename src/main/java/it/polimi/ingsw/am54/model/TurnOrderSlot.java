package it.polimi.ingsw.am54.model;

/**
 * Slot of the turn order track, associated with a position
 * and a food bonus that the occupying player receives upon returning the totem.
 * <p>
 * The turn order track determines the sequence in which players act during
 * a round. At the start of the round, players are ordered in these slots; at the end of each
 * player's turn, they return their totem, receive the food bonus associated with the
 * slot they occupy and the turn passes to the player in the next slot.
 * </p>
 * <p>
 * The slot position influences strategy: early slots offer lower food bonuses
 * but allow acting first, while later slots offer higher food bonuses
 * more generous but force you to wait.
 * </p>
 *
 * @see Board#getTurnOrderTile()
 * @see Game#nextPlayer()
 */
public class TurnOrderSlot {

    /** Progressive position in the slot (0-based: 0 = first player). */
    private int position;

    /** Amount of bonus food received by the player when returning the totem at end of turn. */
    private int bonusFood;

    /** Player currently sitting in this slot, {@code null} if free. */
    private Player occupiedBy;

    /**
     * Creates a turn order slot with specified position and food bonus.
     *
     * @param position  progressive position in the slot (0 = first player)
     * @param bonusFood amount of bonus food assigned to this slot
     */
    public TurnOrderSlot(int position, int bonusFood) {
        this.position = position;
        this.bonusFood = bonusFood;
    }

    /**
     * Assigns a player to this slot, occupying it.
     *
     * @param p the player to assign to the slot
     */
    public void occupy(Player p) {
        this.occupiedBy = p;
    }

    /**
     * Returns the progressive position of this slot in the track.
     *
     * @return the position (0-based)
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Returns the food bonus associated with this slot.
     *
     * @return amount of bonus food
     */
    public int getBonusFood() {
        return this.bonusFood;
    }

    /**
     * Returns the player currently occupying this slot.
     *
     * @return the occupying player, or {@code null} if the slot is free
     */
    public Player getOccupiedBy() {
        return this.occupiedBy;
    }

    /**
     * Frees the slot, removing the currently occupying player.
     * <p>
     * Used when a player is moved to another slot
     * (e.g. {@link Game#moveToLastSlot(Player)}) or when the round ends.
     * </p>
     */
    public void freeSlot() {
        this.occupiedBy = null;
    }
}