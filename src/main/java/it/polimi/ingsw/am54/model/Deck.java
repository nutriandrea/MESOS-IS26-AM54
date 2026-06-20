package it.polimi.ingsw.am54.model;

import java.util.*;


/**
 * Generic deck of cards, with support for drawing and inspection.
 * @author AM54
 */
public class Deck<T extends Card> {

    private Stack<T> cards;


    /**
     * Constructs an empty deck.
     */
    public Deck() {
        this.cards = new Stack<>();
    }

    /**
     * Constructs a deck wrapping an existing stack of cards.
     *
     * @param stack the stack of cards to use as the deck contents
     */
    public Deck(Stack<T> stack) {
        this.cards = stack;
    }


    /**
     * Draws and removes the top card from the deck.
     *
     * @return the top card, or {@code null} if the deck is empty
     */
    public T drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.pop();
    }

    /**
     * Checks whether the deck is empty.
     *
     * @return {@code true} if the deck has no cards left
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Looks at the top card without removing it from the deck.
     *
     * @return the top card, or {@code null} if the deck is empty
     */
    public T peek() {
        if (cards.isEmpty()) return null;
        return cards.peek();
    }

    /**
     * Returns all cards in the deck.
     * Used by PersistenceManager to save the deck state.
     * Returns a copy to prevent external modifications.
     *
     * @return list of all cards in the deck
     */
    public List<T> getAllCards() {
        return new ArrayList<>(cards);
    }

}