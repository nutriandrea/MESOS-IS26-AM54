package it.polimi.ingsw.am54.network.messages;

/**
 * Richiesta inviata dal Client per pescare una carta.
 * Sicura al 100%: il server dedurrà il mittente dal Socket associato.
 */
public class PickCardRequest implements Request {

    // Niente nickname!
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
}