package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to voluntarily end the turn.
 */
public final class EndTurnRequest implements Request {

    /**
     * Forwards the end-turn request to the controller.
     *
     * @param controller      the server-side game controller
     * @param playerNickname  the nickname of the player ending the turn
     * @param connection       the client connection
     */
    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleEndTurn(playerNickname);
    }
}
