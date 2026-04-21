package it.polimi.ingsw.am54.network.messages;


public class TurnChangedUpdate implements Update {

    private final String nickname;

    public TurnChangedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() { return nickname; }
}