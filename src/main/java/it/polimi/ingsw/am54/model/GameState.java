package it.polimi.ingsw.am54.model;

/**
 * Classe astratta che rappresenta lo stato del gioco.
 * Ogni metodo lancia IllegalStateException di default.
 * Gli stati concreti fanno override solo dei metodi permessi.
 */
public abstract class GameState {

    public void joinGame(Game game, String nickname) throws Exception {
        throw new IllegalStateException("Non puoi unirti alla partita in questa fase");
    }

    public void chooseTotemColor(Game game, String nickname, String color) throws Exception {
        throw new IllegalStateException("Non puoi scegliere il colore in questa fase");
    }

    public void setupGame(Game game) throws Exception {
        throw new IllegalStateException("Non puoi avviare il setup in questa fase");
    }

    public void placeTotem(Game game, String nickname, int offerPosition) throws Exception {
        throw new IllegalStateException("Non puoi piazzare il totem in questa fase");
    }

    public void pickCard(Game game, String nickname, String row, int cardIndex) throws Exception {
        throw new IllegalStateException("Non puoi pescare carte in questa fase");
    }

    public void endTurn(Game game, String nickname) throws Exception {
        throw new IllegalStateException("Non puoi terminare il turno in questa fase");
    }

    public void resolvePhase(Game game) throws Exception {
        throw new IllegalStateException("Non puoi risolvere la fase in questo momento");
    }

    /**
     * Salta il turno del giocatore disconnesso.
     * Il comportamento dipende dallo stato corrente:
     * - StartTurnState: marca hasPlacedTotem=true e avanza
     * - PickingState: termina automaticamente il turno di pesca
     * - Altri stati: non permesso (eccezione)
     */
    public void skipTurn(Game game, String nickname) throws Exception {
        throw new IllegalStateException("Non puoi saltare il turno in questa fase");
    }
}
