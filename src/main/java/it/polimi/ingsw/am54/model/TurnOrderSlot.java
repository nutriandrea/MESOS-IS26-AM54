package it.polimi.ingsw.am54.model;

public class TurnOrderSlot {

    private int position;
    private int bonusFood;
    private Player occupiedBy;

    public TurnOrderSlot(int position, int bonusFood) {
        this.position = position;
        this.bonusFood = bonusFood;
    }

    public void occupy(Player p) {
        this.occupiedBy = p;
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

    public void freeSlot() {
        this.occupiedBy = null;
    }
}