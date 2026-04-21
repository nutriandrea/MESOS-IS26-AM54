package it.polimi.ingsw.am54.model;

import java.util.*;


public class Deck<T extends Card> {

    private Stack<T> cards;


    public Deck() {
        this.cards = new Stack<>();
    }
    
   //construttore con stack già presente
    public Deck(Stack<T> stack) {
        this.cards = stack;
    }


    public T drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.pop();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int size() {
        return cards.size();
    }

    // Guarda la prossima carta senza rimuoverla
    public T peek() {
        if (cards.isEmpty()) return null;
        return cards.peek();
    }

}