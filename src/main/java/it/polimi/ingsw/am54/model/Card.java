package it.polimi.ingsw.am54.model;

public class Card {


    public Card(int era) {
        this.era = era;
    }

    private int era;

    private Player owner;

    public int getEra() {
        return era;
    }

    /**
     * Imposta l'era della carta.
     * Utilizzato dal EventCard.getName() per generare il nome della carta.
     *
     * @param era l'era della carta (1-3)
     */
    public void setEra(int era) {
        this.era = era;
    }

    public void addOwner(Player p) {
        this.owner = p;
    }

    public void pick(Player player, Game game) throws Exception {
        throw new UnsupportedOperationException("Cannot pick this card type");
    }

    public boolean canBePurchased(Player player) {
        return true;
    }

    public void reveal(Board board, String row) {
        throw new UnsupportedOperationException("Cannot reveal this generic card type");
    }

}