package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Richiesta inviata dal Client per piazzare il proprio Totem su un'offerta.
 * Sicura al 100%: il server dedurrà il mittente dal Socket associato.
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