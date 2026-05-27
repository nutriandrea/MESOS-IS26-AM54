package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Notifica di cambiamento dello stato del gioco inviata dal server (SERVER -> CLIENT).
 * <p>
 * DISPATCH POLIMORFICO: ogni sottotipo implementa {@link #applyTo(ClientModel)},
 * eliminando switch/if-else nel gestore lato client.
 * Grazie a {@code sealed}, il compilatore garantisce copertura esaustiva.
 * <p>
 * Eccezione: {@link StateChangedUpdate} ha logica aggiuntiva nel gestore
 * ({@code ServerHandler.handleUpdate}) per attivare il cambio scena GUI quando
 * si passa da lobby a partita. La responsabilita' model vs UI resta separata:
 * applyTo() aggiorna il modello, il gestore decide se cambiare scena.
 */
public sealed interface Update extends ClientMessage
        permits StateChangedUpdate, PlayerJoinedUpdate, PlayerDisconnectedUpdate,
                PlayerReconnectedUpdate, TotemPlacedUpdate, TotemColorUpdate,
                TotemReturnedUpdate, CardPickedUpdate, TurnChangedUpdate,
                EndGameUpdate, EventUpdate, ErrorMessageUpdate, LeaderboardUpdate,
                GameSuspendedUpdate, GameResumedUpdate, PingRequest {
    void applyTo(ClientModel model);
}