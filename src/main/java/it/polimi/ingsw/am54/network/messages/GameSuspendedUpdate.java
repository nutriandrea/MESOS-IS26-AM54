package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification to all clients that the game has been suspended
 * because only one active player is left.
 * SERVER -> CLIENT
 */
public final class GameSuspendedUpdate implements Update {

    /**
     * Applies this update to the client model, showing the suspension notice.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateGameSuspended();
    }
}
