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

    /**
     * L'unica richiesta che DEVE contenere il nickname per presentarsi al server.
     */
    public void joinGame() {
        connection.sendRequest(new JoinGameRequest(nickname));
    }

    /**
     * SetupGameRequest non ha parametri nel tuo file.
     */
    public void setupGame() {
        connection.sendRequest(new SetupGameRequest());
    }

    /**
     * ChooseTotemRequest accetta solo il colore (TotemColors color).
     */
    public void chooseTotem(TotemColors color) {
        connection.sendRequest(new ChooseTotemRequest(color));
    }

    /**
     * PlaceTotemRequest accetta solo la posizione (int offerPosition).
     */
    public void placeTotem(int position) {
        connection.sendRequest(new PlaceTotemRequest(position));
    }

    /**
     * PickCardRequest accetta solo (String row, int cardIndex).
     */
    public void pickCard(String row, int index) {
        connection.sendRequest(new PickCardRequest(row, index));
    }

    // Nel file VirtualServer.java
    public void endTurn() {
        connection.sendRequest(new EndTurnRequest());
    }
}
