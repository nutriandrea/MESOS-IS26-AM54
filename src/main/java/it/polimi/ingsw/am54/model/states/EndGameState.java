package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import model.*;
import java.util.List;

/**
 * Stato di fine partita: calcola i punteggi finali e decreta il vincitore.
 * Nessun giocatore puo' pescare o piazzare totem.
 */
public class EndGameState extends GameState {

    @Override
    public void resolvePhase(Game game) throws Exception {
        // 1. Risolvi TUTTI gli eventi visibili (upper + lower), Farmer per ultimo
        game.getBoard().resolveAllVisibleEvents(game);

        // 2. Calcola punteggi e determina vincitore
        List<Player> winners = game.determineWinners();

        // 3. Notifica observers per schermata vittoria
        game.notifyEndGame(winners);
    }
}
