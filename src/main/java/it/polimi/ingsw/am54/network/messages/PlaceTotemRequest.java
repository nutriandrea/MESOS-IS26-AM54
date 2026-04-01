package it.polimi.ingsw.am54.network.messages;

public class PlaceTotemRequest implements ClientMessage {
    private final String nickname;
    private final int offerPosition;

    public PlaceTotemRequest(String nickname, int offerPosition) {
        this.nickname = nickname;
        this.offerPosition = offerPosition;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    public int getOfferPosition() {
        return offerPosition;
    }
}
