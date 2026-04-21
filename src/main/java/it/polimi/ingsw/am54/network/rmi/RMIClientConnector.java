package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.network.messages.ClientMessage;
import it.polimi.ingsw.am54.network.messages.Update;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClientConnector {
    private RMIServerInterface server;
    private String nickname;
    private volatile boolean connected = false;

    public boolean connect(String host, int port, String nickname) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            server = (RMIServerInterface) registry.lookup("RMIServer");
            
            String result = server.connect(nickname);
            if (result != null && result.startsWith("CONNECTED:")) {
                this.nickname = nickname;
                this.connected = true;
                System.out.println("[RMI] Connected as " + nickname);
                return true;
            }
            return false;
        } catch (RemoteException | NotBoundException e) {
            System.err.println("[RMI] Connection failed: " + e.getMessage());
            return false;
        }
    }

    public void chooseTotemColor(String color) {
        if (!connected) return;
        try {
            server.chooseTotemColor(nickname, color);
        } catch (RemoteException e) {
            System.err.println("[RMI] Error: " + e.getMessage());
        }
    }

    public void setupGame() {
        if (!connected) return;
        try {
            server.setupGame(nickname);
        } catch (RemoteException e) {
            System.err.println("[RMI] Error: " + e.getMessage());
        }
    }

    public void placeTotem(int position) {
        if (!connected) return;
        try {
            server.placeTotem(nickname, position);
        } catch (RemoteException e) {
            System.err.println("[RMI] Error: " + e.getMessage());
        }
    }

    public void pickCard(String row, int index) {
        if (!connected) return;
        try {
            server.pickCard(nickname, row, index);
        } catch (RemoteException e) {
            System.err.println("[RMI] Error: " + e.getMessage());
        }
    }

    public void reconnect() {
        if (!connected) return;
        try {
            server.reconnect(nickname);
        } catch (RemoteException e) {
            System.err.println("[RMI] Error: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (!connected) return;
        try {
            server.disconnect(nickname);
            connected = false;
        } catch (RemoteException e) {
            System.err.println("[RMI] Error: " + e.getMessage());
        }
    }

    public String[] getConnectedPlayers() {
        try {
            return server.getConnectedPlayers();
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public String getGameState() {
        try {
            return server.getGameState();
        } catch (RemoteException e) {
            return "UNKNOWN";
        }
    }

    public boolean isConnected() {
        return connected;
    }
}