package model.Model;

import java.io.*;
import java.util.*;


public class TurnOrderSlot {

    /**
     * Default constructor
     */
    public TurnOrderSlot() {
    }

    
    private int position;

    
    private int bonusFood;

    
    private Player occupiedBy;


    /**
     * @param Player p 
     * @return
     */
    public void occupy(Player p) {
        this.occupiedBy = p;
    }

    
    public boolean isAvailable() {
        return this.occupiedBy != null;
    }

    
    public int getPosition() {
        return this.position;
    }

}