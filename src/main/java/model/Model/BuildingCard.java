package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class BuildingCard extends Card {

    /**
     * Default constructor
     */
    public BuildingCard() {
    }

    /**
     * 
     */
    private String name;

    /**
     * 
     */
    private int cost;

    /**
     * 
     */
    private int finalAura;

    /**
     * 
     */
    private Phases phase;

    /**
     * 
     */
    private IEffects effect;

    /**
     * 
     */
    private JobType eventType;




    /**
     * @param name 
     * @param cost 
     * @param finalAura 
     * @param phase 
     * @param effects 
     * @return
     */
    public void buildingCard(String name, int cost, int finalAura, Phases phase, List<IEffect> effects) {
        // TODO implement here
        return null;
    }

    /**
     * @return
     */
    public int getFinalAura() {
        // TODO implement here
        return 0;
    }

}