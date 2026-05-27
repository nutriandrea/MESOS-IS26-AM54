package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Richiesta inviata dal Client per segnalare di essere pronto per il Setup.
 * Essendo un "Trigger", non ha bisogno di parametri: basta il suo arrivo!
 */
public final class SetupGameRequest implements Request {

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleSetupGame(playerNickname);
    }
}