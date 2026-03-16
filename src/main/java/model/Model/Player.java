package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class Player {

    /**
     * Default constructor
     */
    public Player() {
        this.food = 0;
        this.prestige = 0;
        this.myTribe = new ArrayList<>();
        this.myBuildings = new ArrayList<>();
    }

    /**
     * 
     */
    private String nickname;

    /**
     * 
     */
    private TotemColors totemColor;

    /**
     * 
     */
    private int food;

    /**
     * 
     */
    private int prestige;

    /**
     * 
     */
    private boolean extraPick;

    /**
     * 
     */
    private ArrayList<CharacterCard> myTribe;

    /**
     * 
     */
    private ArrayList<BuildingCard> myBuildings;

    /**
     * 
     */
    private int myBuildingDiscount;

    /**
     * 
     */
    private int myFoodDiscount;




    /**
     * @return
     */
    public int getFood() {
        return food;
    }

    /**
     * @return
     */
    public int getAuraPoints() {
        return prestige;
    }

    /**
     * @return
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * @return
     */
    public TotemColors getTotemColors() {
        return totemColor;
    }

    /**
     * @return
     */
    public int getFoodTokens() {
        return food;
    }

    /**
     * @return
     */
    public int getMagicPoints() {
        return prestige;
    }

    /**
     * @return
     */
    public boolean getExtraPick() {
        return extraPick;
    }

    /**
     * @return
     */
    public ArrayList<CharacterCard> getMyTribe() {
        return myTribe;
    }

    /**
     * @return
     */
    public ArrayList<BuildingCard> getBuildings() {
        return myBuildings;
    }

    /**
     * @return
     */
    public int getMyBuildingDiscount() {
        return myBuildingDiscount;
    }

    /**
     * @return
     */
    public int getMyFoodDiscount() {
        return myFoodDiscount;
    }

    /**
     * @param int amout 
     * @return
     */
    public void modifyFood(int amount) {
        if (amount > 0) {
            this.food += amount;
        } else {
            this.food = Math.max(0, this.food - (-amount));
        }
    }

    /**
     * @param int amount 
     * @return
     */
    public void modifyAuraPoints(int amount) {
        this.prestige += amount;
    }

    /**
     * @param int amount 
     * @return
     */
    public void modifyMagicPoints(int amount) {
        this.prestige += amount;
    }

    /**
     * @param Card c 
     * @return
     */
    public void modifyMyTribe(Card c) {
        if (c instanceof CharacterCard) {
            myTribe.add((CharacterCard) c);
        }
    }

    /**
     * @param int amount 
     * @return
     */
    public void modifyMyBuildingDiscount(int amount) {
        this.myBuildingDiscount += amount;
    }

    /**
     * @param int amount 
     * @return
     */
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

}