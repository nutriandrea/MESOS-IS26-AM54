package model;

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

    public void pickCharacter(Game game, String nickname, int cardIndex) throws Exception {
        throw new IllegalStateException("Non puoi pescare un personaggio in questa fase");
    }

    public void buyBuilding(Game game, String nickname, int cardIndex) throws Exception {
        throw new IllegalStateException("Non puoi comprare un edificio in questa fase");
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
}
