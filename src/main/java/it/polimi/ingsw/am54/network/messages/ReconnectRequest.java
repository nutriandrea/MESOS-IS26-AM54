package it.polimi.ingsw.am54.network.messages;

public class ReconnectRequest implements Request {
    private final String nickname;

    public ReconnectRequest(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}
