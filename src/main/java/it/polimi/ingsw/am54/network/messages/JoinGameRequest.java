package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * JoinGameRequest — Primo contatto del client con il server
 */
public final class JoinGameRequest implements Request {

    private final String nickname;

    public JoinGameRequest(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() { return nickname; }

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleJoinGame(this.nickname, connection);
    }
}