package model.Model;

/**
 * 
 */
public class OfferingTile {

    /**
     * Default constructor
     */
    public OfferingTile() {
    }

    /**
     * 
     */
    private char id;

    /**
     * 
     */
    private int cardsFromUpper;

    /**
     * 
     */
    private int cardsFromLower;

    /**
     * 
     */
    private int foodBonus;

    /**
     * 
     */
    private Player occupiedBy;


    /**
     * @return
     */
    public boolean isAvailable() {
        return occupiedBy != null;
    }

    /**
     * @param Player p 
     * @return
     */
    public void occupy(Player p) {
        this.occupiedBy = p;
    }

    /**
     * @return
     */
    public int[] getCards() {

        int[] x = new int[0];
        x[0]=this.cardsFromLower;
        x[1]=this.cardsFromUpper;
        return x;

    }

}