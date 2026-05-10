package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.model.TotemColors;

/**
 * Offre alla View i metodi per interagire con il gioco.
 * Trasforma le chiamate ai metodi in oggetti Request inviati sulla rete.
 */
public class VirtualServer {
    private final ServerConnection connection;
    private final String nickname;

    public VirtualServer(ServerConnection connection, String nickname) {
        this.connection = connection;
        this.nickname = nickname;
    }


    public void joinGame() {
        connection.joinGame(nickname);
    }

    public void setupGame() {
        connection.setupGame();
    }

    public void chooseTotem(TotemColors color) {
        connection.chooseTotem(color);
    }

    public void placeTotem(int position) {
        connection.placeTotem(position);
    }

    public void pickCard(String row, int index) {
        connection.pickCard(row, index);
    }

    public void endTurn() {
        connection.endTurn();
    }

}
