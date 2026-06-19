package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Notifica broadcast quando un giocatore piazza il proprio totem
 * su una tessera offerta. Il client sposta il giocatore dal
 * turnOrderTrack all'offeringTrack.
 */
public final class TotemPlacedUpdate implements Update {

    private final String nickname;
    private final int offerPosition;

    public TotemPlacedUpdate(String nickname, int offerPosition) {
        this.nickname = nickname;
        this.offerPosition = offerPosition;
    }

    public String getNickname() { return nickname; }
    public int getOfferPosition() { return offerPosition; }

    @Override
    public void applyTo(ClientModel model) {
        model.updateTotemPlacement(this.nickname, this.offerPosition);
    }
}