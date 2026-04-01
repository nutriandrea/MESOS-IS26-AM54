package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

public class TurnChangedUpdate implements Serializable {
    private final String nickname;

    public TurnChangedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() { return nickname; }
}
