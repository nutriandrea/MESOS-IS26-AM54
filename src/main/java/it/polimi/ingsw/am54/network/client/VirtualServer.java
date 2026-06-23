package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.model.TotemColors;

/**
 * Offers methods to the View to interact with the game.
 * Transforms method calls into Request objects sent over the network.
 */
public class VirtualServer {
    private final String nickname;

    public VirtualServer(ServerConnection connection, String nickname) {
        // NB: the connection is NOT stored here. After an automatic reconnection
        // the Client replaces the live connection in the singleton; if we
        // kept a "frozen" reference to the constructor, every move would be
        // sent to the old dead connection and silently lost.
        this.nickname = nickname;
    }

    /** ALWAYS returns the currently live connection (updated after reconnections). */
    private ServerConnection connection() {
        return Client.getInstance().getConnection();
    }

    public void joinGame() {
        connection().joinGame(nickname);
    }

    public void setupGame() {
        connection().setupGame();
    }

    public void chooseTotem(TotemColors color) {
        connection().chooseTotem(color);
    }

    public void placeTotem(int position) {
        connection().placeTotem(position);
    }

    public void pickCard(String row, int index) {
        connection().pickCard(row, index);
    }

    public void endTurn() {
        connection().endTurn();
    }

    public void requestLeaderboard(int numPlayers) {
        connection().requestLeaderboard(numPlayers);
    }

}