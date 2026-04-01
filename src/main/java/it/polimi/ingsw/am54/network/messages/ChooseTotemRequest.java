package it.polimi.ingsw.am54.network.messages;

public class ChooseTotemRequest implements ClientMessage {
    private final String nickname;
    private final String color;

    public ChooseTotemRequest(String nickname, String color) {
        this.nickname = nickname;
        this.color = color;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    public String getColor() {
        return color;
    }
}
