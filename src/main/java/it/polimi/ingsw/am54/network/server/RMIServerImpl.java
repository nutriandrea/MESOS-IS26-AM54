package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface;
import it.polimi.ingsw.am54.network.rmi.RMIClientInterface;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerImpl extends UnicastRemoteObject implements RMIGameServer {

    private final Server server;

    public RMIServerImpl(Server server) throws RemoteException {
        super();
        this.server = server;
    }

    /**
     * RMI entry point: registers a new client (passing its callback stub)
     * and returns a personal handler for it to send its game actions.
     *
     * @param clientStub the client's callback stub (to receive updates)
     * @return the RMI handler dedicated to that client
     * @throws RemoteException in case of RMI error
     */
    @Override
    public RMIClientHandlerInterface connect(RMIClientInterface clientStub) throws RemoteException {
        System.out.println("[Server RMI] Nuova connessione accettata.");
        return new RMIClientHandler(clientStub, server);
    }
}