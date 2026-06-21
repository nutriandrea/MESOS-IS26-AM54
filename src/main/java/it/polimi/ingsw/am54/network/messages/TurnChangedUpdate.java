package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification sent by the server when the active player changes.
 * SERVER -> CLIENT
 */
public final class TurnChangedUpdate implements Update {

    private final String nickname;

    /**
     * Constructs a turn-changed notification.
     *
     * @param nickname the nickname of the new current player
     */
    public TurnChangedUpdate(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Returns the nickname of the player whose turn is now active.
     *
     * @return the current player's nickname
     */
    public String getNickname() { return nickname; }

    /**
     * Applies this update to the client model, setting the current player.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateTurn(this.nickname);
    }
}
