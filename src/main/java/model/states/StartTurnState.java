package model.states;

import model.Game;
import model.GameState;

/**
 * Stato di inizio turno: i giocatori piazzano il totem sulle offering tiles.
 */
public class StartTurnState extends GameState {

    @Override
    public void placeTotem(Game game, String nickname, int offerPosition) throws Exception {
        // TODO: piazza totem
        // TODO: se ultimo player ha piazzato:
        // game.setState(new PickingState());
    }
}
