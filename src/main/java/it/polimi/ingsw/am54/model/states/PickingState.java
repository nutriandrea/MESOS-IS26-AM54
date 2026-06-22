package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;

public class PickingState extends GameState {
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

    private boolean hasMandatoryCharactersLeft(Game game, OfferingTile tile) {
        boolean mandatoryUpper = tile.getRemainingUpperPicks() > 0 &&
                !(game.getBoard().getCharacterRowSize("upper") == 0 && game.getBoard().getBuildingRowSize("upper") == 0);
        boolean mandatoryLower = tile.getRemainingLowerPicks() > 0 &&
                !(game.getBoard().getCharacterRowSize("lower") == 0 && game.getBoard().getBuildingRowSize("lower") == 0);
        return mandatoryUpper || mandatoryLower;
    }

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

    @Override
    public Player recoverActivePlayer(Game game) {
        return game.getCurrentPicker();
    }

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
