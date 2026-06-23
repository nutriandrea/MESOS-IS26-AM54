package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.network.messages.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * VirtualView — Proxy Pattern between Model and network
 * ============================================================================
 *---
 * ROLE:
 * The Game (Model) knows nothing about the network. It has a list of observers (IGameObserver)
 * and notifies an event by calling a method (e.g. onCardPicked) on each observer.
 *---
 * The VirtualView registers itself as an observer of the Game. When the Game notifies it,
 * the VirtualView translates the call into a network message and sends it
 * on its ClientConnection (Socket or RMI, it does not matter).
 *---
 * HOW THE BROADCAST HAPPENS:
 * Broadcasting is NOT the responsibility of the VirtualView. Every VirtualView has
 * ONLY ONE ClientConnection and sends ONLY to its own client.
 * Broadcasting happens in the Game: when the Game notifies an event, it does so
 * on ALL registered observers (a for-loop). Therefore:
 *---
 * Game.pickCard("Alice") -> notifyCardPicked() ->
 * for (IGameObserver obs : observers) obs.onCardPicked()
 * -> VV_Alice.connection.sendUpdate(CardPickedUpdate("Alice"))
 * -> VV_Bob.connection.sendUpdate(CardPickedUpdate("Alice"))
 * -> VV_Carlo.connection.sendUpdate(CardPickedUpdate("Alice"))
 *---
 * All clients receive the update, everyone sees the updated board.
 *---
 * DIFF vs FULL STATE:
 * The updates are already "diffs" because they contain ONLY the changed data:
 * - CardPickedUpdate: nickname, row, cardIndex, newFoodBalance
 * - TotemPlacedUpdate: nickname, position
 * - TurnChangedUpdate: nickname
 * etc.
 * The entire state of the game is never sent, only the delta.
 *---
 * FULL FLOW:
 * Client Request -> SocketClientHandler/RMIClientHandler -> Controller -> Game
 * |
 * notifyXxx() (for-loop)
 * |
 * +----------------+----------------+
 * |                |                |
 * VirtualView      VirtualView      VirtualView
 * (Alice)          (Bob)          (Carlo)
 * |                |                |
 * v                v                v
 * Update           Update           Update
 * ============================================================================
 */
public class VirtualView implements IGameObserver {

    /** Network channel towards the client associated with this VirtualView */
    private final ClientConnection connection;

    /** Nickname of the player who owns this connection */
    private final String nickname;


