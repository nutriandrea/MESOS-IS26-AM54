package model;

import java.io.*;
import java.util.*;


public class Player {

    /**
     * Default constructor
     */
    public Player() {
        this.food = 0;
        this.aura = 0;
        this.magicPoints = 0;
        this.myTribe = new ArrayList<>();
        this.myBuildings = new ArrayList<>();
    }

    
    private String nickname;

    
    private TotemColors totemColor;

    
    private int food;

    
    private int aura;

    private int magicPoints;

    
    private boolean extraPick;

    
    private ArrayList<CharacterCard> myTribe;

    
    private ArrayList<BuildingCard> myBuildings;

    
    private int myBuildingDiscount;

    
    private int myFoodDiscount;


    private int eventCostFood;

    private int eventCostAura;


    public int getEventCostFood() {
        return eventCostFood;
    }
    public int getEventCostAura() {
        return eventCostAura;
    }
    public void setEventCostFood(int eventCost) {
        this.eventCostFood += eventCost;
    }
    public void setEventCostAura(int eventCost) {
        this.eventCostAura += eventCost;
    }
    
    public int getFood() {
        return food;
    }

    
    public int getAuraPoints() {
        return aura;
    }

    
    public String getNickname() {
        return nickname;
    }

    
    public TotemColors getTotemColors() {
        return totemColor;
    }

    
    public int getFoodTokens() {
        return food;
    }

    
    public int getMagicPoints() {
        return magicPoints;
    }

    
    public boolean getExtraPick() {
        return extraPick;
    }

    
    public ArrayList<CharacterCard> getMyTribe() {
        return myTribe;
    }

    
    public ArrayList<BuildingCard> getBuildings() {
        return myBuildings;
    }

    
    public int getMyBuildingDiscount() {
        return myBuildingDiscount;
    }

    
    public int getMyFoodDiscount() {
        return myFoodDiscount;
    }



    //fare attenzione a quando vai sotto di cibo che devi pagare in aura
    public void modifyFood(int amount) {
        if (amount > 0) {
            this.food += amount;
        } else {
            this.food = Math.max(0, this.food - (-amount));
        }
    }


    public void modifyAuraPoints(int amount) {
        this.aura += amount;
    }


    public void modifyMagicPoints(int amount) {
        this.magicPoints += amount;
    }


    public void modifyMyTribe(Card c) {
        if (c instanceof CharacterCard) {
            myTribe.add((CharacterCard) c);
        }
    }


    public void modifyMyBuildingDiscount(int amount) {
        this.myBuildingDiscount += amount;
    }


    public void modifyMyFoodDiscount(int amount) {
        this.myFoodDiscount += amount;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setTotemColor(TotemColors color) {
        this.totemColor = color;
    }

    public void setExtraPick(boolean extraPick) {
        this.extraPick = extraPick;
    }

    public void resetEventAttributes(){
        this.eventCostAura=0;
        this.eventCostFood=0;
    }

}