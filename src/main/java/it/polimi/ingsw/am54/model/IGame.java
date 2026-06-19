package it.polimi.ingsw.am54.model;
public interface IGame {
    // Azioni di Gioco
    void joinGame(String nickname) throws GameException;
    void chooseTotemColor(String nickname, String color) throws GameException;
    void placeTotem(String nickname, int offerPosition) throws GameException;
    // void pickCharacter(String nickname, int cardIndex) throws GameException;
    // void buyBuilding(String nickname, int cardIndex) throws GameException;
    void pickCard(String nickname, String row, int cardIndex) throws GameException;
    void endTurn(String nickname) throws GameException;

    void setupGame() throws GameException;

    // Gestione Observer
    // Permette alla UI o alla rete di "iscriversi" per ricevere aggiornamenti
    void addObserver(IGameObserver observer);
    void removeObserver(IGameObserver observer);
}