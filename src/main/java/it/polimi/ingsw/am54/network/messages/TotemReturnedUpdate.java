package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Messaggio di update inviato dal Server quando un giocatore finisce
 * il suo turno di Pick. Notifica ai Client di togliere il Totem
 * dalla Offering Track e rimetterlo nella Turn Order Track.
 */
public final class TotemReturnedUpdate implements Update {

    private final String nickname;
    private final int position;
    private final int newFoodBalance;
    private final int newAuraBalance;

    public TotemReturnedUpdate(String nickname, int position, int newFoodBalance, int newAuraBalance) {
        this.nickname = nickname;
        this.position = position;
        this.newFoodBalance = newFoodBalance;
        this.newAuraBalance = newAuraBalance;
    }

    public String getNickname() { return nickname; }
    public int getPosition() { return position; }
    public int getNewFoodBalance() { return newFoodBalance; }
    public int getNewAuraBalance() { return newAuraBalance; }

    @Override
    public void applyTo(ClientModel model) {
        model.updateTotemReturned(this.nickname, this.newFoodBalance, this.newAuraBalance);
    }
}