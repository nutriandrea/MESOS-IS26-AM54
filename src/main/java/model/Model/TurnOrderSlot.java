package model.Model;

import java.io.*;
import java.util.*;

public class TurnOrderSlot {

    private int position;
    private int bonusFood;
    private Player occupiedBy;

    public TurnOrderSlot() {
    }

    /**
     * Inizializza lo slot assegnando la posizione e pescando il bonus di cibo
     * direttamente da una matrice, in base al numero di giocatori.
     */
    public void createTurnOrderSlot(int position, int numPlayers) {
        this.position = position;

        // Controllo di sicurezza
        if (numPlayers < 2 || numPlayers > 5) return;

        // Matrice dei bonus cibo: [numero_giocatori][posizione - 1]
        // Segue la tua logica: primo +2, secondo +1, ultimo -1, intermedi 0.
        int[][] foodBonuses = {
                {},                          // 0 giocatori (non usato)
                {},                          // 1 giocatore (non usato)
                {1, -1},                     // 2 giocatori (1° e 2°)
                {2, 0, -1},                  // 3 giocatori (1°, 2°, 3°)
                {2, 1, 0, -1},               // 4 giocatori
                {3, 1, 0, 0, -1}             // 5 giocatori
        };

        // position parte da 1, quindi sottraiamo 1 per avere l'indice corretto dell'array (da 0)
        this.bonusFood = foodBonuses[numPlayers][position - 1];
    }

    public void occupy(Player p) {
        this.occupiedBy = p;
    }

    public boolean isAvailable() {
        return this.occupiedBy == null;
    }

    public int getPosition() {
        return this.position;
    }

    public int getBonusFood() {
        return this.bonusFood;
    }

    public Player getOccupiedBy() {
        return this.occupiedBy;
    }
}