package model.states;

import model.Game;
import model.GameState;

/**
 * Stato di setup: i giocatori si uniscono, scelgono il colore e avviano la partita.
 */
public class SetUpGameState extends GameState {

    @Override
    public void joinGame(Game game, String nickname) throws Exception {
        // TODO
    }

    @Override
    public void chooseTotemColor(Game game, String nickname, String color) throws Exception {
        // TODO
    }

    @Override
    public void setupGame(Game game) throws Exception {
        // TODO: crea board, deck, distribuisci cibo
        game.setState(new StartTurnState());
    }
}
