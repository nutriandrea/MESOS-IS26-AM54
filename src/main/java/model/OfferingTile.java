package model;

import java.io.*;

public class OfferingTile {

    private char id;
    private int cardsFromUpper;
    private int cardsFromLower;
    private int foodBonus;
    private Player occupiedBy;

    public OfferingTile(char id, int cardsFromUpper, int cardsFromLower, int foodBonus) {
        this.id = id;
        this.cardsFromUpper = cardsFromUpper;
        this.cardsFromLower = cardsFromLower;
        this.foodBonus = foodBonus;
        this.occupiedBy = null; // default (non inzializzo occupato)
    }


    public char getId() {
        return id;
    }

    public int getFoodBonus() {
        return foodBonus;
    }

    public Player getOccupiedBy() {
        return occupiedBy;
    }

    public boolean isAvailable() {
        return occupiedBy == null;
    }

    public void occupy(Player p) {
        this.occupiedBy = p;
    }

    //sembra essere bad practice usare il occupy ma con null come riferimento
    public void freeSlot() {
        this.occupiedBy = null;
    }

    /**
     * Restituisce le carte che il giocatore può prendere.
     * Indice 0: carte dalla fila inferiore
     * Indice 1: carte dalla fila superiore
     */
    public int[] getCards() {
        // CORRETTO: l'array deve avere dimensione 2 per contenere due elementi
        int[] x = new int[2];
        x[0] = this.cardsFromLower;
        x[1] = this.cardsFromUpper;
        return x;
    }

}