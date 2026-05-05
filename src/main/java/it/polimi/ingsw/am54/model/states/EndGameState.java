package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;

import java.util.List;

//Stato di fine partita: calcola i punteggi finali e decreta il vincitore
//Nessun giocatore puo' pescare o piazzare totem
public class EndGameState extends GameState {

    @Override
    public void resolvePhase(Game game) {
        EventCard currentFinalEvent;

        // Cicliamo finché la Board ci restituisce eventi
        while ((currentFinalEvent = game.getBoard().popNextVisibleEvent()) != null) {

            // Risolviamo l'evento (modifica i punteggi/cibo dei giocatori)
            currentFinalEvent.resolveEvent(game);

            game.notifyEventResolved();
        }

        // CALCOLO PUNTEGGI
        List<Player> winners = game.determineWinners();

        // NOTIFICA VITTORIA
        game.notifyEndGame(winners);
    }
}
