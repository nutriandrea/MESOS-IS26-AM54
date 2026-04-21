package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface;
import it.polimi.ingsw.am54.network.rmi.RMIClientInterface;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerImpl extends UnicastRemoteObject implements RMIGameServer {
    private final Game game;
    private final Controller controller;

    public RMIServerImpl(Game game, Controller controller) throws RemoteException {
        super();
        this.game = game;
        this.controller = controller;
    }

    @Override
    public RMIClientHandlerInterface connect(RMIClientInterface clientStub) throws RemoteException {
        System.out.println("[Server RMI] Nuova connessione accettata.");
        // Crea solo l'handler e passalo al client.
        // La VirtualView nascerà dopo, nel Controller.
        return new RMIClientHandler(clientStub, controller);
    }
}
