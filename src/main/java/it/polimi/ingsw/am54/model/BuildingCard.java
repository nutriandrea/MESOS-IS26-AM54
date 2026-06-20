package it.polimi.ingsw.am54.model;

import java.util.*;


/**
 * Building Card purchasable by players. Has a food cost, a final aura value,
 * an activation phase (immediate, aura or endgame) and a list of associated effects.
 * Effect resolution occurs at purchase time via resolveEffects().
 * @author AM54
 */
public class BuildingCard extends Card {

    private String name;

    private int cost;

    private int finalAura;

    private Phases phase;

    private List<IEffect<BuildingCard>> effects;

    private JobType buildingType;

    /**
     * Constructs a BuildingCard with the specified type, cost, aura, phase, effects and era.
     *
     * @param type    the building job type
     * @param cost    the food cost to purchase the building
     * @param finalAura the base aura points contributed at endgame
     * @param phase   the activation phase of the building effects
     * @param effects the list of effects associated with the building
     * @param era     the era to which the building belongs (1-3)
     */
    public BuildingCard(JobType type, int cost, int finalAura, Phases phase, List<IEffect<BuildingCard>> effects, int era) {
        super(era);
        this.buildingType = type;
        this.cost = cost;
        this.finalAura = finalAura;
        this.phase = phase;
        this.effects = effects;
    }

    /**
     * Resolves all purchase-time effects of this building card on the specified player.
     *
     * @param p the player who purchased the building
     * @param g the current game instance
     */
    public void resolveEffects(Player p, Game g) {
        for (IEffect<BuildingCard> effect : this.effects) {
            effect.applyOnPurchase(p, g, this);
        }
    }

    /**
     * Returns the final aura points contributed by this building at endgame.
     *
     * @return the final aura value
     */
    public int getFinalAura() {
        return finalAura;
    }
    /**
     * Returns the list of effects associated with this building.
     *
     * @return the list of effects
     */
    public List<IEffect<BuildingCard>> getEffects(){
        return this.effects;
    }

    /**
     * Returns the name of this building.
     *
     * @return the building name, or {@code null} if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the food cost to purchase this building.
     *
     * @return the building cost
     */
    public int getCost() {
        return cost;
    }

    /**
     * Returns the job type of this building.
     *
     * @return the building type
     */
    public JobType getBuildingType() {
        return buildingType;
    }

    /**
     * Purchases this building: checks affordability, deducts food (accounting for
     * discounts), assigns ownership, and resolves purchase effects.
     *
     * @param player the player purchasing the building
     * @param game   the current game instance
     * @throws GameException if the player has insufficient food to buy the building
     */
    @Override
    public void pick(Player player, Game game) throws GameException {
        int finalCost = this.cost - player.getMyBuildingDiscount();
        finalCost = Math.max(0, finalCost);

        //checks if the player can actually purchase
        if (player.getFood() < finalCost) {
            throw new GameException("Insufficient food to buy the building! You have " + player.getFood() + ", need " + finalCost);
        }

        player.modifyFood(-finalCost);
        this.addOwner(player);
        player.getBuildings().add(this);

        this.resolveEffects(player, game);
    }

    /**
     * Sets the name of this building.
     *
     * @param name the building name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Checks whether the player can afford to purchase this building,
     * accounting for the player's building discount.
     *
     * @param player the player to check
     * @return {@code true} if the player has enough food to buy the building
     */
    @Override
    public boolean canBePurchased(Player player) {
        // Calculate the effective cost considering the player's discount
        int finalCost = this.cost - player.getMyBuildingDiscount();
        finalCost = Math.max(0, finalCost);
        // Verify if the current food is sufficient
        return player.getFood() >= finalCost;
    }

    /**
     * Reveals this building card by placing it on the board in the specified row.
     *
     * @param board the game board
     * @param row   the target row ("upper" or "lower")
     */
    @Override
    public void reveal(Board board, String row) {
        // This method knows exactly how to put the building back in its place!
        board.addBuildingCard(this, row);
    }

    /**
     * Returns a pipe-separated textual representation of this building for the client.
     * Format: {@code imageId|era|0|BUILDING|cost|finalAura|name}
     *
     * @return the client-facing string representation
     */
    @Override
    public String toClientString() {
        String nomeEdificio = getName() != null ? getName() : "Special Building";
        return getImageId() + "|" + getEra() + "|0|BUILDING|" + getCost() + "|" + getFinalAura() + "|" + nomeEdificio;
    }
}