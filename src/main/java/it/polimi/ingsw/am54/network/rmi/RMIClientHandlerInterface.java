package it.polimi.ingsw.am54.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientHandlerInterface extends Remote {
    // RMI sfruttato al massimo non funziona come socket,
    // ma ha un metodo specifico per ogni azione che il client può chiamare

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
