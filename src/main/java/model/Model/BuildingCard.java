package model.Model;

import java.io.*;
import java.util.*;


public class BuildingCard extends Card implements IPurchasable {

    /**
     * Default constructor
     */
    public BuildingCard() {
    }

    
    private String name;

    
    private int cost;

    
    private int finalAura;

    
    private Phases phase;

    
    private List<IEffect> effects;

    
    private JobType eventType;




    /**
     * @param name 
     * @param cost 
     * @param finalAura 
     * @param phase 
     * @param effects 
     * @return
     */
    public BuildingCard(String name, int cost, int finalAura, Phases phase, List<IEffect> effects) {
        this.name = name;
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