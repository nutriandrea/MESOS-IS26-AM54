package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

public class TotemColorUpdate implements Serializable {
    private final String nickname;
    private final String color;

    public TotemColorUpdate(String nickname, String color) {
        this.nickname = nickname;
        this.color = color;
    }

    public String getNickname() { return nickname; }
    public String getColor() { return color; }
}
