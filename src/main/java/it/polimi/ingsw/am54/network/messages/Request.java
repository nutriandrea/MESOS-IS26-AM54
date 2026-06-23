package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the player to the server (CLIENT -> SERVER).
 * <p>
 * POLYMORPHIC DISPATCH: each subtype implements {@link #execute},
 * eliminating the need for switch/if-else on the type in the read loop.
 * The compiler, thanks to {@code sealed}, guarantees that all cases are covered.
 * <p>
 * Exception: {@link PongResponse} and {@link JoinGameRequest} are handled as
 * early {@code instanceof} in {@code SocketClientHandler} because they operate at the
 * socket level (ping) or need to set the nickname before dispatch.
 */
public sealed interface Request extends ClientMessage
        permits JoinGameRequest, SetupGameRequest, ChooseTotemRequest,
        PlaceTotemRequest, PickCardRequest, EndTurnRequest,
        PongResponse, GetLeaderboardRequest {
    void execute(Controller controller, String playerNickname, ClientConnection connection);
}