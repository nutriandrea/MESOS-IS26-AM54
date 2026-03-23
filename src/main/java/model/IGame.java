package model;
public interface IGame {
    // Azioni di Gioco
    void joinGame(String nickname) throws Exception;
    void chooseTotemColor(String nickname, String color) throws Exception;
    void placeTotem(String nickname, int offerPosition) throws Exception;
    void pickCharacter(String nickname, int cardIndex) throws Exception;
    void buyBuilding(String nickname, int cardIndex) throws Exception;

    // Gestione Observer
    // Permette alla UI o alla rete di "iscriversi" per ricevere aggiornamenti
    void addObserver(IGameObserver observer);
}