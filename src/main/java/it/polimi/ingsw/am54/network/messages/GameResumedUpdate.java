package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification to all clients that the game has resumed
 * after a second player has reconnected.
 * SERVER -> CLIENT
 */
public final class GameResumedUpdate implements Update {

    @Override
    public void applyTo(ClientModel model) {
        model.updateGameResumed();
    }
}