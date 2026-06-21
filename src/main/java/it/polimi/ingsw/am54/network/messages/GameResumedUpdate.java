package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification to all clients that the game has been resumed
 * after a second player has reconnected.
 * SERVER -> CLIENT
 */
public final class GameResumedUpdate implements Update {

    /**
     * Applies this update to the client model, showing the resume notice.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateGameResumed();
    }
}
