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
        // 1. CONTROLLO DISPONIBILITA' PICK & EXTRA PICK
        // ==========================================
        boolean usingExtraPick = false;

        if (isUpper) {
            // Se la tessera è vuota, controlliamo il bonus
            if (tile.getRemainingUpperPicks() <= 0) {
                if (player.hasExtraPick()) {
                    usingExtraPick = true; // Segniamo che stiamo per usare il bonus
                } else {
                    throw new Exception("Non hai più pick disponibili per la fila upper.");
                }
            }
        } else {
            // L'Extra Pick vale SOLO per la upper row. Sulla lower non ci salva.
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
        if (!card.canBePurchased(player)) {
            // Se non può essere acquistata, rimettiamo la carta sulla board
            card.reveal(game.getBoard(), row);
            throw new Exception("Non hai abbastanza cibo (\uD83C\uDF56) per prendere questa carta!");
        }

        // ==========================================
        // 3. CONSUMO EFFETTIVO DEL PICK
        // ==========================================
        // Lo facciamo solo ORA, perché siamo certi che la carta verrà acquistata!
        if (isUpper) {
            if (usingExtraPick) {
                player.setExtraPick(false); // Consumiamo il bonus
            } else {
                tile.useUpperPick(); // Consumiamo la tessera
            }
        } else {
            tile.useLowerPick();
        }

        // Esecuzione logica di acquisto
        card.pick(player, game);

        int finalFood = player.getFood();
        game.notifyCardPicked(player, row, cardIndex, finalFood);

        // ==========================================
        // 4. AUTO-FINE TURNO
        // ==========================================
        // MODIFICATO: Passiamo il player per controllare l'Extra Pick
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

        // MODIFICATO: Pulizia del bonus se il giocatore passa volontariamente
        if (player.hasExtraPick()) {
            player.setExtraPick(false);
        }

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

    // MODIFICATO: Prende (Game, Player) invece di (Game, OfferingTile)
    private boolean shouldAutoEndTurn(Game game, Player player) {
        // REGOLA AUREA: Se ho l'Extra Pick, il turno NON deve chiudersi in automatico!
        if (player.hasExtraPick()) return false;

        OfferingTile tile = player.getCurrentTile();

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

        // MODIFICATO: Pulizia del bonus in caso di disconnessione
        if (player.hasExtraPick()) {
            player.setExtraPick(false);
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