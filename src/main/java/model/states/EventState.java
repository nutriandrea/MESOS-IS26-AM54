package model.states;

import model.*;

/**
 * Stato di risoluzione eventi: nessun giocatore puo' interagire.
 * Risolve automaticamente gli eventi (normali prima, Farmer per ultimi) e decide il prossimo stato.
 */
public class EventState extends GameState {

    @Override
    public void resolvePhase(Game game) throws Exception {
        game.getBoard().resolveAllEventsInOrder(game);

        game.getBoard().clearLowerRow();

        game.getBoard().moveUpperToLower();

        try {
            game.getBoard().refillUpperRow(game);
        } catch (Exception e) {
            // Tribe deck exhausted, continue without refilling
        }

        if (game.isEraTransition()) {
            game.getBoard().moveBuildings();
            game.getBoard().populateUpperRowBuildings(game);
        }

        game.advanceRound();
        if (game.isTribeDeckEmpty()) {
            game.setState(new EndGameState());
        } else {
            game.setState(new StartTurnState());
        }
    }
}
