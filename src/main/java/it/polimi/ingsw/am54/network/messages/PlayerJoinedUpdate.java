package it.polimi.ingsw.am54.network.messages;


public class PlayerJoinedUpdate implements Update {

    private final String nickname;

    public PlayerJoinedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public static final String TYPE = "player_joined";
    @Override public String getType() { return TYPE; }
}