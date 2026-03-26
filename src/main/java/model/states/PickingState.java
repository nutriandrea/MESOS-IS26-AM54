package model.states;

import model.*;

/**
 * Stato di pesca: i giocatori pescano personaggi e comprano edifici dalle offering tiles.
 */
public class PickingState extends GameState {

    @Override
    public void pickCard(Game game, String nickname, String row, int cardIndex) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        if (player == null) {
            throw new Exception("Giocatore non trovato: " + nickname);
        }

        Player currentPicker = game.getCurrentPicker();
        if (currentPicker == null || !currentPicker.equals(player)) {
            throw new Exception("Non è il tuo turno per pescare!");
        }

        OfferingTile tile = player.getCurrentTile();
        if (tile == null) {
            throw new Exception("Non hai una offering tile assegnata!");
        }

        int[] picksAvailable = tile.getCards();
        int remainingPicks = row.equalsIgnoreCase("upper") 
            ? picksAvailable[1] 
            : picksAvailable[0];

        if (remainingPicks <= 0) {
            throw new Exception("Non hai più pick disponibili per la fila " + row);
        }

        int currentPicks = row.equalsIgnoreCase("upper") 
            ? picksAvailable[1] 
            : picksAvailable[0];
        if (row.equalsIgnoreCase("upper")) {
            picksAvailable[1] = currentPicks - 1;
        } else {
            picksAvailable[0] = currentPicks - 1;
        }

        Card card;
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
            throw new Exception("Carta non trovata o non disponibile.");
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

        game.assignPickerToTurnOrder(player);
        player.getCurrentTile().freeSlot();
        System.out.println(player.getNickname() + " si e' posizionato nel turno order");

        if (game.isPickingComplete()) {
            System.out.println("Tutti i giocatori hanno finito di pescare! Transizione a EventState...");
            game.setState(new EventState());
        } else {
        }
    }
}
