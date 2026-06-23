package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;

public class PickingState extends GameState {
    /**
     * Executes the drawing of a card by the current player. Verifies turn,
     * availability of picks on the tile (or any Extra Pick on the upper row),
     * validity of row/index and cost coverage; then consumes the pick, removes
     * the card from the board, applies its logic and notifies the clients. If the picks are
     * exhausted, automatically ends the turn.
     *
     * @param game      the current game
     * @param nickname  the player who is picking
     * @param row       the chosen row ("upper" or "lower")
     * @param cardIndex the index of the card (characters followed by buildings)
     * @throws Exception if it's not their turn, the move is invalid or they can't afford the card
     */
    @Override
    public void pickCard(Game game, String nickname, String row, int cardIndex) throws Exception {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non è il tuo turno per pescare!");
        }

        OfferingTile tile = player.getCurrentTile();
        boolean isUpper = row.equalsIgnoreCase("upper");
        boolean isLower = row.equalsIgnoreCase("lower");

        if (!isUpper && !isLower) {
            throw new Exception("Riga non valida (usa 'upper' o 'lower').");
        }

        boolean usingExtraPick = false;

        // 1. Verify pick availability based on the tile or Extra Pick status
        if (tile == null) {
            if (player.hasExtraPick() && isUpper) {
                usingExtraPick = true;
            } else {
                throw new Exception("Non hai tessere attive. Puoi usare l'Extra Pick SOLO per la fila upper.");
            }
        } else {
            if (isUpper) {
                if (tile.getRemainingUpperPicks() <= 0) {
                    throw new Exception("Non hai più pick disponibili per la fila upper.");
                }
            } else {
                if (tile.getRemainingLowerPicks() <= 0) {
                    throw new Exception("Non hai più pick disponibili per la fila lower.");
                }
            }
        }

        // 2. Peek at the target card without removing it from the board yet
        int charRowSize = game.getBoard().getCharacterRowSize(row);
        Card targetCard = null;
        boolean isBuilding = cardIndex >= charRowSize;

        if (isBuilding) {
            int buildingIndex = cardIndex - charRowSize;
            java.util.List<it.polimi.ingsw.am54.model.BuildingCard> buildings = isUpper ? game.getBoard().getBuildingCardsUpperRow() : game.getBoard().getBuildingCardsLowerRow();
            if (buildingIndex < 0 || buildingIndex >= buildings.size()) {
                throw new Exception("Indice carta edificio non valido.");
            }
            targetCard = buildings.get(buildingIndex);
        } else {
            java.util.List<it.polimi.ingsw.am54.model.CharacterCard> characters = isUpper ? game.getBoard().getCharacterUpperRow() : game.getBoard().getCharacterLowerRow();
            if (cardIndex < 0 || cardIndex >= characters.size()) {
                throw new Exception("Indice carta personaggio non valido.");
            }
            targetCard = characters.get(cardIndex);
        }

        // 3. Validate affordability BEFORE altering game state (prevents the lock-up bug)
        if (!targetCard.canBePurchased(player)) {
            throw new Exception("Cibo insufficiente per acquistare questa carta.");
        }

        // 4. Safely consume the pick and remove the card from the board
        if (tile != null) {
            if (isUpper) tile.useUpperPick();
            else tile.useLowerPick();
        }

        Card pickedCard;
        if (isBuilding) {
            pickedCard = game.getBoard().takeBuildingCard(row, cardIndex - charRowSize);
        } else {
            pickedCard = game.getBoard().takeCharacterCard(row, cardIndex);
        }

        // 5. Execute the purchase logic
        pickedCard.pick(player, game);

        // 6. Notify clients
        int finalFood = player.getFood();
        int finalMagic = player.getMagicPoints();
        game.notifyCardPicked(player, row, cardIndex, finalFood, finalMagic);