    /**
     * Constructor: creates the VirtualView when a player joins the game.
     * It is called by Controller.handleJoinGame() after the connection is created.
     *
     * @param connection the network channel (Socket or RMI) of the player
     * @param nickname the nickname of the player
     */
    public VirtualView(ClientConnection connection, String nickname) {
        this.connection = connection;
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    /**
     * Sends an error message ONLY to this player (e.g. invalid move).
     *
     * @param errorMessage the error text to show to the client
     */
    public void showError(String errorMessage) {
        connection.notifyError(errorMessage);
    }

    /**
     * Notifies this client that another player has disconnected.
     *
     * @param disconnectedNickname the nickname of the disconnected player
     */
    public void showPlayerDisconnected(String disconnectedNickname) {
        connection.notifyPlayerDisconnected(disconnectedNickname);
    }

    /**
     * Notifies this client that another player has reconnected.
     *
     * @param reconnectedNickname the nickname of the reconnected player
     */
    public void showPlayerReconnected(String reconnectedNickname) {
        connection.notifyPlayerReconnected(reconnectedNickname);
    }

    /* =====================================================================
     * --- OBSERVER METHODS ---
     *
     * The Game calls these methods on ALL registered observers.
     * Each VirtualView forwards the Update to its own connection.
     *
     * Example: when Alice picks a card:
     * 1. Game.pickCard("Alice", "upper", 2)
     * 2. Game -> notifyCardPicked(p=Alice, row="upper", index=2, food=5)
     * 3. for (obs : observers) obs.onCardPicked(game, Alice, "upper", 2, 5)
     * 3a. VV_Alice.onCardPicked() -> connection.sendUpdate(CardPickedUpdate)
     * 3b. VV_Bob.onCardPicked()   -> connection.sendUpdate(CardPickedUpdate)
     * 3c. VV_Carlo.onCardPicked() -> connection.sendUpdate(CardPickedUpdate)
     *
     * Result: ALL clients see Alice picking the card and their
     * local board is updated accordingly.
     * ===================================================================== */

    /**
     * Notifies the client that a new player has entered the lobby.
     *
     * @param game   the current game
     * @param player the newly joined player
     */
    @Override
    public void onPlayerJoined(Game game, Player player) {
        connection.notifyPlayerJoined(player.getNickname());
    }

    /**
     * Notifies the client of the totem color chosen by a player.
     *
     * @param game   the current game
     * @param player the player who chose
     * @param color  the selected color
     */
    @Override
    public void onTotemColorChosen(Game game, Player player, TotemColors color) {
        connection.notifyTotemColor(player.getNickname(), color);
    }

    /**
     * Notifies the client that a player has placed the totem on an offering tile.
     *
     * @param game          the current game
     * @param player        the player who placed the totem
     * @param offerPosition the index of the chosen tile
     */
    @Override
    public void onTotemPlaced(Game game, Player player, int offerPosition) {
        connection.notifyTotemPlaced(player.getNickname(), offerPosition);}

    private Map<Integer, String> generateOfferingSnapshot(Board b) {
        Map<Integer, String> offering = new LinkedHashMap<>();
        for (int i = 0; i < b.getOfferingTrack().size(); i++) {
            OfferingTile tile = b.getOfferingTrack().get(i);
            String occName = (tile.getOccupiedBy() != null) ? tile.getOccupiedBy().getNickname() : "VUOTO";

            // If the tile is empty we show the total (cardsFromUpper), if it's occupied we show the remaining (remainingUpperPicks)
            int up = tile.getRemainingUpperPicks();
            int low = tile.getRemainingLowerPicks();

            offering.put(i, "[Tessera " + tile.getId() + " | Pick Su:" + up + " Giù:" + low + " | Bonus 🍖:+" + tile.getFoodBonus() + "] -> " + occName);
        }
        return offering;
    }

    /**
     * Notifies the client that a player has picked a card, attaching the updated snapshot
     * of the offering track (residual picks per tile).
     *
     * @param game            the current game
     * @param player          the player who picked
     * @param row             the card row ("upper" or "lower")
     * @param cardIndex       the index of the picked card
     * @param newFoodBalance  the player's new food balance
     * @param newMagicBalance the player's new magic balance
     */
    @Override
    public void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {
        Map<Integer, String> currentOffering = generateOfferingSnapshot(game.getBoard());

        connection.notifyCardPicked(player.getNickname(), row, cardIndex, newFoodBalance, newMagicBalance, currentOffering);
    }

    /**
     * Notifies the client that a player has finished picking and their totem returns
     * to the turn order, collecting any food bonus.
     *
     * @param game            the current game
     * @param player          the player ending their turn
     * @param position        the return position (slot) in the turn order
     * @param newFoodBalance  the new food balance
     * @param newAuraBalance  the new aura balance
     */
    @Override
    public void onTotemReturned(Game game, Player player, int position, int newFoodBalance, int newAuraBalance) {
        connection.notifyTotemReturned(player.getNickname(), position, newFoodBalance, newAuraBalance);
    }

    /**
     * Notifies the client of the resolution of an Event card, sending a complete
     * snapshot (statistics of all players and content of all board rows
     * serialized into strings).
     *
     * @param game          the current game
     * @param resolvedEvent the event card just resolved
     */
    @Override
    public void onEventResolved(Game game, EventCard resolvedEvent) {
        Board b = game.getBoard();

        // 0. Retrieve the true name of the event!
        String eventName = resolvedEvent.getName();

        // 1. Mapping player statistics
        Map<String, EventUpdate.PlayerSnapshot> stats = new HashMap<>();
        for (Player p : game.getPlayers()) {
            List<String> tribeStr = p.getMyTribe().stream().map(Card::toClientString).collect(Collectors.toList());
            List<String> buildStr = p.getBuildings().stream().map(Card::toClientString).collect(Collectors.toList());
            stats.put(p.getNickname(), new EventUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints(), tribeStr, buildStr
            ));
        }

        // 2. Transformation of cards into Strings
        List<String> charUp = b.getCharacterUpperRow().stream().map(Card::toClientString).collect(Collectors.toList());
        List<String> charLow = b.getCharacterLowerRow().stream().map(Card::toClientString).collect(Collectors.toList());
        List<String> eventUp = b.getEventUpperRow().stream().map(Card::toClientString).collect(Collectors.toList());
        List<String> eventLow = b.getEventLowerRow().stream().map(Card::toClientString).collect(Collectors.toList());
        List<String> buildUp = b.getBuildingCardsUpperRow().stream().map(Card::toClientString).collect(Collectors.toList());
        List<String> buildLow = b.getBuildingCardsLowerRow().stream().map(Card::toClientString).collect(Collectors.toList());

        // 3. Send the complete Snapshot adding the name at the top!
        connection.notifyEventResolved(eventName, stats, charUp, charLow, eventUp, eventLow, buildUp, buildLow);
    }
    /**
     * Notifies the client of the new turn player (can be {@code null} between
     * phases, when no turn is active).
     *
     * @param game             the current game
     * @param newCurrentPlayer the new turn player, or {@code null}
     */
    @Override
    public void onTurnChanged(Game game, Player newCurrentPlayer) {
        // If newCurrentPlayer is null (between phases), sends null.
        String turnOwner = (newCurrentPlayer != null) ? newCurrentPlayer.getNickname() : null;
        connection.notifyTurnChanged(turnOwner);
    }

