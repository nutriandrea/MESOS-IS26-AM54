package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;

// Stato di pesca: i giocatori pescano personaggi e comprano edifici dalle offering tiles
public class PickingState extends GameState {

    @Override
    public void pickCard(Game game, String nickname, String row, int cardIndex) throws Exception {
        Player player = game.getPlayerByNickname(nickname);

        // Usiamo getCurrentPlayer() invece di getCurrentPicker() per supportare il Turno Bonus
        if (!game.getCurrentPlayer().equals(player)) {
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

        if (tile == null) {
            // Se la tessera è NULL, significa che il giro normale è finito
            // e siamo nel "Turno Bonus Extra Pick"
            if (player.hasExtraPick() && isUpper) {
                usingExtraPick = true;
            } else {
                throw new Exception("Non hai tessere attive. Puoi usare l'Extra Pick SOLO per la fila upper.");
            }
        } else {
            // Logica normale durante il turno
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
        // 3. CONSUMO DEL PICK E RISOLUZIONE CARTA
        // ==========================================
        if (usingExtraPick) {
            player.setExtraPick(false); // Consumiamo il bonus
        } else {
            if (isUpper) tile.useUpperPick();
            else tile.useLowerPick();
        }

        // MAGIA DEL POLIMORFISMO: Zero instanceof.
        // Sarà la singola carta, nel suo Override di pick(), a lanciare internamente i suoi effetti "applyOnPurchase"
        card.pick(player, game);

        int finalFood = player.getFood();
        int finalMagic = player.getMagicPoints();
        game.notifyCardPicked(player, row, cardIndex, finalFood, finalMagic);

        // ==========================================
        // 4. AUTO-FINE TURNO
        // ==========================================
        if (usingExtraPick) {
            // Il turno bonus dura esattamente una mossa, quindi finisce subito
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

        // Se siamo nel Turno Bonus e il giocatore decide di passare
        if (tile == null) {
            if (player.hasExtraPick()) {
                player.setExtraPick(false); // Rinuncia al bonus
                executeEndTurnLogic(game, player);
                return;
            } else {
                throw new Exception("Azione non valida.");
            }
        }

        // CONTROLLO REGOLE NORMALI: Pescata Obbligatoria dei Personaggi
        if (hasMandatoryCharactersLeft(game, tile)) {

            // --- INIZIO TRUCCO PER TEST ---
            // Sostituisci "tester" con il nickname che usi di solito per fare debug rapido
            if (player.getNickname().equalsIgnoreCase("tester")||player.getNickname().equalsIgnoreCase("tester2")) {
                System.out.println("[DEV MODE] Il giocatore " + nickname + " ha saltato forzatamente la fase di pick.");
            } else {
                throw new Exception("Non puoi terminare il turno: ci sono ancora Personaggi disponibili che DEVI pescare!");
            }
        }

        executeEndTurnLogic(game, player);
    }

    // --- Helper Methods ---

    private boolean hasMandatoryCharactersLeft(Game game, OfferingTile tile) {
        boolean mandatoryUpper = tile.getRemainingUpperPicks() > 0 && game.getBoard().getCharacterRowSize("upper") > 0;
        boolean mandatoryLower = tile.getRemainingLowerPicks() > 0 && game.getBoard().getCharacterRowSize("lower") > 0;
        return mandatoryUpper || mandatoryLower;
    }

    private boolean shouldAutoEndTurn(Game game, Player player) {
        OfferingTile tile = player.getCurrentTile();
        if (tile == null) return true; // Sicurezza

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

        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non e' il turno di " + nickname);
        }

        if (player.hasExtraPick()) {
            player.setExtraPick(false);
        }

        executeEndTurnLogic(game, player);
    }

    private void executeEndTurnLogic(Game game, Player player) {
        // 1. GESTIONE TURNO NORMALE: Liberiamo la tessera e diamo il bonus cibo
        if (player.getCurrentTile() != null) {
            TurnOrderSlot slot = game.assignPickerToTurnOrder(player);
            if (slot != null) {
                int bonus = slot.getBonusFood();
                if (bonus < 0) {
                    if (player.getFood() >= Math.abs(bonus)) player.modifyFood(bonus);
                    else player.modifyAuraPoints(-2);
                } else {
                    player.modifyFood(bonus);
                }

                // ==========================================
                // INIEZIONE TRIGGER: On Turn End
                // La fine del turno del singolo giocatore lancia l'hook per edifici come BuildingTurnOrderBonus
                // ==========================================
                for (BuildingCard building : player.getBuildings()) {
                    if (building.getEffects() != null) {
                        for (var effect : building.getEffects()) {
                            effect.applyOnTurnEnd(player, game, building);
                        }
                    }
                }
            }

            game.notifyTotemReturned(player, slot.getPosition());
            player.getCurrentTile().freeSlot();
            player.setCurrentTile(null); // Chiudiamo la connessione con la plancia
        }

        // 2. CONTROLLO FLUSSO E TURNO BONUS
        if (game.isPickingComplete()) {

            // Tutti i giocatori hanno finito il giro normale.
            // Cerchiamo il primo giocatore che ha un Extra Pick non usato!
            Player extraPicker = game.getPlayers().stream()
                    .filter(Player::hasExtraPick)
                    .findFirst()
                    .orElse(null);

            if (extraPicker != null) {
                // INIZIA IL TURNO BONUS
                game.setCurrentPlayer(extraPicker);
                // Rimaniamo in PickingState per permettergli di pescare
            } else {
                // Nessuno ha l'Extra Pick (o lo hanno tutti consumato). ORA si cambia fase!
                if (game.isTribeDeckEmpty() && game.getCurrentEra() == 3) {
                    game.setState(new EndGameState());
                } else {
                    game.setState(new EventState());
                }
            }
        } else {
            // Il giro normale continua al giocatore successivo
            game.setCurrentPlayer(game.getCurrentPicker());
        }
    }
}