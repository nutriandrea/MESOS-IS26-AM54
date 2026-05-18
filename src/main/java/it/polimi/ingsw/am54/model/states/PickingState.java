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

        // Controllo se ha pick disponibili
        if (isUpper && tile.getRemainingUpperPicks() <= 0) throw new Exception("Non hai più pick disponibili per la fila upper.");
        if (isLower && tile.getRemainingLowerPicks() <= 0) throw new Exception("Non hai più pick disponibili per la fila lower.");

        // Prelievo virtuale della carta per sicurezza
        int charRowSize = game.getBoard().getCharacterRowSize(row);
        Card card = null;

        if (cardIndex < charRowSize) {
            // È un personaggio
            card = game.getBoard().takeCharacterCard(row, cardIndex);
        } else {
            // È un edificio
            int buildingIndex = cardIndex - charRowSize;
            card = game.getBoard().takeBuildingCard(row, buildingIndex);
        }

        if (card == null) {
            throw new Exception("Carta non trovata o non disponibile a quell'indice.");
        }

        // --- CONTROLLO POLIMORFICO E ROLLBACK ---
        // Usiamo canBePurchased() che per i Personaggi è sempre true
        // e per i Building controlla il cibo - sconti
        if (!card.canBePurchased(player)) {
            // Se non può essere acquistata, rimettiamo la carta sulla board (Rollback)
            // reveal() metterà la carta nella fila giusta grazie al polimorfismo
            card.reveal(game.getBoard(), row);

            // Lanciamo l'eccezione PRIMA di scalare il pick
            throw new Exception("Non hai abbastanza cibo (🍖) per prendere questa carta!");
        }

        // Se la carta esiste e può essere presa, scaliamo il pick dalla tessera
        if (isUpper) tile.useUpperPick();
        else tile.useLowerPick();

        // Esecuzione logica di acquisto/pesca (se non ha cibo per l'edificio, qui lancerà eccezione.
        // In un design perfetto dovremmo rimettere la carta sulla board, ma per ora ci affidiamo ai check del Client)
        card.pick(player, game);

        int finalFood = player.getFood();
        int finalMagic = player.getMagicPoints();
        game.notifyCardPicked(player, row, cardIndex, finalFood, finalMagic);

        // ==========================================
        // AUTO-FINE TURNO
        // ==========================================
        // Se non ci sono più mosse possibili (né pick, né carte fisiche da prendere), chiudiamo il turno in automatico
        if (shouldAutoEndTurn(game, tile)) {
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

    // --- Helper Methods ---

    private boolean hasMandatoryCharactersLeft(Game game, OfferingTile tile) {
        // Se ho ancora pick "upper" E ci sono personaggi in "upper", devo pescare.
        boolean mandatoryUpper = tile.getRemainingUpperPicks() > 0 && game.getBoard().getCharacterRowSize("upper") > 0;
        // Stessa cosa per "lower"
        boolean mandatoryLower = tile.getRemainingLowerPicks() > 0 && game.getBoard().getCharacterRowSize("lower") > 0;

        return mandatoryUpper || mandatoryLower;
    }

    private boolean shouldAutoEndTurn(Game game, OfferingTile tile) {
        // Se ho esaurito tutte le azioni sulla tessera
        if (!tile.hasPicksLeft()) return true;

        // Se mi sono rimasti pick, controllo se le file sono fisicamente VUOTE.
        // Se sia Personaggi che Edifici sono esauriti nella fila in cui potrei pescare, devo finire il turno.
        boolean upperEmpty = game.getBoard().getCharacterRowSize("upper") == 0 && game.getBoard().getBuildingRowSize("upper") == 0;
        boolean lowerEmpty = game.getBoard().getCharacterRowSize("lower") == 0 && game.getBoard().getBuildingRowSize("lower") == 0;

        if (tile.getRemainingUpperPicks() > 0 && !upperEmpty) return false;
        if (tile.getRemainingLowerPicks() > 0 && !lowerEmpty) return false;

        return true;
    }

    /**
     * Salta il turno del giocatore disconnesso durante la fase di pesca.
     * Termina automaticamente il turno (senza controllare carte obbligatorie)
     * e avanza al prossimo picker o alla fase eventi.
     */
    @Override
    public void skipTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        Player currentPicker = game.getCurrentPicker();

        if (currentPicker == null || !currentPicker.equals(player)) {
            throw new Exception("Non e' il turno di " + nickname);
        }

        // Termina il turno saltando il controllo sulle carte obbligatorie
        executeEndTurnLogic(game, player);
    }

    private void executeEndTurnLogic(Game game, Player player) {
        // Assegnazione turno e gestione bonus Cibo
        TurnOrderSlot slot = game.assignPickerToTurnOrder(player);
        if (slot != null) {
            int bonus = slot.getBonusFood();
            if (bonus < 0) {
                // Pagamento cibo usando valore assoluto per sicurezza
                if (player.getFood() >= Math.abs(bonus)) {
                    player.modifyFood(bonus); // Esempio: -1
                } else {
                    player.modifyAuraPoints(-2);
                }
            } else {
                player.modifyFood(bonus);
            }
        }

        game.notifyTotemReturned(player, slot.getPosition());
        // Libera la tessera offerta
        player.getCurrentTile().freeSlot();
        player.setCurrentTile(null); // Pulizia del riferimento

        // Transizione di Fase o Turno
        if (game.isPickingComplete()) {
            if (game.isTribeDeckEmpty() && game.getCurrentEra() == 3) {
                game.setState(new EndGameState());
            } else {
                game.setState(new EventState());
            }
        } else {
            // Aggiorniamo il currentPlayer con il prossimo "picker" da sinistra a destra
            game.setCurrentPlayer(game.getCurrentPicker());
        }
    }
}