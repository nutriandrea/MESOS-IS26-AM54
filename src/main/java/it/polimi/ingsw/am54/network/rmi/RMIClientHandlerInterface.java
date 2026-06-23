package it.polimi.ingsw.am54.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientHandlerInterface extends Remote {
    // RMI used to its fullest does not work like a socket,
    // but has a specific method for each action the client can call

    void joinGameRequest(String nickname) throws RemoteException;
    void setupGameRequest() throws RemoteException;
    void chooseTotemColorRequest(String color) throws RemoteException;
    void placeTotemRequest(int offerPosition) throws RemoteException;
    void pickCardRequest(String row, int cardIndex) throws RemoteException;
    void endTurnRequest() throws RemoteException;
    void getLeaderboardRequest(int numPlayers) throws RemoteException;

    void pong() throws RemoteException;


    void disconnect() throws RemoteException;
}