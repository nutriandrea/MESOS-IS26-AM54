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
    public Deck<BuildingCard> createBuildingDeck(String filePath) {
        // TODO: Caricare carte da file e creare Deck<BuildingCard>
        // Per ora, ritorna un Deck vuoto
        return new Deck<>();
    }

    /**
     * @param filePath 
     * @return
     */
    public Deck<TribeCard> createTribeDeck(String charactersFilePath, String eventsFilePath) {
        // TODO: Caricare carte da file e creare Deck<TribeCard>
        // Per ora, ritorna un Deck vuoto
        return new Deck<>();
    }

}