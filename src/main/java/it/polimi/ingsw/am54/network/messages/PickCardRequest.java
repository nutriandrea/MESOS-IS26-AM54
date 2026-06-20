package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the Client to draw a card.
 * 100% safe: the server will deduce the sender from the associated Socket.
 */
public final class PickCardRequest implements Request {

    private final String row;
    private final int cardIndex;

    /**
     * Constructs a pick-card request.
     *
     * @param row       the row to draw from ("upper" or "lower")
     * @param cardIndex the index of the card within the row
     */
    public PickCardRequest(String row, int cardIndex) {
        this.row = row;
        this.cardIndex = cardIndex;
    }

    /** @return the row identifier ("upper" or "lower") */
    public String getRow() {
        return row;
    }

    /** @return the index of the card in the row */
    public int getCardIndex() {
        return cardIndex;
    }

    /**
     * Forwards the card pick to the controller.
     *
     * @param controller      the server-side game controller
     * @param playerNickname  the nickname of the drawing player
     * @param connection       the client connection
     */
    @Override
    public void execute(Controller controller, String playerNickname, ClientConnection connection) {
        controller.handlePickCard(playerNickname, this.row, this.cardIndex);
    }
}