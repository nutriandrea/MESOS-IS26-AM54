package it.polimi.ingsw.am54.network.messages;

/**
 * Richiesta inviata dal Client per piazzare il proprio Totem su un'offerta.
 * Sicura al 100%: il server dedurrà il mittente dal Socket associato.
 */
public class PlaceTotemRequest implements Request {

    // Rimosso il nickname per sicurezza e rimosso @Override!
    private final int offerPosition;

    public PlaceTotemRequest(int offerPosition) {
        this.offerPosition = offerPosition;
    }

    public int getOfferPosition() {
        return offerPosition;
    }

    public static final String TYPE = "place_totem";
    @Override public String getType() { return TYPE; }
}