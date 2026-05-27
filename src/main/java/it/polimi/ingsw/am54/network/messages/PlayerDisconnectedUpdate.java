package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Notifica broadcast a tutti i client che un giocatore e' stato marcato come disconnesso.
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
