package it.polimi.ingsw.am54.network.messages;

public class SetupGameRequest implements ClientMessage {
    private final String nickname;

    public SetupGameRequest(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String getNickname() {
        return nickname;
    }
}
