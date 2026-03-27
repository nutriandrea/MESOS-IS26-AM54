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

        // ==========================================
        // NUOVA LOGICA DI PESCATA
        // Usiamo i metodi della tessera per scalare fisicamente le pescate disponibili
        // ==========================================
        if (row.equalsIgnoreCase("upper")) {
            if (!tile.useUpperPick()) {
                throw new Exception("Non hai più pick disponibili per la fila upper");
            }
        } else if (row.equalsIgnoreCase("lower")) {
            if (!tile.useLowerPick()) {
                throw new Exception("Non hai più pick disponibili per la fila lower");
            }
        } else {
            throw new Exception("Errore: Fila non valida. Usa 'upper' o 'lower'.");
        }
        // ==========================================

        Card card = null;

        // Calcolo dell'indice per estrarre la carta giusta (Personaggio o Edificio)
        if (row.equalsIgnoreCase("upper")) {
            if (cardIndex < game.getBoard().getCharacterRowSize("upper")) {
                card = game.getBoard().takeCharacterCard(row, cardIndex);
            } else {
                int buildingIndex = cardIndex - game.getBoard().getCharacterRowSize("upper");
                card = game.getBoard().takeBuildingCard(row, buildingIndex);
            }
        } else if (row.equalsIgnoreCase("lower")) {
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

        // Eseguiamo la logica specifica della carta (aggiunge al player, scala il cibo per gli edifici, ecc.)
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

        // Assegna al primo slot libero e ottieni il bonus cibo
        TurnOrderSlot slot = game.assignPickerToTurnOrder(player);
        if (slot != null && slot.getBonusFood() > 0) {
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
