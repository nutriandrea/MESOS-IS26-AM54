package model.states;

import model.Game;
import model.GameState;

/**
 * Stato di pesca: i giocatori pescano personaggi, comprano edifici e terminano il turno.
 */
public class PickingState extends GameState {

    @Override
    public void pickCharacter(Game game, String nickname, int cardIndex) throws Exception {
        // TODO: pesca personaggio
    }

    @Override
    public void buyBuilding(Game game, String nickname, int cardIndex) throws Exception {
        // TODO: compra edificio
    }

    @Override
    public void endTurn(Game game, String nickname) throws Exception {
        // TODO: fine turno
        // TODO: se ultimo player ha finito:
        // game.setState(new EventState());
    }
}
