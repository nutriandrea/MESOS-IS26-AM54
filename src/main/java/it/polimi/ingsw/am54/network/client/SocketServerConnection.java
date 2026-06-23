package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import java.io.*;
import java.net.Socket;

public class SocketServerConnection implements ServerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected;
    private ServerHandler serverHandler;

    /**
     * Opens the TCP connection to the server: creates the socket, initializes the
     * object streams and starts the {@link ServerHandler} thread listening for incoming updates.
     *
     * @param host        the server address
     * @param port        the server's TCP port
     * @param clientModel the local model to update with received updates
     * @throws IOException if the connection or stream opening fails
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

    /**
     * Serializes and sends a Request to the server on the output stream; in case of I/O error
     * closes the connection (triggering auto-reconnection).
     *
     * @param request the Request object to send
     */
    private void sendObject(Object request) {
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
        } catch (IOException e) {
            disconnect();
        }
    }

    // We handle all requests
    @Override
    public void joinGame(String nickname) {
        sendObject(new JoinGameRequest(nickname));
    }

    @Override
    public void setupGame() {
        sendObject(new SetupGameRequest());
    }

    @Override
    public void chooseTotem(TotemColors color) {
        sendObject(new ChooseTotemRequest(color));
    }

    @Override
    public void placeTotem(int position) {
        sendObject(new PlaceTotemRequest(position));
    }

    @Override
    public void pickCard(String row, int index) {
        sendObject(new PickCardRequest(row, index));
    }

    @Override
    public void endTurn() {
        sendObject(new EndTurnRequest());
    }

    @Override
    public void requestLeaderboard(int numPlayers) {
        sendObject(new GetLeaderboardRequest(numPlayers));
    }

    @Override
    public void disconnect() {
        connected = false;
        // We stop the listening thread cleanly first
        if (serverHandler != null) {
            serverHandler.stop();
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}