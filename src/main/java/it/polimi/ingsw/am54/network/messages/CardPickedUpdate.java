package it.polimi.ingsw.am54.network.messages;

public class CardPickedUpdate implements Update {
    private final String nickname;
    private final String row;
    private final int cardIndex;
    private final int newFoodBalance;

    public CardPickedUpdate(String nickname, String row, int cardIndex, int newFoodBalance) {
        this.nickname = nickname;
        this.row = row;
        this.cardIndex = cardIndex;
        this.newFoodBalance = newFoodBalance;
    }

    public String getNickname() { return nickname; }
    public String getRow() { return row; }
    public int getCardIndex() { return cardIndex; }
    public int getNewFoodBalance() { return newFoodBalance; }
}
