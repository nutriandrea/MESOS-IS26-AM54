package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.network.messages.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIServerInterface extends Remote {

    String connect(String nickname) throws RemoteException;

    void chooseTotemColor(String nickname, String color) throws RemoteException;

    void setupGame(String nickname) throws RemoteException;

    void placeTotem(String nickname, int offerPosition) throws RemoteException;

    void pickCard(String nickname, String row, int cardIndex) throws RemoteException;

    void reconnect(String nickname) throws RemoteException;

    void disconnect(String nickname) throws RemoteException;

    String[] getConnectedPlayers() throws RemoteException;

    String getGameState() throws RemoteException;

    String getCurrentPlayer() throws RemoteException;
}