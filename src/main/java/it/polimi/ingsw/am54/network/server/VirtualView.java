package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.network.messages.*;

import java.util.List;
import java.util.stream.Collectors;

public class VirtualView implements IGameObserver {
    private final ClientConnection connection;

    public VirtualView(ClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public void onPlayerJoined(Game game, Player player) {
        connection.sendUpdate(new PlayerJoinedUpdate(player.getNickname()));
    }

    @Override
    public void onTotemColorChosen(Game game, Player player, TotemColors color) {
        connection.sendUpdate(new TotemColorUpdate(player.getNickname(), color));
    }

    @Override
    public void onTotemPlaced(Game game, Player player, int offerPosition) {
        connection.sendUpdate(new TotemPlacedUpdate(player.getNickname(), offerPosition));
    }

    @Override
    public void onCardPicked(Game game, Player player, String row, int cardIndex) {
        connection.sendUpdate(new CardPickedUpdate(player.getNickname(), row, cardIndex));
    }

    @Override
    public void onTurnChanged(Game game, Player newCurrentPlayer) {
        connection.sendUpdate(new TurnChangedUpdate(newCurrentPlayer.getNickname()));
    }

    @Override
    public void onStateChanged(Game game, GameState newState) {
        connection.sendUpdate(new StateChangedUpdate(newState.getClass().getSimpleName()));
    }

    @Override
    public void onEndGame(Game game, List<Player> winners) {
        List<String> winnerNames = winners.stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());
        connection.sendUpdate(new EndGameUpdate(winnerNames));
    }
}