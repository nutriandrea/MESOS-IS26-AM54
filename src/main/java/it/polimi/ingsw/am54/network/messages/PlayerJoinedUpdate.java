package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

public class PlayerJoinedUpdate implements Serializable {
    private final String nickname;

    public PlayerJoinedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}
