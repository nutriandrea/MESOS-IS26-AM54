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
 * ROLE::
 * The Game (Model) knows nothing about the network. It has a list of observers (IGameObserver)
 * notifies an event by calling a method (e.g. onCardPicked) on each observer.
 *---
 * VirtualView registers as a Game observer. When Game notifies it,
 * VirtualView translates the call into a network message and sends it
 * on its ClientConnection (Socket or RMI, doesn't matter).
 *---
 * HOW BROADCAST WORKS:
 * Broadcast is NOT VirtualView's responsibility. Each VirtualView has
 * ONLY ONE ClientConnection and sends ONLY to its own client.
 * Broadcast happens in Game: when Game notifies an event, it does it
 * on ALL registered observers (a for-loop). So:
 *---
 *   Game.pickCard("Alice") -> notifyCardPicked() ->
 *     for (IGameObserver obs : observers) obs.onCardPicked()
 *       -> VV_Alice.connection.sendUpdate(CardPickedUpdate("Alice"))
 *       -> VV_Bob.connection.sendUpdate(CardPickedUpdate("Alice"))
 *       -> VV_Carlo.connection.sendUpdate(CardPickedUpdate("Alice"))
 *---
 * All clients receive the update, all see the updated board.
 *---
 * DIFF vs FULL STATE:
 * Updates are already "diff" because they contain ONLY the changed data:
 *   - CardPickedUpdate: nickname, row, cardIndex, newFoodBalance
 *   - TotemPlacedUpdate: nickname, position
 *   - TurnChangedUpdate: nickname
 *   etc.
 * The entire game state is never sent, only the delta.
 *---
 * FULL FLOW:
 *   Client Request -> SocketClientHandler/RMIClientHandler -> Controller -> Game
 *                                                                             |
 *                                                            notifyXxx() (for-loop)
 *                                                                             |
 *                                                  +----------------+----------------+
 *                                                  |                |                |
 *                                              VirtualView      VirtualView      VirtualView
 *                                                 (Alice)          (Bob)          (Carlo)
 *                                                  |                |                |
 *                                                  v                v                v
 *                                               Update           Update           Update
 * ============================================================================
 */
public class VirtualView implements IGameObserver {

    /** Network channel to the client associated with this VirtualView */
    private final ClientConnection connection;

    /** Nickname of the player owning this connection */
    private final String nickname;


    /**
     * Constructor: creates the VirtualView when a player enters the game.
     * Called by Controller.handleJoinGame() after connection creation.
     *
     * @param connection the player's network channel (Socket or RMI)
     * @param nickname the player's nickname
     */
    public VirtualView(ClientConnection connection, String nickname) {
        this.connection = connection;
        this.nickname = nickname;
    }

    /** @return the nickname of the player owning this view */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sends an error message ONLY to this player.
     * Used by Controller.signalError() when a player makes an invalid move.
     */
    public void showError(String errorMessage) {
        connection.notifyError(errorMessage);
    }

    /**
     * Notifies this view that another player has disconnected.
     */
    public void showPlayerDisconnected(String disconnectedNickname) {
        connection.notifyPlayerDisconnected(disconnectedNickname);
    }

    /**
     * Notifies this view that another player has reconnected.
     */
    public void showPlayerReconnected(String reconnectedNickname) {
        connection.notifyPlayerReconnected(reconnectedNickname);
    }

    /* =====================================================================
     * --- OBSERVER METHODS ---
     *
     * Game calls these methods on ALL registered observers.
     * Each VirtualView forwards the Update to its own connection.
     *
     * Example: when Alice draws a card:
     *   1. Game.pickCard("Alice", "upper", 2)
     *   2. Game -> notifyCardPicked(p=Alice, row="upper", index=2, food=5)
     *   3. for (obs : observers) obs.onCardPicked(game, Alice, "upper", 2, 5)
     *      3a. VV_Alice.onCardPicked() -> connection.sendUpdate(CardPickedUpdate)
     *      3b. VV_Bob.onCardPicked()   -> connection.sendUpdate(CardPickedUpdate)
     *      3c. VV_Carlo.onCardPicked() -> connection.sendUpdate(CardPickedUpdate)
     *
     * Result: ALL clients see Alice draw the card and their
     * local board is updated accordingly.
     * ===================================================================== */

    /** {@inheritDoc} */
    @Override
    public void onPlayerJoined(Game game, Player player) {
        // All players see that a new player has joined
        connection.notifyPlayerJoined(player.getNickname());
    }

    /** {@inheritDoc} */
    @Override
    public void onTotemColorChosen(Game game, Player player, TotemColors color) {
        // All see the color chosen by the player
        connection.notifyTotemColor(player.getNickname(), color);
    }

    /** {@inheritDoc} */
    @Override
    public void onTotemPlaced(Game game, Player player, int offerPosition) {
        // All see the totem placed on an offering tile
        // The client will remove the player from turnOrderTrack and put them in offeringTrack
        connection.notifyTotemPlaced(player.getNickname(), offerPosition);}

    private Map<Integer, String> generateOfferingSnapshot(Board b) {
        Map<Integer, String> offering = new LinkedHashMap<>();
        for (int i = 0; i < b.getOfferingTrack().size(); i++) {
            OfferingTile tile = b.getOfferingTrack().get(i);
            String occName = (tile.getOccupiedBy() != null) ? tile.getOccupiedBy().getNickname() : "VUOTO";

            // If the tile is empty show the total (cardsFromUpper), if occupied show the remaining (remainingUpperPicks)
            int up = tile.getRemainingUpperPicks();
            int low = tile.getRemainingLowerPicks();

            offering.put(i, "[Tessera " + tile.getId() + " | Pick Su:" + up + " Giù:" + low + " | Bonus 🍖:+" + tile.getFoodBonus() + "] -> " + occName);
        }
        return offering;
    }

    /** {@inheritDoc} */
    @Override
    public void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {
        // All see the drawn card. The client:
        //   - Removes the card from the correct row (upper/lower)
        //   - Adds the card to the player's tribe
        //   - Updates the player's food
        //   - Puts the player back in turnOrderTrack
        Map<Integer, String> currentOffering = generateOfferingSnapshot(game.getBoard());

        connection.notifyCardPicked(player.getNickname(), row, cardIndex, newFoodBalance, newMagicBalance, currentOffering);
   }

    /** {@inheritDoc} */
    @Override
    public void onTotemReturned(Game game, Player player, int position, int newFoodBalance, int newAuraBalance) {
        connection.notifyTotemReturned(player.getNickname(), position, newFoodBalance, newAuraBalance);
    }

    /** {@inheritDoc} */
    @Override
    public void onEventResolved(Game game, EventCard resolvedEvent) {
        Board b = game.getBoard();

        // 0. Get the real event name!
        String eventName = resolvedEvent.getName();

        // 1. Map player statistics
        Map<String, EventUpdate.PlayerSnapshot> stats = new HashMap<>();
        for (Player p : game.getPlayers()) {
            List<String> tribeStr = p.getMyTribe().stream().map(Card::toClientString).collect(Collectors.toList());
            List<String> buildStr = p.getBuildings().stream().map(Card::toClientString).collect(Collectors.toList());
            stats.put(p.getNickname(), new EventUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints(), tribeStr, buildStr
            ));
        }

        // 2. Transform cards to Strings
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
     * WHAT IT DOES: Notifies everyone who the new current player is.
     * WHY IT DOES IT: Allows the GUI to enable buttons only for that player, and show others "Waiting for...".
     */
    @Override
    public void onTurnChanged(Game game, Player newCurrentPlayer) {
        // Everyone sees who is the new current player.
        // If newCurrentPlayer is null (between phases), send null.
        String turnOwner = (newCurrentPlayer != null) ? newCurrentPlayer.getNickname() : null;
        connection.notifyTurnChanged(turnOwner);
    }

    /** {@inheritDoc} */
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

        // Initialize the Board structures as empty
        List<String> turnOrder = new java.util.ArrayList<>();
        Map<Integer, String> offering = new java.util.LinkedHashMap<>();
        List<String> charUp = new java.util.ArrayList<>();
        List<String> charLow = new java.util.ArrayList<>();
        List<String> eventUp = new java.util.ArrayList<>();
        List<String> eventLow = new java.util.ArrayList<>();
        List<String> buildUp = new java.util.ArrayList<>();
        List<String> buildLow = new java.util.ArrayList<>();

        Board b = game.getBoard();

        System.out.println("[DEBUG-VV] onStateChanged state=" + newState.getClass().getSimpleName() + " board=" + (b != null));
        if (b != null) {
            System.out.println("[DEBUG-VV] board charUpperRow size=" + b.getCharacterUpperRow().size()
                    + " charLowerRow size=" + b.getCharacterLowerRow().size()
                    + " eventUpperRow size=" + b.getEventUpperRow().size()
                    + " buildUpperRow size=" + b.getBuildingCardsUpperRow().size());
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

        System.out.println("[DEBUG-VV] sending charUp=" + charUp.size() + " charLow=" + charLow.size()
                + " eventUp=" + eventUp.size() + " buildUp=" + buildUp.size());
        // Send the packet (safe!)
        connection.notifyStateChanged(
                newState.getClass().getSimpleName(), game.getCurrentEra(), game.getRoundCount(),
                stats, turnOrder, offering,
                charUp, charLow, eventUp, eventLow, buildUp, buildLow
        );
    }

    /** {@inheritDoc} */
    @Override
    public void onEndGame(Game game, List<Player> winners) {
        List<String> winnerNames = winners.stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());

        // Calculate the real scores map here on the Server
        java.util.Map<String, Integer> finalScores = new java.util.HashMap<>();
        for (Player p : game.getPlayers()) {
            finalScores.put(p.getNickname(), game.calculatePlayerScore(p));
        }

        // Send complete data to the connection
        connection.notifyEndGame(winnerNames, finalScores, game.getPlayers().size());
    }

    /**
     * Sends the complete game state to the client after a reconnection.
     * Builds a full {@link StateChangedUpdate} from the current board and
     * sends it only through this view's connection.
     *
     * @param game the current game instance
     */
    //FOR RECONNECT
    public void sendFullStateOnReconnect(Game game) {
        Board b = game.getBoard();

        // 1. Player Statistics and Cards
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        for (Player p : game.getPlayers()) {
            List<String> tribeStr = p.getMyTribe().stream().map(Card::toClientString).collect(Collectors.toList());
            List<String> buildStr = p.getBuildings().stream().map(Card::toClientString).collect(Collectors.toList());
            stats.put(p.getNickname(), new StateChangedUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints(), p.getTotemColors(), tribeStr, buildStr
            ));
        }

        List<String> turnOrder = new java.util.ArrayList<>();
        Map<Integer, String> offering = new java.util.LinkedHashMap<>();
        List<String> charUp = new java.util.ArrayList<>();
        List<String> charLow = new java.util.ArrayList<>();
        List<String> eventUp = new java.util.ArrayList<>();
        List<String> eventLow = new java.util.ArrayList<>();
        List<String> buildUp = new java.util.ArrayList<>();
        List<String> buildLow = new java.util.ArrayList<>();

        // Board guard
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

        // Send the packet only to THIS connection
        connection.notifyStateChanged(
                game.getCurrentState().getClass().getSimpleName(),
                game.getCurrentEra(), game.getRoundCount(),
                stats, turnOrder, offering,
                charUp, charLow, eventUp, eventLow, buildUp, buildLow
        );
    }

    /**
     * Sends suspension notification ONLY to this player.
     * Used by Controller for broadcast.
     */
    public void showGameSuspended() {
        connection.notifyGameSuspended();
    }

    /**
     * Sends resume notification ONLY to this player.
     * Used by Controller for broadcast.
     */
    public void showGameResumed() {
        connection.notifyGameResumed();
    }
}
