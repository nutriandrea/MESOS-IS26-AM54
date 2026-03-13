package model.Model;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class DeckFactory {

    /**
     * Default constructor
     */
    public DeckFactory() {
    }

    /**
     * 
     */
    private Map<String, IEffect> effectRegistry;

    /**
     * @param filePath 
     * @return
     */
    public Stack<BuildingCard> createBuildingDeck(String filePath) {
        // TODO implement here
        return null;
    }

    /**
     * @param filePath 
     * @return
     */
    public Stack<TribeCard> createTribeDeck(String filePath) {
        // TODO implement here
        return null;
    }

}