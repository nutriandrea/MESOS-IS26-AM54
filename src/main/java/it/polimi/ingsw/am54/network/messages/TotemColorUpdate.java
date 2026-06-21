package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification sent by the server when a player chooses their totem color.
 * SERVER -> CLIENT
 */
public final class TotemColorUpdate implements Update {

    private final String nickname;
    private final TotemColors color;

    /**
     * Constructs a totem color notification.
     *
     * @param nickname the player's nickname
     * @param color    the chosen totem color
     */
    public TotemColorUpdate(String nickname, TotemColors color) {
        this.nickname = nickname;
        this.color = color;
    }

    /**
     * Returns the nickname of the player who chose the color.
     *
     * @return the player's nickname
     */
    public String getNickname() { return nickname; }

    /**
     * Returns the totem color chosen by the player.
     *
     * @return the chosen totem color
     */
    public TotemColors getColor() { return color; }

    /**
     * Applies this update to the client model, recording the color choice.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateTotemColor(this.nickname, this.color);
    }
}
