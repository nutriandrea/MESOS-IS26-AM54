package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface;
import it.polimi.ingsw.am54.network.rmi.RMIClientInterface;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementazione RMI del server esportata nel Registry.
 * Riceve le connessioni dei client RMI e crea un RMIClientHandler dedicato.
 */
public class RMIServerImpl extends UnicastRemoteObject implements RMIGameServer {

    private final Server server;

    public RMIServerImpl(Server server) throws RemoteException {
        super();
        this.server = server;
    }

    @Override
    public RMIClientHandlerInterface connect(RMIClientInterface clientStub) throws RemoteException {
        System.out.println("[Server RMI] Nuova connessione accettata.");
        return new RMIClientHandler(clientStub, server);
    }
}