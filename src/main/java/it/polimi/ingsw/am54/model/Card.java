package it.polimi.ingsw.am54.model;

/**
 * 
 */
public class Card {


    public Card(int era) {
        this.era = era;
    }

    private int era;

    private Player owner;

    public int getEra() {
        return era;
    }


    public void addOwner(Player p) {
        this.owner = p;
    }

    public void pick(Player player, Game game) throws Exception {
        throw new UnsupportedOperationException("Cannot pick this card type");
    }

}