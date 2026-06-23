package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to choose the color of their Totem.
 */
public final class ChooseTotemRequest implements Request {

    private final TotemColors color;

    public ChooseTotemRequest(TotemColors color) {
        this.color = color;
    }

    public TotemColors getColor() {
        return color;
    }

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleChooseTotemColor(playerNickname, this.color.name());
    }
}