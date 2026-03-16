package model.Model;

import java.io.*;
import java.util.*;


public class BuildingCard extends Card implements IPurchasable {

    private String name;

    private int cost;

    private int finalAura;

    private Phases phase;

    private List<IEffect> effects;

    private JobType buildingType;


    public BuildingCard(JobType type, int cost, int finalAura, Phases phase, List<IEffect> effects, int era) {
        super(era);
        this.buildingType = type;
        this.cost = cost;
        this.finalAura = finalAura;
        this.phase = phase;
        this.effects = effects;
    }

    public int getFinalAura() {
        // TODO implement here
        return 0;
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

}