package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

public class TotemPlacedUpdate implements Serializable {
    private final String nickname;
    private final int offerPosition;

    public TotemPlacedUpdate(String nickname, int offerPosition) {
        this.nickname = nickname;
        this.offerPosition = offerPosition;
    }

    public String getNickname() { return nickname; }
    public int getOfferPosition() { return offerPosition; }
}
