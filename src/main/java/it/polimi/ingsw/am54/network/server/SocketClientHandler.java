package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.messages.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClientHandler implements Runnable, ClientHandler, ClientConnection {
    private final Socket socket;
    private final Controller controller;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connected;
    private String playerNickname;

    public SocketClientHandler(Socket socket, Controller controller) {
        this.socket = socket;
        this.controller = controller;
        this.connected = true;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server TCP] Nuovo client in ascolto.");

            while (connected) {
                Object input = in.readObject();
                if (input instanceof Request) {
                    handleRequest((Request) input);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server TCP] Client disconnesso.");
        } finally {
            disconnect();
        }
    }

    private void handleRequest(Request request) {
        if (request instanceof JoinGameRequest req) {
            this.playerNickname = req.getNickname();
            controller.handleJoinGame(this.playerNickname, this);
        } else if (request instanceof SetupGameRequest) {
            controller.handleSetupGame(this.playerNickname);
        } else if (request instanceof ChooseTotemRequest req) {
        // Aggiungi .name() per convertire l'Enum in Stringa!
            controller.handleChooseTotemColor(this.playerNickname, req.getColor().name());
        } else if (request instanceof PlaceTotemRequest req) {
            controller.handlePlaceTotem(this.playerNickname, req.getOfferPosition());
        } else if (request instanceof PickCardRequest req) {
            controller.handlePickCard(this.playerNickname, req.getRow(), req.getCardIndex());
        } else if (request instanceof EndTurnRequest) {
            controller.handleEndTurn(this.playerNickname);
        }
    }

    @Override
    public synchronized void sendUpdate(Update update) {
        if (!connected) return;
        try {
            out.writeObject(update);
            out.flush();
            out.reset();
        } catch (IOException e) {
            disconnect();
        }
    }

    @Override
    public void sendError(String errorMessage) {
        sendUpdate(new ErrorMessageUpdate(errorMessage));
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        if (playerNickname != null) {
            controller.removeClient(playerNickname);
        }
    }
}