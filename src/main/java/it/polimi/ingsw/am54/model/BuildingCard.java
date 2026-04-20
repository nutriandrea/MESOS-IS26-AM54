package it.polimi.ingsw.am54.model;

import java.util.*;


public class BuildingCard extends Card implements IPurchasable {

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

    public void resolveEffects(Player p, Game g) {
        // Scorri tutti gli effetti assegnati a questo edificio
        for (IEffect effect : this.effects) {
            // Applica l'effetto. Passi null al posto di CharacterCard perché è un edificio
            effect.apply(p, g, null);
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

    @Override
    public boolean canBePurchased(Player player) {
        return (player.getFoodTokens()-player.getMyBuildingDiscount()>=this.cost);
    }

    @Override
    public void pick(Player player, Game game) throws Exception {
        int finalCost = this.cost - player.getMyBuildingDiscount();
        finalCost = Math.max(0, finalCost);

        //controlla se il player può effettivamente acquistare
        if (player.getFood() < finalCost) {
            throw new Exception("Cibo insufficiente per comprare l'edificio! Hai " + player.getFood() + ", servono " + finalCost);
        }

        player.modifyFood(-finalCost);
        this.addOwner(player);
        player.getBuildings().add(this);

        this.resolveEffects(player, game);
    }

}