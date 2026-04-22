package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.network.messages.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientHandlerInterface extends Remote {
    // Il client usa questo metodo per inviare Request (comandi) al server
    void handleRequest(Request request) throws RemoteException;
    void disconnect() throws RemoteException;
}
