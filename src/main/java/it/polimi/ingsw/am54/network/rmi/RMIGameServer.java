package it.polimi.ingsw.am54.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIGameServer extends Remote {
    // The client connects, passes itself (its stub) and receives its personal handler in return
    RMIClientHandlerInterface connect(RMIClientInterface clientStub) throws RemoteException;
}