package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class SustenanceEvent extends EventCard {

    private boolean finalEvent;
    /**
     */
    public SustenanceEvent(int era, boolean finalEvent) {
        super(era);
        this.finalEvent = finalEvent;
    }

    /**
     */
    public void resolveEvent(Game g) {
        // TODO implement here
    }
}