        // 7. Auto-end turn logic (preserved from original logic)
        if (usingExtraPick) {
            player.setExtraPick(false);
            executeEndTurnLogic(game, player);
        } else if (shouldAutoEndTurn(game, player)) {
            executeEndTurnLogic(game, player);
        }
    }

    /**
     * Voluntarily ends the player's picking turn. Not allowed if there are
     * mandatory picks left with available characters (except for the development
     * nicknames "tester"/"tester2"). In case of unused Extra Pick, consumes it.
     *
     * @param game     the current game
     * @param nickname the player ending the turn
     * @throws Exception if it's not their turn or there are residual mandatory picks
     */
    @Override
    public void endTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non è il tuo turno!");
        }

        OfferingTile tile = player.getCurrentTile();

        if (tile == null) {
            if (player.hasExtraPick()) {
                player.setExtraPick(false);
                executeEndTurnLogic(game, player);
                return;
            } else {
                throw new Exception("Azione non valida.");
            }
        }

        if (hasMandatoryCharactersLeft(game, tile)) {
            if (player.getNickname().equalsIgnoreCase("tester")||player.getNickname().equalsIgnoreCase("tester2")) {
                System.out.println("[DEV MODE] Il giocatore " + nickname + " ha saltato forzatamente la fase di pick.");
            } else {
                throw new Exception("Non puoi terminare il turno: ci sono ancora Personaggi disponibili che DEVI pescare!");
            }
        }

        executeEndTurnLogic(game, player);
    }

    /**
     * Checks if the tile still has mandatory picks to spend on rows that
     * contain cards (a pick is mandatory as long as there are pickable cards).
     *
     * @param game the current game
     * @param tile the player's tile
     * @return true if there are mandatory picks left
     */
    private boolean hasMandatoryCharactersLeft(Game game, OfferingTile tile) {
        boolean mandatoryUpper = tile.getRemainingUpperPicks() > 0 &&
                !(game.getBoard().getCharacterRowSize("upper") == 0 && game.getBoard().getBuildingRowSize("upper") == 0);
        boolean mandatoryLower = tile.getRemainingLowerPicks() > 0 &&
                !(game.getBoard().getCharacterRowSize("lower") == 0 && game.getBoard().getBuildingRowSize("lower") == 0);
        return mandatoryUpper || mandatoryLower;
    }

    /**
     * Skips the picking turn of the current player (e.g. disconnected): closes their
     * pick returning the totem, applies the end-of-turn building effects and advances
     * the phase (next picker, or transition to Event/EndGame if picking is complete).
     *
     * @param game     the current game
     * @param nickname the player to skip
     * @throws Exception if it's not their turn
     */
    @Override
    public void skipTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non e' il turno di " + nickname);
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

    /**
     * Ends the player's turn and advances the phase: reinserts the player
     * into the turn order collecting the bonus, then chooses the next picker or,
     * if picking is complete, transitions to EndGame (deck exhausted in Era 3) or Event.
     *
     * @param game   the current game
     * @param player the player whose turn is being closed
     */
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

    /**
     * Finalizes the totem's return to the turn order: applies the food bonus/malus
     * of the slot (lacking food, the malus becomes -2 aura), notifies the return and
     * frees the player's offering tile.
     *
     * @param game   the current game
     * @param player the player ending the turn
     * @param slot   the turn order slot assigned to the player
     */
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

    /**
     * Determines if the turn should close automatically, i.e., when both rows of the
     * tile no longer have available picks or pickable cards.
     *
     * @param game   the current game
     * @param player the turn player
     * @return true if there is nothing left to pick and the turn can close itself
     */
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
     * After a crash recovery, identifies the player who is picking (the first
     * occupant of the offering track not yet returned to the turn order).
     *
     * @param game the current game
     * @return the current picker, or {@code null}
     */
    @Override
    public Player recoverActivePlayer(Game game) {
        return game.getCurrentPicker();
    }

    /**
     * Handles the disconnection of a NON-turn player who had an occupied tile:
     * frees it, moves them to the bottom of the turn order and notifies the
     * totem return (the turn player continues picking undisturbed).
     *
     * @param game            the current game
     * @param p               the disconnected player
     * @param isCurrentPlayer true if they were the turn player
     * @throws Exception in case of error
     */
    @Override
    public void handleDisconnection(Game game, Player p, boolean isCurrentPlayer) throws Exception {
        if (!isCurrentPlayer && p.getCurrentTile() != null) {
            p.getCurrentTile().freeSlot();
            p.setCurrentTile(null);
            game.moveToLastSlot(p);
            game.notifyTotemReturned(p, -1);
        }
    }
}