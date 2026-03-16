package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class Card {

    /**
     * Default constructor
     */
    public Card() {
    }

    /**
     * 
     */
    private int era;

    /**
     * 
     */
    private Player owner;


    /**
     * @return
     */
    public int getEra() {
        return era;
    }

    /**
     * @return
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * @return
     */
    public void addOwner(Player p) {
        this.owner = p;
    }

}