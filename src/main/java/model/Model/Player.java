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
    private int foodTokens;

    /**
     * 
     */
    private int auraPoints;

    /**
     * 
     */
    private int magicPoints;

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
        // TODO implement here
        return 0;
    }

    /**
     * @return
     */
    public int getAuraPoints() {
        // TODO implement here
        return 0;
    }

    /**
     * @return
     */
    public String getNickname() {
        // TODO implement here
        return "";
    }

    /**
     * @return
     */
    public TotemColors getTotemColors() {
        // TODO implement here
        return null;
    }

    /**
     * @return
     */
    public int getFoodTokens() {
        // TODO implement here
        return 0;
    }

    /**
     * @return
     */
    public int getMagicPoints() {
        // TODO implement here
        return 0;
    }

    /**
     * @return
     */
    public boolean getExtraPick() {
        // TODO implement here
        return false;
    }

    /**
     * @return
     */
    public ArrayList<CharacterCard> getMyTribe() {
        // TODO implement here
        return null;
    }

    /**
     * @return
     */
    public ArrayList<BuildingCard> getBuildings() {
        // TODO implement here
        return null;
    }

    /**
     * @return
     */
    public int getMyBuildingDiscount() {
        // TODO implement here
        return 0;
    }

    /**
     * @return
     */
    public int getMyFoodDiscount() {
        // TODO implement here
        return 0;
    }

    /**
     * @param int amout 
     * @return
     */
    public void modifyFood(int amount) {
        // TODO implement here
    }

    /**
     * @param int amount 
     * @return
     */
    public void modifyAuraPoints(int amount) {
        // TODO implement here
    }

    /**
     * @param int amount 
     * @return
     */
    public void modifyMagicPoints(int amount) {
        // TODO implement here
    }

    /**
     * @param Card c 
     * @return
     */
    public void modifyMyTribe(Card c) {
        // TODO implement here
    }

    /**
     * @param int amount 
     * @return
     */
    public void modifyMyBuildingDiscount(int amount) {
        // TODO implement here
    }

    /**
     * @param int amount 
     * @return
     */
    public void modifyMyFoodDiscount(int amount) {
        // TODO implement here
    }

}