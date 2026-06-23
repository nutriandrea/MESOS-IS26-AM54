package it.polimi.ingsw.am54.model;

/**
 * Base class of all game cards (Characters, Events, Buildings).
 * Defines common behaviors (era, owner, image) and polymorphic
 * methods that subclasses specialize: picking, affordability, revealing
 * on the board and serialization for the client.
 */
public class Card {

    public Card(int era) {
        this.era = era;
    }

    private int era;

    private transient Player owner;

    public int getEra() {
        return era;
    }

    /**
     * Sets the card's era.
     * Used by EventCard.getName() to generate the card's name.
     *
     * @param era the card's era (1-3)
     */
    public void setEra(int era) {
        this.era = era;
    }

    public void addOwner(Player p) {
        this.owner = p;
    }

    /**
     * Executes the drawing/purchasing logic of the card by a player.
     * Default implementation not supported: each concrete subclass
     * redefines it (e.g. character enters tribe, building subtracts food).
     *
     * @param player the player who picks the card
     * @param game   the current game (to apply effects and rules)
     * @throws Exception if the card type is not pickable or the action is invalid
     */
    public void pick(Player player, Game game) throws Exception {
        throw new UnsupportedOperationException("Cannot pick this card type");
    }

    /**
     * Indicates whether the player can afford this card. Basically always true
     * (free cards); paid subclasses (e.g. {@link BuildingCard})
     * check available food.
     *
     * @param player the player who wants to buy the card
     * @return true if the card is purchasable by the player
     */
    public boolean canBePurchased(Player player) {
        return true;
    }

    /**
     * Reveals the card by placing it in the correct row of the board.
     * Each subclass knows which structure to enter (characters, events, buildings).
     *
     * @param board the board on which to reveal the card
     * @param row   the target row ("upper" or "lower")
     */
    public void reveal(Board board, String row) {
        throw new UnsupportedOperationException("Cannot reveal this generic card type");
    }

    private String imageId = "0";

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    /**
     * Returns a textual representation of the card for the Client.
     * Each subclass produces a specific format with its own fields.
     * Data is separated by '|' to ease client-side parsing.
     *
     * @return the serialized card string
     */
    public String toClientString() {
        throw new UnsupportedOperationException("Formato client non definito per questa carta");
    }
}