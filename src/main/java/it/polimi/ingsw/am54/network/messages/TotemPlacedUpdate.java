package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification when a player places their totem
 * on an offering tile. The client moves the player from
 * turnOrderTrack to the offeringTrack.
 */
public final class TotemPlacedUpdate implements Update {

    private final String nickname;
    private final int offerPosition;

    /**
     * Constructs a totem-placed notification.
     *
     * @param nickname      the player who placed the totem
     * @param offerPosition the offering tile index
     */
    public TotemPlacedUpdate(String nickname, int offerPosition) {
        this.nickname = nickname;
        this.offerPosition = offerPosition;
    }

    /** @return the placing player's nickname */
    public String getNickname() { return nickname; }
    /** @return the offering tile position */
    public int getOfferPosition() { return offerPosition; }

    /**
     * Applies this update to the client model, moving the totem to the offering track.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateTotemPlacement(this.nickname, this.offerPosition);
    }
}