package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification to all clients that a player has been marked as disconnected.
 * SERVER -> CLIENT
 */
public final class PlayerDisconnectedUpdate implements Update {
    private final String nickname;

    /**
     * Constructs a disconnection notification.
     *
     * @param nickname the nickname of the disconnected player
     */
    public PlayerDisconnectedUpdate(String nickname) {
        this.nickname = nickname;
    }

    /** @return the disconnected player's nickname */
    public String getNickname() {
        return nickname;
    }

    /**
     * Applies this update to the client model, marking the player as disconnected.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updatePlayerDisconnected(this.nickname);
    }
}
