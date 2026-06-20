package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameException;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.OfferingTile;

/**
 * State that manages the totem placement phase at the beginning of each round.
 * <p>
 * In this phase, players, following the order determined by the turn order track,
 * place their totem on one of the available offering tiles. The choice of tile
 * determines the card draw order in the subsequent {@link PickingState}: the first
 * player who placed the totem on the leftmost tile will be the first to draw,
 * and so on.
 * </p>
 * <p>
 * The phase ends when all active players have placed their totem. At that point
 * it automatically transitions to {@link PickingState}. If a player disconnects during
 * this phase, their totem is removed and the turn order is compacted.
 * The method {@link #skipTurn(Game, String)} allows skipping the placement,
 * automatically placing the player in the last available spot.
 * </p>
 *
 * @author AM54
 */
public class StartTurnState extends GameState {
    /**
     * Places the player's totem on the specified offering tile.
     * <p>
     * The method performs the following checks in order:
     * <ol>
     *   <li>Verifies it is indeed the turn of the player requesting the placement</li>
     *   <li>Checks that the player has not already placed the totem this round</li>
     *   <li>Verifies the specified position is valid (within the offering track limits)</li>
     *   <li>Checks the target offering tile is not already occupied by another totem</li>
     * </ol>
     * </p>
     * <p>
     * If all checks pass, the totem is placed on the tile, the player is
     * marked as {@code hasPlacedTotem = true} and the tile is associated with the player via
     * {@code currentTile}. The player's old slot in the turn order track is
     * freed. If all active players have placed, it transitions to {@link PickingState};
     * otherwise it passes to the next player.
     * </p>
     *
     * @param game           the current game instance
     * @param nickname       the nickname of the player requesting the placement
     * @param offerPosition  index of the offering tile (0-based) to place the totem on
     * @throws GameException if not the player's turn, if they already placed,
     *                       if the position is out of bounds or if the tile is already occupied
     */
    @Override
    public void placeTotem(Game game, String nickname, int offerPosition) throws GameException {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().getNickname().equalsIgnoreCase(player.getNickname())) {
            throw new GameException("It is not your turn to place the totem!");
        }
        if (player.hasPlacedTotem()) {
            throw new GameException("You have already placed your totem this round!");
        }

        if (offerPosition < 0 || offerPosition >= game.getBoard().getOfferingTrack().size()) {
            throw new GameException("Invalid offering position.");
        }
        OfferingTile targetTile = game.getBoard().getOfferingTrack().get(offerPosition);

        if (!targetTile.isAvailable()) {
            throw new GameException("This offering tile is already occupied!");
        }

        game.getBoard().addTotemToPosition(player, targetTile);
        player.setHasPlacedTotem(true);
        player.setCurrentTile(targetTile);

        game.notifyTotemPlaced(player, offerPosition);

        game.getBoard().getTurnOrderTile().stream()
                .filter(slot -> slot.getOccupiedBy() != null &&
                        slot.getOccupiedBy().getNickname().equalsIgnoreCase(player.getNickname()))
                .findFirst()
                .ifPresent(slot -> slot.freeSlot());

        // FIX: The phase passes when all ACTIVE players have placed
        if (game.getBoard().allTotemsPlaced(game.getActivePlayerCount())) {
            game.setState(new PickingState());
            Player firstPicker = game.getBoard().getFirstPlayerFromOfferingTrack();
            game.setCurrentPlayer(firstPicker);
        } else {
            game.nextPlayer();
        }
    }

    /**
     * Skips the totem placement turn for the specified player.
     * <p>
     * The player is automatically moved to the last available slot in the
     * turn order track (least advantageous position for the subsequent draw phase).
     * They are still marked as {@code hasPlacedTotem = true} so as not to block the transition
     * to the next phase. After the skip, the turn passes to the next player.
     * </p>
     *
     * @param game     the current game instance
     * @param nickname the nickname of the player skipping the turn
     * @throws GameException if not the player's turn or if they already placed the totem
     */
    @Override
    public void skipTurn(Game game, String nickname) throws GameException {
        Player player = game.getPlayerByNickname(nickname);
        if (!game.getCurrentPlayer().equals(player)) {
            throw new GameException("It is not " + nickname + "'s turn");
        }
        if (player.hasPlacedTotem()) {
            throw new GameException(nickname + " has already placed the totem");
        }
        game.moveToLastSlot(player);
        player.setHasPlacedTotem(true);
        game.nextPlayer();
    }

    /**
     * Retrieves the next active player who has not yet placed the totem.
     * <p>
     * Scans the turn order track and returns the first player
     * who has not yet completed totem placement this round.
     * Useful for recovery in case of disconnection or to determine whose
     * turn it is after a reconnection.
     * </p>
     *
     * @param game the current game instance
     * @return the first player who still needs to place the totem, or {@code null}
     *         if everyone has already placed
     */
    @Override
    public Player recoverActivePlayer(Game game) {
        for (it.polimi.ingsw.am54.model.TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            Player p = slot.getOccupiedBy();
            if (p != null && !p.hasPlacedTotem()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Handles a player disconnection during the totem placement phase.
     * <p>
     * If the player had already placed on an offering tile, the totem is removed
     * and the tile is freed for other players. The player is removed from the
     * turn order track and subsequent slots are shifted upward to maintain
     * contiguity. If the disconnected player was the current one, it passes to the
     * next player. If after the disconnection all remaining players have placed,
     * it automatically transitions to {@link PickingState}.
     * </p>
     *
     * @param game             the current game instance
     * @param p                the player who disconnected
     * @param isCurrentPlayer  {@code true} if the disconnected player was the current one
     * @throws GameException if an error occurs during handling
     */
    @Override
    public void handleDisconnection(Game game, Player p, boolean isCurrentPlayer) throws GameException {
        if (p.hasPlacedTotem() && p.getCurrentTile() != null) {
            p.getCurrentTile().freeSlot();
            p.setCurrentTile(null);
            game.notifyTotemReturned(p, -1);
        }

        game.getBoard().removePlayerFromTurnOrderAndShiftUp(p);

        if (isCurrentPlayer) {
            game.nextPlayer();
        }

        if (game.getBoard().allTotemsPlaced(game.getActivePlayerCount())) {
            game.setState(new PickingState());
            Player first = game.getBoard().getFirstPlayerFromOfferingTrack();
            if (first != null) game.setCurrentPlayer(first);
        }
    }
}