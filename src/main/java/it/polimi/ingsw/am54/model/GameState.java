package it.polimi.ingsw.am54.model;

/**
 * Classe astratta che rappresenta lo stato del gioco.
 * Ogni metodo lancia IllegalStateException di default.
 * Gli stati concreti fanno override solo dei metodi permessi.
 */
public abstract class GameState {

    public void joinGame(Game game, String nickname) throws GameException {
        throw new IllegalStateException("Non puoi unirti alla partita in questa fase");
    }

    public void chooseTotemColor(Game game, String nickname, String color) throws GameException {
        throw new IllegalStateException("Non puoi scegliere il colore in questa fase");
    }

    public void setupGame(Game game) throws GameException {
        throw new IllegalStateException("Non puoi avviare il setup in questa fase");
    }

    public void placeTotem(Game game, String nickname, int offerPosition) throws GameException {
        throw new IllegalStateException("Non puoi piazzare il totem in questa fase");
    }

    public void pickCard(Game game, String nickname, String row, int cardIndex) throws GameException {
        throw new IllegalStateException("Non puoi pescare carte in questa fase");
    }

    public void endTurn(Game game, String nickname) throws GameException {
        throw new IllegalStateException("Non puoi terminare il turno in questa fase");
    }

    public void resolvePhase(Game game) throws GameException {
        throw new IllegalStateException("Non puoi risolvere la fase in questo momento");
    }

    public void skipTurn(Game game, String nickname) throws GameException {
        throw new IllegalStateException("Non puoi saltare il turno in questa fase");
    }

    public boolean isDisconnectBlocked() {
        return false;
    }

    public boolean isRemovePermanent() {
        return false;
    }

    public boolean isAutoResolvable() {
        return false;
    }

    public boolean isGameOver() {
        return false;
    }

    public boolean shouldResetToSetupOnEmpty() {
        return false;
    }

    public Player recoverActivePlayer(Game game) {
        return null;
    }

    public void handleDisconnection(Game game, Player p, boolean isCurrentPlayer) throws GameException {
    }
}
