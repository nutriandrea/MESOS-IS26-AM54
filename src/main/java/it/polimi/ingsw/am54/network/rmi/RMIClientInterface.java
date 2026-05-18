package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.EventUpdate;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import it.polimi.ingsw.am54.network.messages.Update;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RMIClientInterface extends Remote {
    // Il server usa questo metodo per inviare Update al client
    // SEVER -> CLIENT
    void onPlayerJoined(String nickname) throws RemoteException;
    void onTotemColorChosen(String nickname, TotemColors color) throws RemoteException;
    void onTotemPlaced(String nickname, int position) throws RemoteException;
    void onTotemReturned(String nickname, int food, int aura) throws RemoteException;
    void onEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats,
                         List<String> charUp, List<String> charLow, List<String> eventUp, List<String> eventLow,
                         List<String> buildUp, List<String> buildLow)  throws RemoteException;
    void onCardPicked(String nickname, String row, int cardIndex,
                      int newFood, int newMagic, Map<Integer, String> offering)
            throws RemoteException;
    void onTurnChanged(String nickname) throws RemoteException;
    void onStateChanged(String stateName, int era, int currentRound, Map<String, StateChangedUpdate.PlayerSnapshot> players,
                        List<String> turnOrder, Map<Integer, String> offering, List<String> charUp,
                        List<String> charLow, List<String> eventUp, List<String> eventLow,
                        List<String> buildUp, List<String> buildLow)
            throws RemoteException;
    void onEndGame(List<String> winnerNames) throws RemoteException;
    void onErrorMessage(String message) throws RemoteException;

    void onPlayerDisconnected(String nickname) throws RemoteException;
    void onPlayerReconnected(String nickname) throws RemoteException;

    void onGameSuspended() throws RemoteException;
    void onGameResumed() throws RemoteException;

    void ping() throws RemoteException; // Per l'heartbeat
}
