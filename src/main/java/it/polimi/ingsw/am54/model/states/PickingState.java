package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;

//Stato di pesca: i giocatori pescano personaggi e comprano edifici dalle offering tiles
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
            throw new Exception("Error: Line not found");
        }

        if (isUpper) {
            if (!tile.useUpperPick()) throw new Exception("Non hai più pick disponibili per la fila upper");
        } else {
            if (!tile.useLowerPick()) throw new Exception("Non hai più pick disponibili per la fila lower");
        }

        // prelievo della carta dalla board
        Card card;
        int charRowSize = game.getBoard().getCharacterRowSize(row);

        if (cardIndex < charRowSize) {
            card = game.getBoard().takeCharacterCard(row, cardIndex);
        } else {
            int buildingIndex = cardIndex - charRowSize;
            card = game.getBoard().takeBuildingCard(row, buildingIndex);
        }

        if (card == null) {
            throw new Exception("Carta non trovata o non disponibile a quell'indice.");
        }

        // esecuzione logica di acquisto/pesca
        card.pick(player, game);
    }

    @Override
    public void endTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        Player currentPicker = game.getCurrentPicker();

        if (currentPicker == null || !currentPicker.equals(player)) {
            throw new Exception("Non è il tuo turno!");
        }


        TurnOrderSlot slot = game.assignPickerToTurnOrder(player);
        if (slot != null) {
            int bonus = slot.getBonusFood();
            if(bonus<0){
                if(player.getFood()>=bonus){
                    player.modifyFood(bonus);
                }
                else{
                    player.modifyAuraPoints(-2);
                }
            }
            else{player.modifyFood(bonus);}
        }


        player.getCurrentTile().freeSlot();

        // controllo transizione di fase
        if (game.isPickingComplete()) {
            game.setState(new EventState());
        }
    }
}