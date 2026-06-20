package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification sent when a player draws a card.
 * Contains the row (upper/lower), the index and the new balances
 * of the player's food and magic.
 */
public final class CardPickedUpdate implements Update {
    private final String nickname;
    private final String row;
    private final int cardIndex;
    private final int newFoodBalance;
    private final int newMagicBalance;
    private final java.util.Map<Integer, String> updatedOfferingTrack;

    /**
     * Constructs a card-picked notification.
     *
     * @param nickname              the player who drew the card
     * @param row                   the row drawn from ("upper" or "lower")
     * @param cardIndex             the index of the drawn card
     * @param newFoodBalance        the player's updated food balance
     * @param newMagicBalance       the player's updated magic balance
     * @param updatedOfferingTrack  the full offering track snapshot after the pick
     */
    public CardPickedUpdate(String nickname, String row, int cardIndex, int newFoodBalance, int newMagicBalance, java.util.Map<Integer, String> updatedOfferingTrack) {
        this.nickname = nickname;
        this.row = row;
        this.cardIndex = cardIndex;
        this.newFoodBalance = newFoodBalance;
        this.newMagicBalance = newMagicBalance;
        this.updatedOfferingTrack = updatedOfferingTrack;
    }

    /** @return the drawing player's nickname */
    public String getNickname() { return nickname; }
    /** @return the row drawn from */
    public String getRow() { return row; }
    /** @return the index of the drawn card */
    public int getCardIndex() { return cardIndex; }
    /** @return the updated food balance */
    public int getNewFoodBalance() { return newFoodBalance; }
    /** @return the updated magic balance */
    public int getNewMagicBalance() { return newMagicBalance; }
    /** @return the full offering track snapshot */
    public java.util.Map<Integer, String> getUpdatedOfferingTrack() { return updatedOfferingTrack; }

    /**
     * Applies this update to the client model, removing the card and updating balances.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateCardPicked(this.nickname, this.row, this.cardIndex,
                this.newFoodBalance, this.newMagicBalance, this.updatedOfferingTrack);
    }
}
