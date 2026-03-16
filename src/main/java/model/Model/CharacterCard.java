package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class CharacterCard extends TribeCard {

    private int level;

    private JobType type;

    private int aura;

    private Phases phase;

    private List<IEffect> effects;


    /**
     *
     */
    public CharacterCard(int level, JobType type, int aura, Phases phase, List<IEffect> effects, int era) {
        super(era);
        this.level = level;
        this.type = type;
        this.aura = aura;
        this.phase = phase;
        this.effects = effects;
    }

}