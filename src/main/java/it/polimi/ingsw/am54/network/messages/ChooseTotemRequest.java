package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Richiesta inviata dal Client per scegliere il colore del proprio Totem.
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