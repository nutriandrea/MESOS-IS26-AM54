package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

public final class TurnChangedUpdate implements Update {

    private final String nickname;

    public TurnChangedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() { return nickname; }

    @Override
    public void applyTo(ClientModel model) {
        model.updateTurn(this.nickname);
    }
}