    /**
     * Notifies the client of a phase change by sending the COMPLETE snapshot of the state
     * (unlike other, differential updates): statistics of all players, turn order,
     * offering track and the six rows of cards. Sent at every state transition
     * and upon reconnection.
     *
     * @param game     the current game
     * @param newState the new game state
     */
    @Override
    public void onStateChanged(Game game, GameState newState) {
        // 1. Player Statistics and Cards (always valid)
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new java.util.LinkedHashMap<>();
        for (Player p : game.getPlayers()) {
            List<String> tribeStr = p.getMyTribe().stream().map(Card::toClientString).collect(Collectors.toList());
            List<String> buildStr = p.getBuildings().stream().map(Card::toClientString).collect(Collectors.toList());
            stats.put(p.getNickname(), new StateChangedUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints(), p.getTotemColors(), tribeStr, buildStr
            ));
        }

        // Let's initialize the Board structures as empty
        List<String> turnOrder = new java.util.ArrayList<>();
        Map<Integer, String> offering = new java.util.LinkedHashMap<>();
        List<String> charUp = new java.util.ArrayList<>();
        List<String> charLow = new java.util.ArrayList<>();
        List<String> eventUp = new java.util.ArrayList<>();
        List<String> eventLow = new java.util.ArrayList<>();
        List<String> buildUp = new java.util.ArrayList<>();
        List<String> buildLow = new java.util.ArrayList<>();

        Board b = game.getBoard();
        if (b != null) {
            turnOrder = b.getTurnOrderTile().stream()
                    .map(slot -> {
                        int bonus = slot.getBonusFood();
                        String foodStr = (bonus > 0 ? "+" : "") + bonus;
                        String auraWarning = (bonus < 0) ? " (-2 ✨ se non hai cibo)" : "";
                        String occName = (slot.getOccupiedBy() != null) ? slot.getOccupiedBy().getNickname() : "VUOTO";
                        return "[Slot " + slot.getPosition() + " | Cibo: " + foodStr + auraWarning + "] -> " + occName;
                    })
                    .collect(Collectors.toList());

            for (int i = 0; i < b.getOfferingTrack().size(); i++) {
                OfferingTile tile = b.getOfferingTrack().get(i);
                String occName = (tile.getOccupiedBy() != null) ? tile.getOccupiedBy().getNickname() : "VUOTO";
                offering.put(i, "[Tessera " + tile.getId() + " | Pick Su:" + tile.getRemainingUpperPicks() + " Giù:" + tile.getRemainingLowerPicks() + " | Bonus 🍖:+" + tile.getFoodBonus() + "] -> " + occName);
            }

            charUp = b.getCharacterUpperRow().stream().map(Card::toClientString).collect(Collectors.toList());
            charLow = b.getCharacterLowerRow().stream().map(Card::toClientString).collect(Collectors.toList());
            eventUp = b.getEventUpperRow().stream().map(Card::toClientString).collect(Collectors.toList());
            eventLow = b.getEventLowerRow().stream().map(Card::toClientString).collect(Collectors.toList());
            buildUp = b.getBuildingCardsUpperRow().stream().map(Card::toClientString).collect(Collectors.toList());
            buildLow = b.getBuildingCardsLowerRow().stream().map(Card::toClientString).collect(Collectors.toList());
        }

        // Send the packet (safely!)
        connection.notifyStateChanged(
                newState.getClass().getSimpleName(), game.getCurrentEra(), game.getRoundCount(),
                stats, turnOrder, offering,
                charUp, charLow, eventUp, eventLow, buildUp, buildLow
        );
    }

    /**
     * Notifies the client of the end of the game. Calculates here, server-side, the real
     * final score of each player and sends winners, scores and the number of players.
     *
     * @param game    the concluded game
     * @param winners the winning players
     */
    @Override
    public void onEndGame(Game game, List<Player> winners) {
        List<String> winnerNames = winners.stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());

        // We calculate the real scores map here on the Server
        java.util.Map<String, Integer> finalScores = new java.util.HashMap<>();
        for (Player p : game.getPlayers()) {
            finalScores.put(p.getNickname(), game.calculatePlayerScore(p));
        }

        // We send the complete data to the connection
        connection.notifyEndGame(winnerNames, finalScores, game.getPlayers().size());
    }

    /**
     * Notifies this client that the game has been suspended (only one active
     * player remaining). Invoked by the Controller via broadcast.
     */
    public void showGameSuspended() {
        connection.notifyGameSuspended();
    }

    /**
     * Notifies this client that the game has resumed (at least two active players).
     * Invoked by the Controller via broadcast.
     */
    public void showGameResumed() {
        connection.notifyGameResumed();
    }
}