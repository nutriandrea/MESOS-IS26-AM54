package it.polimi.ingsw.am54.model;

import java.util.*;


/**
 * Generic deck of cards (LIFO), based on a {@link Stack}. Used for the Tribe deck
 * (characters + events) and for the Buildings deck.
 *
 * @param <T> the type of card contained in the deck
 */
public class Deck<T extends Card> {

    private Stack<T> cards;


    public Deck() {
        this.cards = new Stack<>();
    }

    //constructor with an already existing stack
    public Deck(Stack<T> stack) {
        this.cards = stack;
    }


    /**
     * Draws the card at the top of the deck, removing it.
     *
     * @return the drawn card, or {@code null} if the deck is empty
     */
    public T drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.pop();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Looks at the card at the top of the deck without removing it (e.g., to detect the
     * passage of Era by comparing its era).
     *
     * @return the next card, or {@code null} if the deck is empty
     */
    public T peek() {
        if (cards.isEmpty()) return null;
        return cards.peek();
    }

    /**
     * Returns all the cards present in the deck.
     * Used by the PersistenceManager to save the state of the decks.
     * Returns a copy to avoid external modifications.
     *
     * @return list of all cards in the deck
     */
    public List<T> getAllCards() {
        return new ArrayList<>(cards);
    }

}