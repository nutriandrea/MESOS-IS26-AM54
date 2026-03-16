package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class Board {

    /**
     * TEST GIT constructor
     */
    public Board() {
    }

    /**
     * 
     */
    private List<TribeCard> upperRow;

    /**
     * 
     */
    private List<TribeCard> lowerRow;

    /**
     * 
     */
    private List<BuildingCard> upperRowBuildings;

    /**
     * 
     */
    private List<BuildingCard> lowerRowBuildings;

    /**
     * 
     */
    private List<OfferingTile> offeringTrack;

    /**
     * 
     */
    private List<TurnOrderSlot> turnOrderTile;





    /**
     * @param int numPlayers 
     * @return
     */
    public void refillUpperRow(int numPlayers) {
        // TODO implement here
    }

    /**
     * @return
     */
    public void moveUpperToLower() {
        // TODO implement here
    }

    /**
     * @param int numPlayers 
     * @return
     */
    public void generateBoard(int numPlayers) {
        // TODO implement here
    }

    /**
     * @return
     */
    public void moveBuildings() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void removeCard() {
        // TODO implement here
    }

    /**
     * @return
     */
    public List<Card> getRow() {
        // TODO implement here
        return null;
    }

}