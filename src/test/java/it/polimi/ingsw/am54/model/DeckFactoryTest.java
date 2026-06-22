package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test della factory dei mazzi: generazione dei mazzi Tribù (personaggi + eventi)
 * e Edifici dai file JSON, con il corretto ordinamento per Era.
 */
public class DeckFactoryTest {

    private static final String CHARS = "/data/CharacterCardsSetup.json";
    private static final String EVENTS = "/data/EventCardsSetup.json";
    private static final String BUILDINGS = "/data/BuildingCardsSetup.json";

    @Test
    public void testCreateTribeDeckHasCards() {
        Deck<TribeCard> deck = DeckFactory.createTribeDeck(CHARS, EVENTS, 3);
        assertNotNull(deck);
        assertFalse(deck.isEmpty(), "Il mazzo tribù deve contenere carte");

        // La prima carta pescabile deve essere di Era 1 (le ere basse sono in cima)
        TribeCard top = deck.peek();
        assertNotNull(top);
        assertEquals(1, top.getEra(), "Le carte di Era 1 devono stare in cima al mazzo");
    }

    @Test
    public void testCreateBuildingDeckHasCards() {
        Deck<BuildingCard> deck = DeckFactory.createBuildingDeck(BUILDINGS, 4);
        assertNotNull(deck);
        assertFalse(deck.isEmpty(), "Il mazzo edifici deve contenere carte");
        assertEquals(1, deck.peek().getEra(), "L'Era 1 deve essere in cima");
    }

    @Test
    public void testTribeDeckCardsAreDrawableAndOrderedByEra() {
        Deck<TribeCard> deck = DeckFactory.createTribeDeck(CHARS, EVENTS, 2);
        int lastEra = 0;
        int drawn = 0;
        TribeCard c;
        while ((c = deck.drawCard()) != null) {
            assertTrue(c.getEra() >= lastEra,
                    "Pescando dall'alto le ere non devono mai diminuire");
            lastEra = c.getEra();
            drawn++;
        }
        assertTrue(drawn > 0);
        assertTrue(lastEra >= 1);
    }

    @Test
    public void testInvalidPlayerCountThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> DeckFactory.createTribeDeck(CHARS, EVENTS, 1));
        assertThrows(IllegalArgumentException.class,
                () -> DeckFactory.createTribeDeck(CHARS, EVENTS, 6));
        assertThrows(IllegalArgumentException.class,
                () -> DeckFactory.createBuildingDeck(BUILDINGS, 1));
        assertThrows(IllegalArgumentException.class,
                () -> DeckFactory.createBuildingDeck(BUILDINGS, 6));
    }
}
