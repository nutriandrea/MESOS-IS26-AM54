package model;

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
        this.remainingUpperPicks = this.cardsFromUpper;
        this.remainingLowerPicks = this.cardsFromLower;
    }

    //sembra essere bad practice usare il occupy ma con null come riferimento
    public void freeSlot() {
        this.occupiedBy = null;
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

    public boolean hasPicksLeft() {
        return remainingUpperPicks > 0 || remainingLowerPicks > 0;
    }

    public int getRemainingUpperPicks() {
        return remainingUpperPicks;
    }

    public int getRemainingLowerPicks() {
        return remainingLowerPicks;
    }

}