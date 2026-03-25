package model.states;

import model.Game;
import model.GameState;

/**
 * Stato di fine partita: calcola i punteggi finali e decreta il vincitore.
 * Nessun giocatore puo' pescare o piazzare totem.
 */
public class EndGameState extends GameState {

    @Override
    public void resolvePhase(Game game) throws Exception {
        // TODO: calcola punteggi finali
        // TODO: risolvi effetti ENDGAME dei building
        // TODO: bonus Inventor PP, Artist PP
        // TODO: trasforma risorse avanzate in punti
        // TODO: decreta vincitore
        // TODO: notifica Observer per schermata vittoria
    }
}
