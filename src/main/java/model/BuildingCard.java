package model;

import java.io.*;
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
            // Applica l'effetto. Passi 'null' al posto di CharacterCard perché questo è un Edificio!
            effect.apply(p, g, null);
        }
    }

    public int getFinalAura() {
        // TODO implement here
        return 0;
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
    public void buy(Player player) {
        int foodAmount = this.cost-player.getMyBuildingDiscount();
        if (foodAmount>=0) {player.modifyFood(-foodAmount);}

        this.addOwner(player);
        player.modifyMyTribe(this);

        //sarà poi la board a toglierselo, non la carta stessa
    }

    @Override
    public void pick(Player player, Game game) throws Exception {
        int finalCost = this.cost - player.getMyBuildingDiscount();
        finalCost = Math.max(0, finalCost);

        if (player.getFood() < finalCost) {
            throw new Exception("Cibo insufficiente per comprare l'edificio! Hai " + player.getFood() + ", servono " + finalCost);
        }

        player.modifyFood(-finalCost);
        this.addOwner(player);
        player.getBuildings().add(this);
        System.out.println(player.getNickname() + " ha comprato l'edificio: " + getName() + " per " + finalCost + " cibo");
    }

}