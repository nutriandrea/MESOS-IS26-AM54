package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to place its own Totem on an offering.
 * 100% safe: the server will deduce the sender from the associated Socket.
 */
public final class PlaceTotemRequest implements Request {

    private final int offerPosition;

    public PlaceTotemRequest(int offerPosition) {
        this.offerPosition = offerPosition;
    }

    public int getOfferPosition() {
        return offerPosition;
    }

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handlePlaceTotem(playerNickname, this.offerPosition);
    }
}