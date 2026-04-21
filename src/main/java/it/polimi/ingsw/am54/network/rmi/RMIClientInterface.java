package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.network.messages.Update;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientInterface extends Remote {
    // Il server usa questo metodo per inviare Update al client
    // SEVER -> CLIENT
    void receiveUpdate(Update update) throws RemoteException;
}
