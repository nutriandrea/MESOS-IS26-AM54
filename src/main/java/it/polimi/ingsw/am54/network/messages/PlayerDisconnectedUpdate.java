package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification to all clients that a player has been marked as disconnected.
 * SERVER -> CLIENT
 */
public final class PlayerDisconnectedUpdate implements Update {
    private final String nickname;

    public PlayerDisconnectedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void applyTo(ClientModel model) {
        model.updatePlayerDisconnected(this.nickname);
    }
}