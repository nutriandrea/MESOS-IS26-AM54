package it.polimi.ingsw.am54.network.messages;

public class JoinGameRequest implements ClientMessage {
    private final String nickname;

    public JoinGameRequest(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String getNickname() {
        return nickname;
    }
}
