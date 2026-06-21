package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface;
import it.polimi.ingsw.am54.network.rmi.RMIClientInterface;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMI implementation of the server exported in the Registry.
 * Receives RMI client connections and creates a dedicated RMIClientHandler.
 */
public class RMIServerImpl extends UnicastRemoteObject implements RMIGameServer {

    private final Server server;

    /**
     * Constructs the RMI server implementation bound to the given server.
     *
     * @param server the game server instance
     * @throws RemoteException if the remote object cannot be exported
     */
    public RMIServerImpl(Server server) throws RemoteException {
        super();
        this.server = server;
    }

    /** {@inheritDoc} */
    @Override
    public RMIClientHandlerInterface connect(RMIClientInterface clientStub) throws RemoteException {
        System.out.println("[Server RMI] Nuova connessione accettata.");
        return new RMIClientHandler(clientStub, server);
    }
}