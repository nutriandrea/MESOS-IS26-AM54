package it.polimi.ingsw.am54.model;

public class OfferingTile {

    private char id;
    private int cardsFromUpper;
    private int cardsFromLower;
    private int foodBonus;
    private Player occupiedBy;

    private int remainingUpperPicks;
    private int remainingLowerPicks;

    public OfferingTile(char id, int cardsFromUpper, int cardsFromLower, int foodBonus) {
        this.id = id;
        this.cardsFromUpper = cardsFromUpper;
        this.cardsFromLower = cardsFromLower;
        this.foodBonus = foodBonus;
        this.occupiedBy = null; // default (non inzializzo occupato)
        this.remainingUpperPicks = cardsFromUpper;
        this.remainingLowerPicks = cardsFromLower;
    }


    public char getId() {
        return id;
    }

    /**
     * Restituisce il numero di carte che possono essere prese dalla fila superiore.
     * Utilizzato dal PersistenceManager per salvare lo stato della tessera.
     *
     * @return numero di carte dalla fila superiore
     */
    public int getCardsFromUpper() {
        return cardsFromUpper;
    }

    /**
     * Restituisce il numero di carte che possono essere prese dalla fila inferiore.
     * Utilizzato dal PersistenceManager per salvare lo stato della tessera.
     *
     * @return numero di carte dalla fila inferiore
     */
    public int getCardsFromLower() {
        return cardsFromLower;
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
        this.remainingUpperPicks = this.cardsFromUpper;
        this.remainingLowerPicks = this.cardsFromLower;
    }

    //sembra essere bad practice usare il occupy ma con null come riferimento
    public void freeSlot() {
        this.occupiedBy = null;
    }
    public void resetPicks() {
        this.remainingUpperPicks = this.cardsFromUpper;
        this.remainingLowerPicks = this.cardsFromLower;
    }

    public boolean useUpperPick() {
        if (remainingUpperPicks > 0) {
            remainingUpperPicks--;
            return true;
        }
        return false;
    }

    public boolean useLowerPick() {
        if (remainingLowerPicks > 0) {
            remainingLowerPicks--;
            return true;
        }
        return false;
    }

    public int getRemainingUpperPicks() {
        return remainingUpperPicks;
    }

    public int getRemainingLowerPicks() {
        return remainingLowerPicks;
    }

}