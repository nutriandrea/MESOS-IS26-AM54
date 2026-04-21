package it.polimi.ingsw.am54.network.messages;


public class TotemPlacedUpdate implements Update {

    private final String nickname;
    private final int offerPosition;

    public TotemPlacedUpdate(String nickname, int offerPosition) {
        this.nickname = nickname;
        this.offerPosition = offerPosition;
    }

    public String getNickname() { return nickname; }
    public int getOfferPosition() { return offerPosition; }
}