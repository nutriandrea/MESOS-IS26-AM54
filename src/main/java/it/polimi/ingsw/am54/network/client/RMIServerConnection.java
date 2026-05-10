package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.rmi.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;


public class RMIServerConnection extends UnicastRemoteObject implements ServerConnection, RMIClientInterface {

    /** Stub RMI del server: usato per inviare Request e rispondere ai ping */
    private RMIClientHandlerInterface serverStub;

    /** Modello locale del client: viene aggiornato con gli update ricevuti */
    private ClientModel clientModel;

    /* =====================================================================
     * COSTRUTTORE
     * ===================================================================== */

    public RMIServerConnection(RMIGameServer server, ClientModel clientModel) throws RemoteException {
        this.clientModel = clientModel;
        // Ci registriamo sul server e otteniamo il nostro handler personale
        this.serverStub = server.connect(this);
    }

    /* =====================================================================
     * AZIONI IN USCITA (Dal Client al Server) - Implementa ServerConnection
     * Il giocatore interagisce con la GUI, il VirtualServer chiama questi
     * metodi, e li inoltriamo al server tramite RMI.
     * ===================================================================== */

    @Override
    public void joinGame(String nickname) {
        try { serverStub.joinGameRequest(nickname); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void setupGame() {
        try { serverStub.setupGameRequest(); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void chooseTotem(TotemColors color) {
        try { serverStub.chooseTotemColorRequest(color.name()); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void placeTotem(int position) {
        try { serverStub.placeTotemRequest(position); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void pickCard(String row, int index) {
        try { serverStub.pickCardRequest(row, index); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void endTurn() {
        try { serverStub.endTurnRequest(); }
        catch (RemoteException e) { disconnect(); }
    }

    /* =====================================================================
     * EVENTI IN INGRESSO (Dal Server al Client) - Implementa RMIClientInterface
     * Il Server chiama direttamente questi metodi, niente istanceof
     * ===================================================================== */

    @Override
    public void onPlayerJoined(String nickname) throws RemoteException {
        clientModel.updatePlayerJoined(nickname);
    }

    @Override
    public void onTotemColorChosen(String nickname, TotemColors color) throws RemoteException {
        clientModel.updateTotemColor(nickname, color);
    }

    @Override
    public void onTotemPlaced(String nickname, int position) throws RemoteException {
        clientModel.updateTotemPlacement(nickname, position);
    }

    @Override
    public void onTotemReturned(String nickname, int food, int aura) throws RemoteException {
        // Presupponendo che tu abbia o aggiunga questo metodo nel ClientModel
        clientModel.updateTotemReturned(nickname, food, aura);
    }

    @Override
    public void onCardPicked(String nickname, String row, int cardIndex,
                             int newFood, Map<Integer, String> offering) throws RemoteException {
        clientModel.updateCardPicked(nickname, row, cardIndex, newFood, offering);
    }

    @Override
    public void onTurnChanged(String nickname) throws RemoteException {
        clientModel.updateTurn(nickname);
    }

    @Override
    public void onEventResolved(Map<String, EventUpdate.PlayerSnapshot> stats,
                                List<String> charUp, List<String> charLow,
                                List<String> eventUp, List<String> eventLow,
                                List<String> buildUp, List<String> buildLow) throws RemoteException {
        // Ricostruiamo il DTO localmente solo per comodità
        EventUpdate localSnapshot = new EventUpdate(stats, charUp, charLow, eventUp, eventLow, buildUp, buildLow);
        clientModel.updateEventSnapshot(localSnapshot);
    }

    @Override
    public void onStateChanged(String stateName, int era, Map<String, StateChangedUpdate.PlayerSnapshot> players,
                               List<String> turnOrder, Map<Integer, String> offering,
                               List<String> charUp, List<String> charLow,
                               List<String> eventUp, List<String> eventLow,
                               List<String> buildUp, List<String> buildLow) throws RemoteException {
        // usiamo il DTO localmente per semplicità
        StateChangedUpdate localSnapshot = new StateChangedUpdate(stateName, era, players, turnOrder, offering, charUp, charLow, eventUp, eventLow, buildUp, buildLow);
        clientModel.updateState(localSnapshot);
    }

    @Override
    public void onEndGame(List<String> winnerNames) throws RemoteException {
        clientModel.updateEndGame(winnerNames);
    }

    @Override
    public void onErrorMessage(String message) throws RemoteException {
        clientModel.updateError(message);
    }

    @Override
    public void onPlayerDisconnected(String nickname) throws RemoteException {
        clientModel.updatePlayerDisconnected(nickname);
    }

    @Override
    public void onPlayerReconnected(String nickname) throws RemoteException {
        clientModel.updatePlayerReconnected(nickname);
    }

    /* =====================================================================
     * HEARTBEAT (Gestione del Ping/Pong)
     * ===================================================================== */

    @Override
    public void ping() throws RemoteException {
        try {
            // Risposta immediata al server per confermare che siamo vivi
            serverStub.pong();
        } catch (RemoteException e) {
            // Se non riusciamo a rispondere, il server e' giù
            disconnect();
        }
    }

    /* =====================================================================
     * DISCONNESSIONE
     * ===================================================================== */

    @Override
    public void disconnect() {
        try {
            serverStub.disconnect();
        } catch (RemoteException ignored) {}
    }
}
