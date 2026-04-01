package it.polimi.ingsw.am54.model;


public interface IPurchasable {

    public boolean canBePurchased(Player player);

    public void buy(Player p);

}