package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;

// Stato per la risoluzione del Pick Extra alla fine del round normale
public class ExtraPickingState extends GameState {

    @Override
    public void pickCard(Game game, String nickname, String row, int cardIndex) throws Exception {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non è il tuo turno per pescare!");
        }

        if (!player.hasExtraPick()) {
            throw new Exception("Non hai un pick extra a disposizione!");
        }

        boolean isUpper = row.equalsIgnoreCase("upper");
        boolean isLower = row.equalsIgnoreCase("lower");

        if (!isUpper && !isLower) {
            throw new Exception("Riga non valida (usa 'upper' o 'lower').");
        }

        int charRowSize = game.getBoard().getCharacterRowSize(row);
        Card card = null;

        if (cardIndex < charRowSize) {
            card = game.getBoard().takeCharacterCard(row, cardIndex);
        } else {
            int buildingIndex = cardIndex - charRowSize;
            card = game.getBoard().takeBuildingCard(row, buildingIndex);
        }

        if (card == null) {
            throw new Exception("Carta non trovata a quell'indice.");
        }

        if (!card.canBePurchased(player)) {
            card.reveal(game.getBoard(), row);
            throw new Exception("Non hai abbastanza cibo (🍖) per prendere questa carta!");
        }

        // Consuma definitivamente l'extra pick
        player.setExtraPick(false);

        card.pick(player, game);

        int finalFood = player.getFood();
        int finalMagic = player.getMagicPoints();
        game.notifyCardPicked(player, row, cardIndex, finalFood, finalMagic);

        // Chiude automaticamente lo stato
        finishExtraPick(game);
    }

    @Override
    public void endTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non è il tuo turno!");
        }

        // Rinuncia volontariamente all'extra pick
        player.setExtraPick(false);
        finishExtraPick(game);
    }

    @Override
    public void skipTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        if (game.getCurrentPlayer().equals(player)) {
            player.setExtraPick(false);
            finishExtraPick(game);
        }
    }

    private void finishExtraPick(Game game) {
        // Cerca se ci sono ALTRI giocatori che hanno guadagnato un extra pick nel frattempo
        Player nextExtraPicker = game.getPlayers().stream().filter(Player::hasExtraPick).findFirst().orElse(null);

        if (nextExtraPicker != null) {
            game.setCurrentPlayer(nextExtraPicker);
        } else {
            // Fine totale della fase Pick -> Passaggio agli Eventi
            if (game.isTribeDeckEmpty() && game.getCurrentEra() == 3) {
                game.setState(new EndGameState());
            } else {
                game.setState(new EventState());
            }
        }
    }
}