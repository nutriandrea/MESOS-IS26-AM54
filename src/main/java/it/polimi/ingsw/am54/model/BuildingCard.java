package it.polimi.ingsw.am54.model;

import java.util.*;

/**
 * Building Card: unlike characters, it has a food cost (reduced by the
 * player's discounts) and provides aura and/or effects that trigger in different
 * phases (purchase, turn end, event, game end).
 */
public class BuildingCard extends Card {

    private String name;

    private int cost;

    private int finalAura;

    private Phases phase;

    private List<IEffect<BuildingCard>> effects;

    private JobType buildingType;

    public BuildingCard(JobType type, int cost, int finalAura, Phases phase, List<IEffect<BuildingCard>> effects, int era) {
        super(era);
        this.buildingType = type;
        this.cost = cost;
        this.finalAura = finalAura;
        this.phase = phase;
        this.effects = effects;
    }

    /**
     * Applies all the "on purchase" effects of the building on the player.
     *
     * @param p the player who built the building
     * @param g the current game
     */
    public void resolveEffects(Player p, Game g) {
        for (IEffect<BuildingCard> effect : this.effects) {
            effect.applyOnPurchase(p, g, this);
        }
    }

    public int getFinalAura() {
        return finalAura;
    }
    public List<IEffect<BuildingCard>> getEffects(){
        return this.effects;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public JobType getBuildingType() {
        return buildingType;
    }

    /**
     * Picks the building: calculates the actual cost (net of the player's
     * discount), verifies available food, subtracts the cost, assigns ownership
     * and resolves immediate effects.
     *
     * @param player the player who builds the building
     * @param game   the current game
     * @throws Exception if the player doesn't have enough food
     */
    @Override
    public void pick(Player player, Game game) throws Exception {
        int finalCost = this.cost - player.getMyBuildingDiscount();
        finalCost = Math.max(0, finalCost);

        //checks if the player can actually buy
        if (player.getFood() < finalCost) {
            throw new Exception("Cibo insufficiente per comprare l'edificio! Hai " + player.getFood() + ", servono " + finalCost);
        }

        player.modifyFood(-finalCost);
        this.addOwner(player);
        player.getBuildings().add(this);

        this.resolveEffects(player, game);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Indicates whether the player can afford the building, considering the
     * accumulated constructor discount.
     *
     * @param player the player who wants to build
     * @return true if the food is sufficient to cover the discounted cost
     */
    @Override
    public boolean canBePurchased(Player player) {
        // We calculate the actual cost considering the player's discount
        int finalCost = this.cost - player.getMyBuildingDiscount();
        finalCost = Math.max(0, finalCost);
        // We check if the current food is sufficient
        return player.getFood() >= finalCost;
    }

    /**
     * Reveals the building by placing it in the Buildings row of the board.
     *
     * @param board the game board
     * @param row   the target row ("upper" or "lower")
     */
    @Override
    public void reveal(Board board, String row) {
        // This method knows exactly how to put the building back in its place!
        board.addBuildingCard(this, row);
    }

    @Override
    public String toClientString() {
        String nomeEdificio = getName() != null ? getName() : "Edificio Speciale";
        return getImageId() + "|" + getEra() + "|0|BUILDING|" + getCost() + "|" + getFinalAura() + "|" + nomeEdificio;
    }
}