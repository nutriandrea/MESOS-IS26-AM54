package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.network.messages.*;
import java.util.ArrayList;
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
    public void onPlayerReconnected(Game game, Player player) {
        clientHandler.sendMessage(new PlayerReconnectedUpdate(player.getNickname()));
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
        clientHandler.sendMessage(new StateChangedUpdate(newState.getClass().getSimpleName()));
        System.out.println("[VirtualView] Stato cambiato a: " + newState.getClass().getSimpleName());

        if (newState instanceof it.polimi.ingsw.am54.model.states.StartTurnState) {
            System.out.println("[VirtualView] Invio stato della scacchiera...");
            sendBoardState(game);
        }
    }

    @Override
    public void onEndGame(Game game, List<Player> winners) {
        List<String> winnerNames = winners.stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());
        clientHandler.sendMessage(new EndGameUpdate(winnerNames));
    }

    public void sendBoardState(Game game) {
        try {
            sendBoardStateInternal(game);
        } catch (Exception e) {
            System.err.println("[VirtualView] Errore nell'invio stato: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendBoardStateInternal(Game game) {
        Board board = game.getBoard();

        List<String> playerNames = game.getPlayers().stream()
                .map(Player::getNickname).collect(Collectors.toList());
        List<String> playerColors = game.getPlayers().stream()
                .map(p -> p.getTotemColors() != null ? p.getTotemColors().name() : "NONE")
                .collect(Collectors.toList());

        List<Integer> offeringPositions = new ArrayList<>();
        List<Integer> offeringFood = new ArrayList<>();
        List<Boolean> offeringAvailable = new ArrayList<>();
        List<String> offeringOccupiedBy = new ArrayList<>();

        for (OfferingTile tile : board.getOfferingTrack()) {
            offeringPositions.add((int) tile.getId());
            offeringFood.add(tile.getFoodBonus());
            offeringAvailable.add(tile.isAvailable());
            offeringOccupiedBy.add(tile.getOccupiedBy() != null ? tile.getOccupiedBy().getNickname() : "");
        }

        List<String> turnOrderNicknames = new ArrayList<>();
        List<Integer> turnOrderFood = new ArrayList<>();
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            turnOrderNicknames.add(slot.getOccupiedBy() != null ? slot.getOccupiedBy().getNickname() : "");
            turnOrderFood.add(slot.getBonusFood());
        }

        List<String> upperRowCards = new ArrayList<>();
        for (Card c : board.getRow("upper")) {
            upperRowCards.add(getCardName(c));
        }
        List<String> lowerRowCards = new ArrayList<>();
        for (Card c : board.getRow("lower")) {
            lowerRowCards.add(getCardName(c));
        }

        List<String> upperBuildings = new ArrayList<>();
        for (BuildingCard b : board.getBuildingCardsUpperRow()) {
            upperBuildings.add(b.getName());
        }
        List<String> lowerBuildings = new ArrayList<>();
        for (BuildingCard b : board.getBuildingCardsLowerRow()) {
            lowerBuildings.add(b.getName());
        }

        String currentPlayer = game.getCurrentPlayer() != null ? game.getCurrentPlayer().getNickname() : "";

        BoardStateUpdate update = new BoardStateUpdate(
                game.getCurrentEra(), game.getRoundCount(), currentPlayer,
                playerNames, playerColors,
                offeringPositions, offeringFood, offeringAvailable, offeringOccupiedBy,
                turnOrderNicknames, turnOrderFood,
                upperRowCards, lowerRowCards,
                upperBuildings, lowerBuildings
        );

        clientHandler.sendMessage(update);
    }

    private String getCardName(Card c) {
        if (c instanceof TribeCard tc) {
            return "Card#" + tc.getEra();
        } else if (c instanceof BuildingCard bc) {
            return bc.getName();
        }
        return "Unknown";
    }

    private int getCardEra(Card c) {
        return c != null ? c.getEra() : 0;
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