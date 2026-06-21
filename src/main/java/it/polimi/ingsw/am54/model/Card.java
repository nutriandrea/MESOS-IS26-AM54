package it.polimi.ingsw.am54.model;

/**
 * Abstract base class for all game cards. Defines the era, owner,
 * image identifier and common methods for pick, reveal and textual
 * representation for the client. Subclasses specialize behavior for
 * each card type (Building, Character, Event, Tribe).
 * @author AM54
 */
public class Card {


    /**
     * Constructs a Card belonging to the specified era.
     *
     * @param era the era to which the card belongs (1-3)
     */
    public Card(int era) {
        this.era = era;
    }

    private int era;

    private transient Player owner;

    /**
     * Returns the era to which this card belongs.
     *
     * @return the era (1-3)
     */
    public int getEra() {
        return era;
    }

    /**
     * Sets the card era.
     * Used by EventCard.getName() to generate the card name.
     *
     * @param era the card era (1-3)
     */
    public void setEra(int era) {
        this.era = era;
    }

    /**
     * Sets the owner of this card.
     *
     * @param p the player who owns the card
     */
    public void addOwner(Player p) {
        this.owner = p;
    }

    /**
     * Attempts to pick (purchase/draw) this card.
     * Default implementation throws; subclasses override with specific logic.
     *
     * @param player the player picking the card
     * @param game   the current game instance
     * @throws GameException if the card cannot be picked
     */
    public void pick(Player player, Game game) throws GameException {
        throw new UnsupportedOperationException("Cannot pick this card type");
    }

    /**
     * Checks whether the specified player can purchase this card.
     * Default implementation always returns {@code true}; subclasses override with specific logic.
     *
     * @param player the player to check
     * @return {@code true} if the card can be purchased
     */
    public boolean canBePurchased(Player player) {
        return true;
    }

    /**
     * Reveals this card by placing it on the board in the specified row.
     * Default implementation throws; subclasses override with specific logic.
     *
     * @param board the game board
     * @param row   the target row ("upper" or "lower")
     */
    public void reveal(Board board, String row) {
        throw new UnsupportedOperationException("Cannot reveal this generic card type");
    }

    private String imageId = "0";

    /**
     * Returns the image identifier of this card.
     *
     * @return the image ID string
     */
    public String getImageId() {
        return imageId;
    }

    /**
     * Sets the image identifier of this card.
     *
     * @param imageId the image ID string to set
     */
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    /**
     * Returns a textual representation of the card for the Client.
     * Each subclass produces a specific format with its own fields.
     * Data is separated by '|' for easy client-side parsing.
     */
    public String toClientString() {
        throw new UnsupportedOperationException("Client format not defined for this card");
    }
}