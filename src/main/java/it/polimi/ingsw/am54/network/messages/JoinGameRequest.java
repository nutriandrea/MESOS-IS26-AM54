package it.polimi.ingsw.am54.network.messages;

/**
 * JoinGameRequest — Primo contatto del client con il server
 */
public class JoinGameRequest implements Request {

    private final String nickname;

    public JoinGameRequest(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() { return nickname; }

    public static final String TYPE = "join_game";
    @Override public String getType() { return TYPE; }
}