package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to choose their Totem color.
 */
public final class ChooseTotemRequest implements Request {

    private final TotemColors color;

    /**
     * Constructs a choose-totem request.
     *
     * @param color the totem color chosen by the player
     */
    public ChooseTotemRequest(TotemColors color) {
        this.color = color;
    }

    /** @return the chosen totem color */
    public TotemColors getColor() {
        return color;
    }

    /**
     * Forwards the color choice to the controller.
     *
     * @param controller      the server-side game controller
     * @param playerNickname  the nickname of the choosing player
     * @param connection       the client connection
     */
    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handleChooseTotemColor(playerNickname, this.color.name());
    }
}