package model.Model;

import java.io.*;
import java.util.*;


public class CharacterCard extends TribeCard {

    /**
     * Default constructor
     */
    public CharacterCard() {
    }

    
    private int level;

    
    private JobType type;

    
    private int aura;

    
    private IEffect effect;






    /**
     * @param level 
     * @param type 
     * @param effects 
     * @return
     */
    public CharacterCard(int level, JobType type, List<IEffect> effects) {
        this.level = level;
        this.type = type;
        // this.effects = effects; // if effects field is List<IEffect>
    }

}