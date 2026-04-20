package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.Request;
import it.polimi.ingsw.am54.network.messages.Update;
import it.polimi.ingsw.am54.network.rmi.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerConnection extends UnicastRemoteObject implements ServerConnection, RMIClientInterface {
    private RMIClientHandlerInterface serverStub;

    public RMIServerConnection(RMIGameServer server) throws RemoteException {
        // Ci registriamo sul server e otteniamo il nostro handler personale
        this.serverStub = server.connect(this);
    }

    @Override
    public void sendRequest(Request request) {
        try {
            serverStub.sendRequest(request);
        } catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void receiveUpdate(Update update) throws RemoteException {
        // Smistamento alla view identico a quello TCP
    }

    @Override
    public void disconnect() {
        try { serverStub.disconnect(); } catch (RemoteException ignored) {}
    }
}
