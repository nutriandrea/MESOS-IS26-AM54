package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Request sent by the player to the server (CLIENT -> SERVER).
 * <p>
 * POLYMORPHIC DISPATCH: each subtype implements {@link #execute},
 * eliminating the need for switch/if-else on type in the reading loop.
 * The compiler, thanks to {@code sealed}, ensures all cases are covered.
 * <p>
 * Exception: {@link PongResponse} and {@link JoinGameRequest} are handled as
 * early {@code instanceof} in {@code SocketClientHandler} because they operate at
 * socket level (ping) or need to set the nickname before dispatch.
 */
public sealed interface Request extends ClientMessage
        permits JoinGameRequest, SetupGameRequest, ChooseTotemRequest,
                PlaceTotemRequest, PickCardRequest, EndTurnRequest,
                PongResponse, GetLeaderboardRequest {
    /**
     * Executes this request on the server, dispatching it to the controller.
     *
     * @param controller      the server-side game controller
     * @param playerNickname  the nickname of the requesting player
     * @param connection       the client connection used to send responses
     */
    void execute(Controller controller, String playerNickname, ClientConnection connection);
}