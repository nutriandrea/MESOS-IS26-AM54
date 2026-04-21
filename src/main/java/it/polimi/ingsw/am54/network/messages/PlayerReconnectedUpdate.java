package it.polimi.ingsw.am54.network.messages;

public class PlayerReconnectedUpdate implements Update {
    private final String nickname;

    public PlayerReconnectedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}
