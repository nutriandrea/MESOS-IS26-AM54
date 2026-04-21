package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.rmi.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerConnection extends UnicastRemoteObject implements ServerConnection, RMIClientInterface {

    private RMIClientHandlerInterface serverStub;
    private ClientModel clientModel; // Necessario per aggiornare lo stato locale

    // ClientModel nel costruttore
    public RMIServerConnection(RMIGameServer server, ClientModel clientModel) throws RemoteException {
        this.clientModel = clientModel;
        // Ci registriamo sul server e otteniamo il nostro handler personale
        this.serverStub = server.connect(this);
    }

    /**
     * CLIENT -> SERVER
     */
    @Override
    public void sendRequest(Request request) {
        try {
            serverStub.handleRequest(request);
        } catch (RemoteException e) {
            disconnect();
        }
    }

    /**
     * SERVER -> CLIENT
     */
    @Override
    public void receiveUpdate(Update update) throws RemoteException {
        // AGGIUNTO: Smistamento identico a quello che avviene in ServerHandler per il TCP
        if (update instanceof PlayerJoinedUpdate msg) {
            clientModel.updatePlayerJoined(msg.getNickname());
        }
        else if (update instanceof TotemPlacedUpdate msg) {
            clientModel.updateTotemPlacement(msg.getNickname(), msg.getOfferPosition());
        }
        else if (update instanceof CardPickedUpdate msg) {
            clientModel.updateCardPicked(msg.getNickname(), msg.getRow(), msg.getCardIndex());
        }
        else if (update instanceof TurnChangedUpdate msg) {
            clientModel.updateTurn(msg.getNickname());
        }
        else if (update instanceof StateChangedUpdate msg) {
            clientModel.updateState(msg.getStateName());
        }
        else if (update instanceof TotemColorUpdate msg) {
            clientModel.updateTotemColor(msg.getNickname(), msg.getColor());
        }
        else if (update instanceof EndGameUpdate msg) {
            clientModel.updateEndGame(msg.getWinners());
        }
        else if (update instanceof ErrorMessageUpdate msg) {
            clientModel.updateError(msg.getMessage());
        }
    }

    @Override
    public void disconnect() {
        try {
            serverStub.disconnect();
        } catch (RemoteException ignored) {}
    }
}