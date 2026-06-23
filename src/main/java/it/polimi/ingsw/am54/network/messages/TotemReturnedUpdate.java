package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Update message sent by the Server when a player ends
 * their Pick turn. Notifies Clients to remove the Totem
 * from the Offering Track and put it back in the Turn Order Track.
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