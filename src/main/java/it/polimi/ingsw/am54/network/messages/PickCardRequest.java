package it.polimi.ingsw.am54.network.messages;

public class PickCardRequest implements ClientMessage {
    private final String nickname;
    private final String row;
    private final int cardIndex;

    public PickCardRequest(String nickname, String row, int cardIndex) {
        this.nickname = nickname;
        this.row = row;
        this.cardIndex = cardIndex;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    public String getRow() {
        return row;
    }

    public int getCardIndex() {
        return cardIndex;
    }
}
