package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.server.ClientConnection;

/**
 * Richiesta inviata dal giocatore al server (CLIENT -> SERVER).
 * <p>
 * DISPATCH POLIMORFICO: ogni sottotipo implementa {@link #execute},
 * eliminando la necessita' di switch/if-else sul tipo nel loop di lettura.
 * Il compilatore, grazie a {@code sealed}, garantisce che tutti i casi siano coperti.
 * <p>
 * Eccezione: {@link PongResponse} e {@link JoinGameRequest} sono gestiti come
 * early {@code instanceof} in {@code SocketClientHandler} perche' operano a
 * livello socket (ping) o necessitano di settare il nickname prima del dispatch.
 */
public sealed interface Request extends ClientMessage
        permits JoinGameRequest, SetupGameRequest, ChooseTotemRequest,
                PlaceTotemRequest, PickCardRequest, EndTurnRequest,
                PongResponse, GetLeaderboardRequest {
    void execute(Controller controller, String playerNickname, ClientConnection connection);
}