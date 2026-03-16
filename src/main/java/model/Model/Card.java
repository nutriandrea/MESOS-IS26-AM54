package model.Model;

import java.io.*;
import java.util.*;


public class Card {

    /**
     * Default constructor
     */
    public Card() {
    }

    
    private int era;

    
    private Player owner;


    
    public int getEra() {
        return era;
    }

    
    public Player getOwner() {
        return owner;
    }

    
    public void addOwner(Player p) {
        this.owner = p;
    }

}