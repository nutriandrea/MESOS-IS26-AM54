package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Notifica broadcast a tutti i client che un giocatore si e' riconnesso.
 * SERVER -> CLIENT
 */
public final class PlayerReconnectedUpdate implements Update {
    private final String nickname;

    public PlayerReconnectedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void applyTo(ClientModel model) {
        model.updatePlayerReconnected(this.nickname);
    }
}
