package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to pick a card.
 * 100% safe: the server will deduce the sender from the associated Socket.
 */
public final class PickCardRequest implements Request {

    private final String row;
    private final int cardIndex;

    public PickCardRequest(String row, int cardIndex) {
        this.row = row;
        this.cardIndex = cardIndex;
    }

    public String getRow() {
        return row;
    }

    public int getCardIndex() {
        return cardIndex;
    }

    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handlePickCard(playerNickname, this.row, this.cardIndex);
    }
}