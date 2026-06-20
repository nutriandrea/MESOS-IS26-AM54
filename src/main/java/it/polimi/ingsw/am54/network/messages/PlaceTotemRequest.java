package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to place its Totem on an offering.
 * 100% safe: the server will deduce the sender from the associated Socket.
 */
public final class PlaceTotemRequest implements Request {

    private final int offerPosition;

    /**
     * Constructs a place-totem request.
     *
     * @param offerPosition the index of the offering tile to place the totem on
     */
    public PlaceTotemRequest(int offerPosition) {
        this.offerPosition = offerPosition;
    }

    /** @return the offering tile position */
    public int getOfferPosition() {
        return offerPosition;
    }

    /**
     * Forwards the totem placement to the controller.
     *
     * @param controller      the server-side game controller
     * @param playerNickname  the nickname of the placing player
     * @param connection       the client connection
     */
    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handlePlaceTotem(playerNickname, this.offerPosition);
    }
}