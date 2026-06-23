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
     * Apre la connessione TCP verso il server: crea il socket, inizializza gli stream di
     * oggetti e avvia il thread {@link ServerHandler} che ascolta gli update in arrivo.
     *
     * @param host        l'indirizzo del server
     * @param port        la porta TCP del server
     * @param clientModel il modello locale da aggiornare con gli update ricevuti
     * @throws IOException se la connessione o l'apertura degli stream fallisce
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
     * Serializza e invia una Request al server sullo stream di output; in caso di errore
     * di I/O chiude la connessione (innescando l'auto-riconnessione).
     *
     * @param request l'oggetto Request da inviare
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

    //Gestiamo tutte le richieste
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
        // Fermiamo prima il thread in ascolto in modo pulito
        if (serverHandler != null) {
            serverHandler.stop();
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}