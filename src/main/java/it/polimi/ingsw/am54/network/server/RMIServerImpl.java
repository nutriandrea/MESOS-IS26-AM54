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
     * Punto d'ingresso RMI: registra un nuovo client (passando il suo stub di callback)
     * e gli restituisce un handler personale per inviare le proprie azioni di gioco.
     *
     * @param clientStub lo stub di callback del client (per ricevere gli update)
     * @return l'handler RMI dedicato a quel client
     * @throws RemoteException in caso di errore RMI
     */
    @Override
    public RMIClientHandlerInterface connect(RMIClientInterface clientStub) throws RemoteException {
        System.out.println("[Server RMI] Nuova connessione accettata.");
        return new RMIClientHandler(clientStub, server);
    }
}