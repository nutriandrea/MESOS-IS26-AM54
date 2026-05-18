package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;

// Stato di pesca: i giocatori pescano personaggi e comprano edifici dalle offering tiles
public class PickingState extends GameState {

    @Override
    public void pickCard(Game game, String nickname, String row, int cardIndex) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        Player currentPicker = game.getCurrentPicker();

        if (currentPicker == null || !currentPicker.equals(player)) {
            throw new Exception("Non è il tuo turno per pescare!");
        }

        OfferingTile tile = player.getCurrentTile();
        boolean isUpper = row.equalsIgnoreCase("upper");
        boolean isLower = row.equalsIgnoreCase("lower");

        if (!isUpper && !isLower) {
            throw new Exception("Riga non valida (usa 'upper' o 'lower').");
        }

        // ==========================================
        // 1. CONTROLLO DISPONIBILITA' PICK NORMALE
        // ==========================================
        if (isUpper) {
            if (tile.getRemainingUpperPicks() <= 0) {
                throw new Exception("Non hai più pick disponibili per la fila upper.");
            }
        } else {
            if (tile.getRemainingLowerPicks() <= 0) {
                throw new Exception("Non hai più pick disponibili per la fila lower.");
            }
        }

        // ==========================================
        // 2. PRELIEVO E CONTROLLO CARTA
        // ==========================================
        int charRowSize = game.getBoard().getCharacterRowSize(row);
        Card card = null;

        if (cardIndex < charRowSize) {
            card = game.getBoard().takeCharacterCard(row, cardIndex);
        } else {
            int buildingIndex = cardIndex - charRowSize;
            card = game.getBoard().takeBuildingCard(row, buildingIndex);
        }

        if (card == null) {
            throw new Exception("Carta non trovata o non disponibile a quell'indice.");
        }

        if (!card.canBePurchased(player)) {
            card.reveal(game.getBoard(), row);
            throw new Exception("Non hai abbastanza cibo (🍖) per prendere questa carta!");
        }

        // ==========================================
        // 3. CONSUMO EFFETTIVO DEL PICK
        // ==========================================
        if (isUpper) {
            tile.useUpperPick();
        } else {
            tile.useLowerPick();
        }

        card.pick(player, game);

        int finalFood = player.getFood();
        int finalMagic = player.getMagicPoints();
        game.notifyCardPicked(player, row, cardIndex, finalFood, finalMagic);

        // ==========================================
        // 4. AUTO-FINE TURNO
        // ==========================================
        if (shouldAutoEndTurn(game, player)) {
            executeEndTurnLogic(game, player);
        }
    }

    @Override
    public void endTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        Player currentPicker = game.getCurrentPicker();

        if (currentPicker == null || !currentPicker.equals(player)) {
            throw new Exception("Non è il tuo turno!");
        }

        OfferingTile tile = player.getCurrentTile();

        // CONTROLLO REGOLE: Pescata Obbligatoria dei Personaggi
        if (hasMandatoryCharactersLeft(game, tile)) {
            throw new Exception("Non puoi terminare il turno volontariamente: ci sono ancora Personaggi disponibili che DEVI pescare!");
        }

        executeEndTurnLogic(game, player);
    }

    private boolean hasMandatoryCharactersLeft(Game game, OfferingTile tile) {
        boolean mandatoryUpper = tile.getRemainingUpperPicks() > 0 && game.getBoard().getCharacterRowSize("upper") > 0;
        boolean mandatoryLower = tile.getRemainingLowerPicks() > 0 && game.getBoard().getCharacterRowSize("lower") > 0;
        return mandatoryUpper || mandatoryLower;
    }

    private boolean shouldAutoEndTurn(Game game, Player player) {
        OfferingTile tile = player.getCurrentTile();

        if (!tile.hasPicksLeft()) return true;

        boolean upperEmpty = game.getBoard().getCharacterRowSize("upper") == 0 && game.getBoard().getBuildingRowSize("upper") == 0;
        boolean lowerEmpty = game.getBoard().getCharacterRowSize("lower") == 0 && game.getBoard().getBuildingRowSize("lower") == 0;

        if (tile.getRemainingUpperPicks() > 0 && !upperEmpty) return false;
        if (tile.getRemainingLowerPicks() > 0 && !lowerEmpty) return false;

        return true;
    }

    @Override
    public void skipTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        Player currentPicker = game.getCurrentPicker();

        if (currentPicker == null || !currentPicker.equals(player)) {
            throw new Exception("Non e' il turno di " + nickname);
        }

        executeEndTurnLogic(game, player);
    }

    private void executeEndTurnLogic(Game game, Player player) {
        TurnOrderSlot slot = game.assignPickerToTurnOrder(player);
        if (slot != null) {
            int bonus = slot.getBonusFood();
            if (bonus < 0) {
                if (player.getFood() >= Math.abs(bonus)) {
                    player.modifyFood(bonus);
                } else {
                    player.modifyAuraPoints(-2);
                }
            } else {
                player.modifyFood(bonus);
            }
        }

        game.notifyTotemReturned(player, slot.getPosition());
        player.getCurrentTile().freeSlot();
        player.setCurrentTile(null);

        // TRANSIZIONE: Se tutti hanno pickato, controlliamo gli Extra Pick!
        if (game.isPickingComplete()) {
            Player extraPicker = game.getPlayers().stream().filter(Player::hasExtraPick).findFirst().orElse(null);

            if (extraPicker != null) {
                game.setCurrentPlayer(extraPicker);
                game.setState(new ExtraPickingState());
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
}