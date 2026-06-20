package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * JoinGameRequest — First contact of the client with the server
 */
public final class JoinGameRequest implements Request {

    private final String nickname;

    /**
     * Constructs a join-game request for the given nickname.
     *
     * @param nickname the nickname the client wants to use
     */
    public JoinGameRequest(String nickname) {
        this.nickname = nickname;
    }

    /** @return the requested nickname */
    public String getNickname() { return nickname; }

    /**
     * Forwards the join request to the controller.
     *
     * @param controller      the server-side game controller
     * @param playerNickname  unused (the nickname is carried by this request)
     * @param connection       the client connection
     */
    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleJoinGame(this.nickname, connection);
    }
}