package model.states;

import model.*;

/**
 * Stato di risoluzione eventi: nessun giocatore puo' interagire.
 * Risolve automaticamente gli eventi (normali prima, Farmer per ultimi) e decide il prossimo stato.
 */
public class EventState extends GameState {

    @Override
    public void resolvePhase(Game game) throws Exception {
        // 1. Risolvi eventi (ordina: non-Farmer prima, Farmer dopo)
        game.getBoard().resolveAllEventsInOrder(game);

        // 2. Pulisci caratteri dalla lower row
        game.getBoard().clearLowerRow();

        // 3. Sposta upper -> lower
        game.getBoard().moveUpperToLower();

        // 4. Riempi upper row
        game.getBoard().refillUpperRow(game);

        // 5. Sposta buildings SOLO se era transition
        if (game.isEraTransition()) {
            game.getBoard().moveBuildings();
            game.getBoard().populateUpperRowBuildings(game);
        }

        // 7. Decidi prossimo stato
        game.advanceRound();
        if (game.isTribeDeckEmpty()) {
            game.setState(new EndGameState());
        } else {
            game.setState(new StartTurnState());
        }
    }
}
