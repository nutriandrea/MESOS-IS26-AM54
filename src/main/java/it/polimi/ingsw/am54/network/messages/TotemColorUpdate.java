package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.model.TotemColors; // Ricordati l'import!

// Per ora mettiamo ClientMessage (poi diventerà Update)
public class TotemColorUpdate implements Update {

    private final String nickname;
    private final TotemColors color;

    public TotemColorUpdate(String nickname, TotemColors color) {
        this.nickname = nickname;
        this.color = color;
    }

    public String getNickname() { return nickname; }
    public TotemColors getColor() { return color; }
}