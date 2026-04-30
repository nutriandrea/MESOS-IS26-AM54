package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface;
import it.polimi.ingsw.am54.network.rmi.RMIClientInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIClientHandler extends UnicastRemoteObject implements RMIClientHandlerInterface, ClientConnection, ClientHandler {
    private final RMIClientInterface clientStub;
    private final Controller controller;
    private String playerNickname;
    private boolean connected;

    public RMIClientHandler(RMIClientInterface clientStub, Controller controller) throws RemoteException {
        super();
        this.clientStub = clientStub;
        this.controller = controller;
        this.connected = true;
    }

    @Override
    public void handleRequest(Request request) throws RemoteException {
        if (!connected) return;

        if (request instanceof JoinGameRequest req) {
            this.playerNickname = req.getNickname();
            controller.handleJoinGame(this.playerNickname, this);
        } else if (request instanceof SetupGameRequest) {
            controller.handleSetupGame(this.playerNickname);
        } else if (request instanceof ChooseTotemRequest req) {
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
    public void sendUpdate(Update update) {
        if (!connected) return;
        try {
            clientStub.receiveUpdate(update);
        } catch (RemoteException e) {
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
        this.connected = false;
        if (playerNickname != null) {
            controller.removeClient(playerNickname);
        }
    }
}
