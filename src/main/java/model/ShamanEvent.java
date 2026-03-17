package model;

import java.io.*;
import java.util.*;


public class ShamanEvent extends EventCard {

    private boolean finalEvent;
    /**
     * Constructor
     */
    public ShamanEvent(int era, boolean finalEvent) {
        super(era);
        this.finalEvent = finalEvent;
    }

    /**
     */
    public void resolveEvent(Game g) {
        // TODO implement here
    }

}