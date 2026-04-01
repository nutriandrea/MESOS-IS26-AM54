package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

public class CardPickedUpdate implements Serializable {
    private final String nickname;
    private final String row;
    private final int cardIndex;

    public CardPickedUpdate(String nickname, String row, int cardIndex) {
        this.nickname = nickname;
        this.row = row;
        this.cardIndex = cardIndex;
    }

    public String getNickname() { return nickname; }
    public String getRow() { return row; }
    public int getCardIndex() { return cardIndex; }
}
