package it.polimi.ingsw.am54.model;
public interface IGame {
    // Game Actions
    void joinGame(String nickname) throws Exception;
    void chooseTotemColor(String nickname, String color) throws Exception;
    void placeTotem(String nickname, int offerPosition) throws Exception;
    void pickCard(String nickname, String row, int cardIndex) throws Exception;
    void endTurn(String nickname) throws Exception;

    void setupGame() throws Exception;

    // Observer Management
    // Allows the UI or network to "subscribe" to receive updates
    void addObserver(IGameObserver observer);
    void removeObserver(IGameObserver observer);
}