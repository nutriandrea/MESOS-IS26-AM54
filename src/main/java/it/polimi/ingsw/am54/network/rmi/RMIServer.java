package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class RMIServer {
    private final int port;
    private final Game game;
    private final Controller controller;

    public RMIServer(int port) {
        this.port = port;
        this.game = new Game();
        this.controller = new Controller(game);
    }

    public void start() {
        try {
            LocateRegistry.createRegistry(port);
            
            RMIServerImplementation rmiServer = new RMIServerImplementation(game, controller);
            Naming.rebind("rmi://localhost:" + port + "/RMIServer", rmiServer);
            
            System.out.println("[RMI Server] Started on port " + port);
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("[RMI Server] Failed to start: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        System.out.println("=== AVVIO RMI SERVER ===");
        new RMIServer(port).start();
    }
}