package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Game state change notification sent by the server (SERVER -> CLIENT).
 * <p>
 * POLYMORPHIC DISPATCH: each subtype implements {@link #applyTo(ClientModel)},
 * eliminating switch/if-else in the client-side handler.
 * Thanks to {@code sealed}, the compiler guarantees exhaustive coverage.
 * <p>
 * Exception: {@link StateChangedUpdate} has additional logic in the handler
 * ({@code ServerHandler.handleUpdate}) to trigger the GUI scene change when
 * transitioning from lobby to game. Model vs UI responsibility remains separate:
 * applyTo() updates the model, the handler decides whether to change scenes.
 */
public sealed interface Update extends ClientMessage
        permits StateChangedUpdate, PlayerJoinedUpdate, PlayerDisconnectedUpdate,
                PlayerReconnectedUpdate, TotemPlacedUpdate, TotemColorUpdate,
                TotemReturnedUpdate, CardPickedUpdate, TurnChangedUpdate,
                EndGameUpdate, EventUpdate, ErrorMessageUpdate, LeaderboardUpdate,
                GameSuspendedUpdate, GameResumedUpdate, PingRequest {
    /**
     * Applies this update to the given client model, mutating its state.
     *
     * @param model the client-side model to update
     */
    void applyTo(ClientModel model);
}