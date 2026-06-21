package it.polimi.ingsw.am54.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote RMI Interface for the client's initial connection.
 * The client calls connect() passing its own stub to receive
 * a personal RMIClientHandlerInterface to send requests on.
 */
public interface RMIGameServer extends Remote {
    /**
     * Accepts a new client connection, registering the client stub and
     * returning a dedicated handler for sending game requests.
     *
     * @param clientStub the remote client stub for server-to-client callbacks
     * @return a personal {@link RMIClientHandlerInterface} for the client
     * @throws RemoteException if the RMI call fails
     */
    RMIClientHandlerInterface connect(RMIClientInterface clientStub) throws RemoteException;
}
