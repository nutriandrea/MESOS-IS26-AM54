package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Update message sent by the Server when a player finishes
 * their Pick turn. Notifies Clients to remove the Totem
 * from the Offering Track and put it back in the Turn Order Track.
 */
public final class TotemReturnedUpdate implements Update {

    private final String nickname;
    private final int position;
    private final int newFoodBalance;
    private final int newAuraBalance;

    /**
     * Constructs a totem-returned notification.
     *
     * @param nickname        the player whose totem was returned
     * @param position        the offering tile position the totem occupied
     * @param newFoodBalance  the player's updated food balance
     * @param newAuraBalance  the player's updated aura balance
     */
    public TotemReturnedUpdate(String nickname, int position, int newFoodBalance, int newAuraBalance) {
        this.nickname = nickname;
        this.position = position;
        this.newFoodBalance = newFoodBalance;
        this.newAuraBalance = newAuraBalance;
    }

    /** @return the player's nickname */
    public String getNickname() { return nickname; }
    /** @return the offering tile position */
    public int getPosition() { return position; }
    /** @return the updated food balance */
    public int getNewFoodBalance() { return newFoodBalance; }
    /** @return the updated aura balance */
    public int getNewAuraBalance() { return newAuraBalance; }

    /**
     * Applies this update to the client model, returning the totem to the turn order.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateTotemReturned(this.nickname, this.position, this.newFoodBalance, this.newAuraBalance);
    }
}