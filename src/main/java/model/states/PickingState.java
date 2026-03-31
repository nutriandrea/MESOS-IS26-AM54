package model.states;

import model.*;

/**
 * Stato di pesca: i giocatori pescano personaggi e comprano edifici dalle offering tiles.
 */
public class PickingState extends GameState {

    @Override
    public void pickCard(Game game, String nickname, String row, int cardIndex) throws Exception {
        Player player = game.getPlayerByNickname(nickname);

        Player currentPicker = game.getCurrentPicker();
        if (currentPicker == null || !currentPicker.equals(player)) {
            throw new Exception("Non è il tuo turno per pescare!");
        }

        OfferingTile tile = player.getCurrentTile();

        if (!row.equalsIgnoreCase("upper") && !row.equalsIgnoreCase("lower")) {
            throw new Exception("Errore: Fila non valida. Usa 'upper' o 'lower'.");
        }

        if (row.equalsIgnoreCase("upper")) {
            if (!tile.useUpperPick()) {
                throw new Exception("Non hai più pick disponibili per la fila upper");
            }
        } else {
            if (!tile.useLowerPick()) {
                throw new Exception("Non hai più pick disponibili per la fila lower");
            }
        }

        Card card = null;

        if (row.equalsIgnoreCase("upper")) {
            if (cardIndex < game.getBoard().getCharacterRowSize("upper")) {
                card = game.getBoard().takeCharacterCard(row, cardIndex);
            } else {
                int buildingIndex = cardIndex - game.getBoard().getCharacterRowSize("upper");
                card = game.getBoard().takeBuildingCard(row, buildingIndex);
            }
        } else {
            if (cardIndex < game.getBoard().getCharacterRowSize("lower")) {
                card = game.getBoard().takeCharacterCard(row, cardIndex);
            } else {
                int buildingIndex = cardIndex - game.getBoard().getCharacterRowSize("lower");
                card = game.getBoard().takeBuildingCard(row, buildingIndex);
            }
        }

        if (card == null) {
            throw new Exception("Carta non trovata o non disponibile a quell'indice.");
        }

        card.pick(player, game);

    }

    @Override
    public void endTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        if (player == null) {
            throw new Exception("Giocatore non trovato: " + nickname);
        }

        Player currentPicker = game.getCurrentPicker();
        if (currentPicker == null || !currentPicker.equals(player)) {
            throw new Exception("Non è il tuo turno!");
        }

        TurnOrderSlot slot = game.assignPickerToTurnOrder(player);
        if (slot != null) {
            player.modifyFood(slot.getBonusFood());
        }
        
        player.getCurrentTile().freeSlot();
        System.out.println(player.getNickname() + " si è posizionato nell'ordine di turno.");

        if (game.isPickingComplete()) {
            System.out.println("Tutti i giocatori hanno finito di pescare! Transizione a EventState...");
            game.setState(new EventState());
        }
    }
}
