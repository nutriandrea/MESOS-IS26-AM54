package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;

/**
 * State that manages the card drawing phase (characters and buildings) by players.
 * <p>
 * Each player, following the order determined by the totem placement in the
 * {@link StartTurnState}, draws available cards from the upper and lower
 * rows of the board. Each offering tile with a placed totem defines how many picks are
 * available for the upper row and for the lower row (typically 1 pick per row).
 * </p>
 * <p>
 * During this phase the player can:
 * <ul>
 *   <li>Draw a character or building card from either row via {@link #pickCard}</li>
 *   <li>End their turn early via {@link #endTurn}</li>
 *   <li>Skip the turn via {@link #skipTurn}</li>
 * </ul>
 * </p>
 * <p>
 * A player without an active tile (because they arrived after tiles were exhausted) can
 * still perform an "Extra Pick" on the upper row if enabled by a building with
 * effect {@link it.polimi.ingsw.am54.model.effects.buildings.BuildingExtraPick}.
 * </p>
 * <p>
 * The phase ends when all players have completed their draw. At that point it transitions
 * to {@link EventState} for event resolution, or to {@link EndGameState} if the
 * tribe deck is exhausted and the game is in Era III.
 * </p>
 *
 * @author AM54
 */
public class PickingState extends GameState {
    /**
     * Allows the player to draw a card (character or building) from either row.
     * <p>
     * The method handles the entire draw flow:
     * <ol>
     *   <li>Verifies it's the requesting player's turn</li>
     *   <li>Determines if the player has an active tile or is using an Extra Pick</li>
     *   <li>Checks availability of remaining picks for the requested row (upper/lower)</li>
     *   <li>Retrieves the card at the specified index, distinguishing between characters (first part of row)
     *       and buildings (second part of the row) based on character row size</li>
     *   <li>Decrements the tile pick counter</li>
     *   <li>Applies the card effect on the player via {@code card.pick()}</li>
     *   <li>Notifies the draw to connected clients</li>
     *   <li>If the Extra Pick has been consumed or the turn ends automatically</li>
     *       (all picks exhausted), executes end-of-turn logic</li>
     * </ol>
     * </p>
     *
     * @param game      the current game instance
     * @param nickname  the nickname of the player drawing
     * @param row       the row to draw from ("upper" or "lower")
     * @param cardIndex index of the card in the row (0-based); characters occupy
     *                  the first positions, buildings the following ones
     * @throws GameException if not the player's turn, if the row is invalid,
     *                       if no picks are available for that row, or if the card
     *                       is not found at the specified position
     */
    @Override
    public void pickCard(Game game, String nickname, String row, int cardIndex) throws GameException {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().equals(player)) {
            throw new GameException("It is not your turn to pick!");
        }

        OfferingTile tile = player.getCurrentTile();
        boolean isUpper = row.equalsIgnoreCase("upper");
        boolean isLower = row.equalsIgnoreCase("lower");

        if (!isUpper && !isLower) {
            throw new GameException("Invalid row (use 'upper' or 'lower').");
        }

        boolean usingExtraPick = false;

        if (tile == null) {
            if (player.hasExtraPick() && isUpper) {
                usingExtraPick = true;
            } else {
                throw new GameException("You have no active tiles. You can use the Extra Pick ONLY for the upper row.");
            }
        } else {
            if (isUpper) {
                if (tile.getRemainingUpperPicks() <= 0) {
                    throw new GameException("No more picks available for the upper row.");
                }
            } else {
                if (tile.getRemainingLowerPicks() <= 0) {
                    throw new GameException("No more picks available for the lower row.");
                }
            }
        }

        int charRowSize = game.getBoard().getCharacterRowSize(row);
        Card card = null;

        if (cardIndex < charRowSize) {
            card = game.getBoard().takeCharacterCard(row, cardIndex);
        } else {
            int buildingIndex = cardIndex - charRowSize;
            card = game.getBoard().takeBuildingCard(row, buildingIndex);
        }

