package it.polimi.ingsw.am54.network.messages;

public class CardPickedUpdate implements Update {
    private final String nickname;
    private final String row;
    private final int cardIndex;
    private final int newFoodBalance;
    private final java.util.Map<Integer, String> updatedOfferingTrack;

    public CardPickedUpdate(String nickname, String row, int cardIndex, int newFoodBalance,  java.util.Map<Integer, String> updatedOfferingTrack) {
        this.nickname = nickname;
        this.row = row;
        this.cardIndex = cardIndex;
        this.newFoodBalance = newFoodBalance;
        this.updatedOfferingTrack = updatedOfferingTrack;
    }

    public String getNickname() { return nickname; }
    public String getRow() { return row; }
    public int getCardIndex() { return cardIndex; }
    public int getNewFoodBalance() { return newFoodBalance; }
    public java.util.Map<Integer, String> getUpdatedOfferingTrack() { return updatedOfferingTrack; }
}
