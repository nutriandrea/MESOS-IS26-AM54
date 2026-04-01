package model;

import it.polimi.ingsw.am54.model.BuildingCard;
import it.polimi.ingsw.am54.model.Deck;
import it.polimi.ingsw.am54.model.DeckFactory;
import it.polimi.ingsw.am54.model.TribeCard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeckFactoryTest {

    @Test
    void testCreateBuildingDeck_InvalidPlayerCount_LessThanTwo() {
        Deck<BuildingCard> deck = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", 1);
        assertNotNull(deck);
        assertTrue(deck.isEmpty());
    }

    @Test
    void testCreateBuildingDeck_InvalidPlayerCount_MoreThanFive() {
        Deck<BuildingCard> deck = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", 6);
        assertNotNull(deck);
        assertTrue(deck.isEmpty());
    }

    @Test
    void testCreateTribeDeck_InvalidPlayerCount_LessThanTwo() {
        Deck<TribeCard> deck = DeckFactory.createTribeDeck(
            "data/CharacterCardsSetup.json", 
            "data/EventCardsSetup.json", 
            1
        );
        assertNotNull(deck);
        assertTrue(deck.isEmpty());
    }

    @Test
    void testCreateTribeDeck_InvalidPlayerCount_MoreThanFive() {
        Deck<TribeCard> deck = DeckFactory.createTribeDeck(
            "data/CharacterCardsSetup.json", 
            "data/EventCardsSetup.json", 
            6
        );
        assertNotNull(deck);
        assertTrue(deck.isEmpty());
    }

    @Test
    void testCreateBuildingDeck_ValidInput() {
        Deck<BuildingCard> deck = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", 2);
        assertNotNull(deck);
    }

    @Test
    void testCreateTribeDeck_ValidInput() {
        Deck<TribeCard> deck = DeckFactory.createTribeDeck(
            "data/CharacterCardsSetup.json", 
            "data/EventCardsSetup.json", 
            2
        );
        assertNotNull(deck);
    }

    @Test
    void testCreateBuildingDeck_InvalidFilePath() {
        Deck<BuildingCard> deck = DeckFactory.createBuildingDeck("invalid/path.json", 2);
        assertNotNull(deck);
    }

    @Test
    void testCreateTribeDeck_InvalidFilePath() {
        Deck<TribeCard> deck = DeckFactory.createTribeDeck(
            "invalid/char.json", 
            "invalid/event.json", 
            2
        );
        assertNotNull(deck);
    }
}
