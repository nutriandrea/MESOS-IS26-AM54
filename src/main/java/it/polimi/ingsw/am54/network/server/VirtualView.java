package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.network.messages.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Traduce gli eventi del Model di gioco in messaggi di rete (Update)
 * da inviare al Client specifico a cui è associata questa View.
 */
public class VirtualView implements IGameObserver {

    // Il "cameriere" associato a questa specifica vista
    private final ClientHandler clientHandler;

    public VirtualView(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    // --- METODI SCATENATI IN BROADCAST DAL MODEL ---

    @Override
    public void onPlayerJoined(Game game, Player player) {
        clientHandler.sendMessage(new PlayerJoinedUpdate(player.getNickname()));
    }

    @Override
    public void onTotemColorChosen(Game game, Player player, TotemColors color) {
        clientHandler.sendMessage(new TotemColorUpdate(player.getNickname(), color));
    }

    @Override
    public void onTotemPlaced(Game game, Player player, int offerPosition) {
        clientHandler.sendMessage(new TotemPlacedUpdate(player.getNickname(), offerPosition));
    }

    @Override
    public void onCardPicked(Game game, Player player, String row, int cardIndex) {
        clientHandler.sendMessage(new CardPickedUpdate(player.getNickname(), row, cardIndex));
    }

    @Override
    public void onTurnChanged(Game game, Player newCurrentPlayer) {
        clientHandler.sendMessage(new TurnChangedUpdate(newCurrentPlayer.getNickname()));
    }

    @Override
    public void onStateChanged(Game game, GameState newState) {
        // Ottimo l'uso del getSimpleName() per non mandare oggetti pesanti!
        clientHandler.sendMessage(new StateChangedUpdate(newState.getClass().getSimpleName()));
    }

    @Override
    public void onEndGame(Game game, List<Player> winners) {
        // Estrazione pulita dei soli nomi (DTO improvvisato e funzionale)
        List<String> winnerNames = winners.stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());
        clientHandler.sendMessage(new EndGameUpdate(winnerNames));
    }

    // --- METODI SCATENATI DIRETTAMENTE DAL CONTROLLER ---

    /**
     * Invia un errore specifico solo a questo client.
     * Verrà chiamato dal Controller quando questo giocatore fa un'azione illegale.
     */
    public void signalError(String message) {
        clientHandler.sendError(message);
    }
}