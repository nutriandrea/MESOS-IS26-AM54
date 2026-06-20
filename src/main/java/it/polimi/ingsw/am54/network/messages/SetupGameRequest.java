package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to signal readiness for Setup.
 * Being a "Trigger", it does not need parameters: its arrival is enough!
 */
public final class SetupGameRequest implements Request {

    /**
     * Forwards the setup-game request to the controller.
     *
     * @param controller      the server-side game controller
     * @param playerNickname  the nickname of the host starting the game
     * @param connection       the client connection
     */
    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleSetupGame(playerNickname);
    }
}