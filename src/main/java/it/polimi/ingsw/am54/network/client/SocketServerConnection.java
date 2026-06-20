package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import java.io.*;
import java.net.Socket;

/**
 * Implementation of the client connection via TCP Socket.
 * Creates a Socket to the server, opens the streams, and starts a
 * ServerHandler in a separate thread for asynchronous reception.
 */
public class SocketServerConnection implements ServerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected;
    private ServerHandler serverHandler;

    /**
     * Constructs a TCP socket connection to the server and starts the
     * background listening thread.
     *
     * @param host       the server host name or IP
     * @param port       the server TCP port
     * @param clientModel the local model to update with incoming messages
     * @throws IOException if the socket cannot be opened
     */
    public SocketServerConnection(String host, int port, ClientModel clientModel) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.connected = true;
        this.serverHandler = new ServerHandler(this.in, this.out, clientModel);

        new Thread(this.serverHandler).start();
    }

    // private method to send data on the stream more cleanly
    private void sendObject(Object request) {
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
        } catch (IOException e) {
            disconnect();
        }
    }

    //Handle all requests
    /**
     * Sends a join-game request.
     *
     * @param nickname the nickname to join with
     */
    @Override
    public void joinGame(String nickname) {
        sendObject(new JoinGameRequest(nickname));
    }

    /** Sends a setup-game request. */
    @Override
    public void setupGame() {
        sendObject(new SetupGameRequest());
    }

    /**
     * Sends a choose-totem-color request.
     *
     * @param color the chosen totem color
     */
    @Override
    public void chooseTotem(TotemColors color) {
        sendObject(new ChooseTotemRequest(color));
    }

    /**
     * Sends a place-totem request.
     *
     * @param position the offering tile index
     */
    @Override
    public void placeTotem(int position) {
        sendObject(new PlaceTotemRequest(position));
    }

    /**
     * Sends a pick-card request.
     *
     * @param row   "upper" or "lower"
     * @param index the card index within the row
     */
    @Override
    public void pickCard(String row, int index) {
        sendObject(new PickCardRequest(row, index));
    }

    /** Sends an end-turn request. */
    @Override
    public void endTurn() {
        sendObject(new EndTurnRequest());
    }

    /**
     * Sends a leaderboard request.
     *
     * @param numPlayers the number-of-players filter
     */
    @Override
    public void requestLeaderboard(int numPlayers) {
        sendObject(new GetLeaderboardRequest(numPlayers));
    }

    /** Closes the socket and stops the listening thread. */
    @Override
    public void disconnect() {
        connected = false;
        // First stop the listening thread cleanly
        if (serverHandler != null) {
            serverHandler.stop();
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}