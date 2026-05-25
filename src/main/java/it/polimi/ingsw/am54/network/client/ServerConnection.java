package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.Request;

/**
 * Interfaccia generica per la connessione verso il Server.
 * Nasconde se stiamo usando Socket o RMI.
 */
public interface ServerConnection {
    // Invia messaggi al server, un metodo per tipo, in stile RMI
    void joinGame(String nickname);
    void setupGame();
    void chooseTotem(TotemColors color);
    void placeTotem(int position);
    void pickCard(String row, int index);
    void endTurn();
    void requestLeaderboard(int numPlayers);

    // Chiude la connessione col server
    void disconnect();
}
