package model.states;

import model.Game;
import model.GameState;

/**
 * Stato di risoluzione eventi: nessun giocatore puo' interagire.
 * Risolve automaticamente gli eventi e decide il prossimo stato.
 */
public class EventState extends GameState {

    @Override
    public void resolvePhase(Game game) throws Exception {
        // TODO: risolvi eventi nella lower row (Farmer ultimo)
        // TODO: scarta tribe cards dalla lower row
        // TODO: muovi upper -> lower
        // TODO: riempi upper
        // TODO: se deck vuoto: game.setState(new EndGameState());
        // TODO: altrimenti: game.setState(new StartTurnState());
    }
}