        if (card == null) {
            throw new GameException("Card not found at the specified position.");
        }

        if (tile != null) {
            if (isUpper) tile.useUpperPick();
            else tile.useLowerPick();
        }

        card.pick(player, game);

        int finalFood = player.getFood();
        int finalMagic = player.getMagicPoints();
        game.notifyCardPicked(player, row, cardIndex, finalFood, finalMagic);

        if (usingExtraPick) {
            executeEndTurnLogic(game, player);
        } else if (shouldAutoEndTurn(game, player)) {
            executeEndTurnLogic(game, player);
        }
    }

    /**
     * Ends the player's draw turn early.
     * <p>
     * The player can voluntarily end the turn even if they still have picks available.
     * However, if there are still mandatory drawable characters (non-empty row with remaining picks),
     * the turn CANNOT end — the player is obligated to draw all available characters
     * before being able to pass (exception: DEV mode for nicknames "tester" and "tester2").
     * </p>
     * <p>
     * If the player has no active tile but has an Extra Pick available, the Extra Pick
     * is consumed and proceeds with end-of-turn logic.
     * </p>
     *
     * @param game     the current game instance
     * @param nickname the nickname of the player ending the turn
     * @throws GameException if not the player's turn, if the action is not valid,
     *                       or if there are still mandatory characters to draw
     */
    @Override
    public void endTurn(Game game, String nickname) throws GameException {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().equals(player)) {
            throw new GameException("It is not your turn!");
        }

        OfferingTile tile = player.getCurrentTile();

        if (tile == null) {
            if (player.hasExtraPick()) {
                player.setExtraPick(false);
                executeEndTurnLogic(game, player);
                return;
            } else {
                throw new GameException("Invalid action.");
            }
        }

        if (hasMandatoryCharactersLeft(game, tile)) {
            throw new GameException("You cannot end your turn: there are still Characters available that you MUST draw!");
        }

        executeEndTurnLogic(game, player);
    }

    /**
     * Checks if there are still mandatory characters to draw for the player's tile.
     * <p>
     * A character is considered "mandatory" when the tile still has picks available
     * for that row AND the corresponding row is not completely empty (neither characters nor buildings).
     * Both the upper and lower rows are checked.
     * </p>
     *
     * @param game the current game instance
     * @param tile the offering tile associated with the player
     * @return {@code true} if there are still mandatory characters to draw
     */
    private boolean hasMandatoryCharactersLeft(Game game, OfferingTile tile) {
        boolean mandatoryUpper = tile.getRemainingUpperPicks() > 0 &&
                !(game.getBoard().getCharacterRowSize("upper") == 0 && game.getBoard().getBuildingRowSize("upper") == 0);
        boolean mandatoryLower = tile.getRemainingLowerPicks() > 0 &&
                !(game.getBoard().getCharacterRowSize("lower") == 0 && game.getBoard().getBuildingRowSize("lower") == 0);
        return mandatoryUpper || mandatoryLower;
    }

    /**
     * Forcibly skips the player's draw turn.
     * <p>
     * If the player has an Extra Pick available, it is consumed. If the player has an
     * active tile, they are moved to the last turn order slot and the
     * player's building end-of-turn effects (food bonus/penalty) are applied. The tile
     * freed and the totem returned.
     * </p>
     * <p>
     * After the skip, if everyone has completed drawing, it looks for any player with
     * Extra Pick still to exercise. If there are none, it transitions to {@link EventState}
     * or to {@link EndGameState} if the tribe deck is exhausted in Era III.
     * </p>
     *
     * @param game     the current game instance
     * @param nickname the nickname of the player skipping the turn
     * @throws GameException if not the player's turn
     */
    @Override
    public void skipTurn(Game game, String nickname) throws GameException {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().equals(player)) {
            throw new GameException("It is not " + nickname + "'s turn");
        }

        if (player.hasExtraPick()) {
            player.setExtraPick(false);
        }

        if (player.getCurrentTile() != null) {
            TurnOrderSlot slot = game.moveToLastSlot(player);
            finalizePlayerTurn(game, player, slot);
            if (slot != null) {
                for (BuildingCard building : player.getBuildings()) {
                    if (building.getEffects() != null) {
                        for (var effect : building.getEffects()) {
                            effect.applyOnTurnEnd(player, game, building);
                        }
                    }
                }
            }
        }

        if (game.isPickingComplete()) {
            Player extraPicker = game.getPlayers().stream()
                    .filter(Player::hasExtraPick)
                    .findFirst()
                    .orElse(null);

            if (extraPicker != null) {
                game.setCurrentPlayer(extraPicker);
            } else {
                if (game.isTribeDeckEmpty() && game.getCurrentEra() == 3) {
                    game.setState(new EndGameState());
                } else {
                    game.setState(new EventState());
                }
            }
        } else {
            game.setCurrentPlayer(game.getCurrentPicker());
        }
    }

    private void executeEndTurnLogic(Game game, Player player) {
        if (player.getCurrentTile() != null) {
            finalizePlayerTurn(game, player, game.assignPickerToTurnOrder(player));
        }

        if (game.isPickingComplete()) {
            Player extraPicker = game.getPlayers().stream()
                    .filter(Player::hasExtraPick)
                    .findFirst()
                    .orElse(null);

            if (extraPicker != null) {
                game.setCurrentPlayer(extraPicker);
            } else {
                if (game.isTribeDeckEmpty() && game.getCurrentEra() == 3) {
                    game.setState(new EndGameState());
                } else {
                    game.setState(new EventState());
                }
            }
        } else {
            game.setCurrentPlayer(game.getCurrentPicker());
        }
    }

    private void finalizePlayerTurn(Game game, Player player, TurnOrderSlot slot) {
        if (slot != null) {
            int bonus = slot.getBonusFood();
            if (bonus < 0) {
                if (player.getFood() >= Math.abs(bonus)) player.modifyFood(bonus);
                else player.modifyAuraPoints(-2);
            } else {
                player.modifyFood(bonus);
            }
        }

        game.notifyTotemReturned(player, slot != null ? slot.getPosition() : -1);
        player.getCurrentTile().freeSlot();
        player.setCurrentTile(null);
    }

    private boolean shouldAutoEndTurn(Game game, Player player) {
        OfferingTile tile = player.getCurrentTile();
        if (tile == null) return false;

        boolean upperDone = tile.getRemainingUpperPicks() <= 0 ||
                (game.getBoard().getCharacterRowSize("upper") == 0 && game.getBoard().getBuildingRowSize("upper") == 0);
        boolean lowerDone = tile.getRemainingLowerPicks() <= 0 ||
                (game.getBoard().getCharacterRowSize("lower") == 0 && game.getBoard().getBuildingRowSize("lower") == 0);
        return upperDone && lowerDone;
    }

    /**
     * Retrieves the player who should resume drawing after a reconnection.
     *
     * @param game the current game instance
     * @return the player whose pick turn is currently active
     */
    @Override
    public Player recoverActivePlayer(Game game) {
        return game.getCurrentPicker();
    }

    /**
     * Handles a player disconnection during the picking phase by freeing
     * their offering tile and moving them to the last turn order slot,
     * but only if they are not the current player.
     *
     * @param game             the current game instance
     * @param p                the player who disconnected
     * @param isCurrentPlayer  {@code true} if the disconnected player was the current one
     * @throws GameException if an error occurs during handling
     */
    @Override
    public void handleDisconnection(Game game, Player p, boolean isCurrentPlayer) throws GameException {
        if (!isCurrentPlayer && p.getCurrentTile() != null) {
            p.getCurrentTile().freeSlot();
            p.setCurrentTile(null);
            game.moveToLastSlot(p);
            game.notifyTotemReturned(p, -1);
        }
    }
}
