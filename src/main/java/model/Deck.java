package model;

import java.io.*;
import java.util.*;


public class Deck<T extends Card> {

    /**
     * Default constructor
     */
    public Deck() {
        this.cards = new Stack<>();
    }
    
    /**
     * Constructor with existing stack
     */
    public Deck(Stack<T> stack) {
        this.cards = stack;
    }

    
    private Stack<T> cards;



    
    public T drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.pop();
    }

    
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    // Guarda la prossima carta senza rimuoverla
    public T peek() {
        if (cards.isEmpty()) return null;
        return cards.peek();
    }

}