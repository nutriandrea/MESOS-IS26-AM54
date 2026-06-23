package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to signal they are ready for Setup.
 * Being a "Trigger", it needs no parameters: its arrival is enough!
 */
public final class SetupGameRequest implements Request {

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleSetupGame(playerNickname);
    }
}