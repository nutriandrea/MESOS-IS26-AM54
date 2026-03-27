package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    private Deck<CharacterCard> deck;

    @BeforeEach
    void setUp() {
        deck = new Deck<>();
    }

    @Test
    void testDeck_EmptyInitially() {
        assertTrue(deck.isEmpty());
    }

    @Test
    void testDeck_DrawCard_EmptyDeck() {
        CharacterCard card = deck.drawCard();
        assertNull(card);
    }

    @Test
    void testDeck_Peek_EmptyDeck() {
        CharacterCard card = deck.peek();
        assertNull(card);
    }

    @Test
    void testDeck_AddAndDraw() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        Stack<CharacterCard> stack = new Stack<>();
        stack.push(card);
        
        deck = new Deck<>(stack);
        
        assertFalse(deck.isEmpty());
        assertEquals(card, deck.peek());
        assertEquals(card, deck.drawCard());
    }

    @Test
    void testDeck_Peek_DoesNotRemove() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        Stack<CharacterCard> stack = new Stack<>();
        stack.push(card);
        
        deck = new Deck<>(stack);
        
        deck.peek();
        assertFalse(deck.isEmpty());
    }

    @Test
    void testDeck_DrawCard_RemovesFromDeck() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        Stack<CharacterCard> stack = new Stack<>();
        stack.push(card);
        
        deck = new Deck<>(stack);
        
        deck.drawCard();
        assertTrue(deck.isEmpty());
    }

    @Test
    void testDeck_MultipleCards() {
        Stack<CharacterCard> stack = new Stack<>();
        for (int i = 0; i < 5; i++) {
            stack.push(new CharacterCard(i, JobType.HUNTER, 1, Phases.EVENT, null, 1));
        }
        
        deck = new Deck<>(stack);
        
        assertFalse(deck.isEmpty());
        assertEquals(4, deck.drawCard().getLevel());
        assertEquals(3, deck.drawCard().getLevel());
        assertEquals(2, deck.drawCard().getLevel());
    }

    @Test
    void testDeck_PeekAfterMultipleDraws() {
        Stack<CharacterCard> stack = new Stack<>();
        for (int i = 0; i < 3; i++) {
            stack.push(new CharacterCard(i, JobType.HUNTER, 1, Phases.EVENT, null, 1));
        }
        
        deck = new Deck<>(stack);
        
        deck.drawCard();
        deck.drawCard();
        
        assertNotNull(deck.peek());
        assertEquals(0, deck.peek().getLevel());
    }
}
