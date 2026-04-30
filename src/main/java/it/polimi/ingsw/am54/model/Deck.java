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

    // Guarda la prossima carta senza rimuoverla
    public T peek() {
        if (cards.isEmpty()) return null;
        return cards.peek();
    }

    /**
     * Restituisce tutte le carte presenti nel mazzo.
     * Utilizzato dal PersistenceManager per salvare lo stato dei mazzi.
     * Ritorna una copia per evitare modifiche esterne.
     *
     * @return lista di tutte le carte nel mazzo
     */
    public List<T> getAllCards() {
        return new ArrayList<>(cards);
    }

}