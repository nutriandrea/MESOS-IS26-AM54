package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to voluntarily end the turn.
 */
public final class EndTurnRequest implements Request {

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleEndTurn(playerNickname);
    }
}