package it.polimi.ingsw.am54.model;

import java.util.*;


/**
 * Mazzo di carte generico (LIFO), basato su uno {@link Stack}. Usato per il mazzo
 * Tribù (personaggi + eventi) e per il mazzo Edifici.
 *
 * @param <T> il tipo di carta contenuta nel mazzo
 */
public class Deck<T extends Card> {

    private Stack<T> cards;


    public Deck() {
        this.cards = new Stack<>();
    }

   //construttore con stack già presente
    public Deck(Stack<T> stack) {
        this.cards = stack;
    }


    /**
     * Pesca la carta in cima al mazzo rimuovendola.
     *
     * @return la carta pescata, oppure {@code null} se il mazzo è vuoto
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
     * Guarda la carta in cima al mazzo senza rimuoverla (es. per rilevare il
     * passaggio di Era confrontandone l'era).
     *
     * @return la prossima carta, oppure {@code null} se il mazzo è vuoto
     */
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
