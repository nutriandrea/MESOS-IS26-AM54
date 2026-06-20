package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification to all clients that a player has reconnected.
 * SERVER -> CLIENT
 */
public final class PlayerReconnectedUpdate implements Update {
    private final String nickname;

    /**
     * Constructs a reconnection notification.
     *
     * @param nickname the nickname of the reconnected player
     */
    public PlayerReconnectedUpdate(String nickname) {
        this.nickname = nickname;
    }

    /** @return the reconnected player's nickname */
    public String getNickname() {
        return nickname;
    }

    /**
     * Applies this update to the client model, marking the player as reconnected.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updatePlayerReconnected(this.nickname);
    }
}
