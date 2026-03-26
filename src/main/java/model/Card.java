package model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class Card {

    /**
     * Default constructor
     */
    public Card(int era) {
        this.era = era;
    }

    private int era;

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

    public void pick(Player player, Game game) throws Exception {
        throw new UnsupportedOperationException("Cannot pick this card type");
    }

}