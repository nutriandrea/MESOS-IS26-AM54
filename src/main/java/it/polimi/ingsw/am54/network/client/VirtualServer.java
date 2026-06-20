package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.model.TotemColors;

/**
 * Client-side facade for sending actions to the server.
 * Transforms each call into a Request object (Socket) or into
 * a direct RMI call via ServerConnection.
 * Used by GUI and CLI to decouple the view from the network.
 */
public class VirtualServer {
    private final ServerConnection connection;
    private final String nickname;

    /**
     * Constructs a VirtualServer bound to the given connection and nickname.
     *
     * @param connection the underlying server connection
     * @param nickname   the player's nickname
     */
    public VirtualServer(ServerConnection connection, String nickname) {
        this.connection = connection;
        this.nickname = nickname;
    }


    /** Sends a join-game request using the stored nickname. */
    public void joinGame() {
        connection.joinGame(nickname);
    }

    /** Sends a setup-game request. */
    public void setupGame() {
        connection.setupGame();
    }

    /**
     * Sends a choose-totem-color request.
     *
     * @param color the chosen totem color
     */
    public void chooseTotem(TotemColors color) {
        connection.chooseTotem(color);
    }

    /**
     * Sends a place-totem request.
     *
     * @param position the offering tile index
     */
    public void placeTotem(int position) {
        connection.placeTotem(position);
    }

    /**
     * Sends a pick-card request.
     *
     * @param row   "upper" or "lower"
     * @param index the card index within the row
     */
    public void pickCard(String row, int index) {
        connection.pickCard(row, index);
    }

    /** Sends an end-turn request. */
    public void endTurn() {
        connection.endTurn();
    }

    /**
     * Sends a leaderboard request.
     *
     * @param numPlayers the number-of-players filter
     */
    public void requestLeaderboard(int numPlayers) {
        connection.requestLeaderboard(numPlayers);
    }

}
