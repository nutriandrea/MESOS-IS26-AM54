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

    /**
     * Metodo di supporto per gestire il crash del server in RMI.
     * Segnala la caduta della rete al Modello in modo disaccoppiato.
     */
    private void handleServerCrash() {
        if (this.serverStub == null) return;
        System.err.println("[RMI] Connessione persa, avviso il modello...");
        clientModel.triggerConnectionLost();
        // Sganciamo lo stub rotto
        this.serverStub = null;
    }

    /* =====================================================================
     * AZIONI IN USCITA (Dal Client al Server) - Implementa ServerConnection
     * Il giocatore interagisce con la GUI, il VirtualServer chiama questi
     * metodi, e li inoltriamo al server tramite RMI.
     * ===================================================================== */

    @Override
    public void joinGame(String nickname) {
        try { serverStub.joinGameRequest(nickname); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    @Override
    public void setupGame() {
        try { serverStub.setupGameRequest(); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    @Override
    public void chooseTotem(TotemColors color) {
        try { serverStub.chooseTotemColorRequest(color.name()); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    @Override
    public void placeTotem(int position) {
        try { serverStub.placeTotemRequest(position); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    @Override
    public void pickCard(String row, int index) {
        try { serverStub.pickCardRequest(row, index); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    @Override
    public void endTurn() {
        try { serverStub.endTurnRequest(); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    @Override
    public void requestLeaderboard(int numPlayers) {
        try { serverStub.getLeaderboardRequest(numPlayers); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
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
        clientModel.updateTotemReturned(nickname, food, aura);
    }

    @Override
    public void onCardPicked(String nickname, String row, int cardIndex,
                             int newFood, int newMagic, Map<Integer, String> offering) throws RemoteException {
        clientModel.updateCardPicked(nickname, row, cardIndex, newFood, newMagic, offering);
    }

    @Override
    public void onTurnChanged(String nickname) throws RemoteException {
        clientModel.updateTurn(nickname);
    }

    @Override
    public void onEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats,
                                List<String> charUp, List<String> charLow,
                                List<String> eventUp, List<String> eventLow,
                                List<String> buildUp, List<String> buildLow) throws RemoteException {
        // Ricostruiamo il DTO localmente solo per comodità
        EventUpdate localSnapshot = new EventUpdate(name,stats, charUp, charLow, eventUp, eventLow, buildUp, buildLow);
        clientModel.updateEventSnapshot(localSnapshot);
    }

    @Override
    public void onStateChanged(String stateName, int era, int currentRound, Map<String, StateChangedUpdate.PlayerSnapshot> players,
                               List<String> turnOrder, Map<Integer, String> offering,
                               List<String> charUp, List<String> charLow,
                               List<String> eventUp, List<String> eventLow,
                               List<String> buildUp, List<String> buildLow) throws RemoteException {
        String oldState = clientModel.getCurrentState();

        StateChangedUpdate localSnapshot = new StateChangedUpdate(stateName, era, currentRound, players, turnOrder, offering, charUp, charLow, eventUp, eventLow, buildUp, buildLow);
        clientModel.updateState(localSnapshot);

        String newState = clientModel.getCurrentState();

        if (oldState != null && newState != null) {
            if (("SetUpGameState".equals(oldState) || "Lobby".equals(oldState)) &&
                    (!"SetUpGameState".equals(newState) && !"Lobby".equals(newState))) {
                try {
                    it.polimi.ingsw.am54.gui.GUIApp.loadBoardScene();
                } catch (Exception | NoClassDefFoundError e) {
                    // Modalità CLI attiva, transizione GUI ignorata
                }
            }
        }
    }

    @Override
    public void onEndGame(EndGameUpdate update) throws RemoteException {
        clientModel.updateEndGame(
                update.getWinners(),
                update.getAllScores(),
                update.getNumPlayers()
        );
    }

    @Override
    public void onErrorMessage(String message) throws RemoteException {
        clientModel.updateError(message);
    }

    @Override
    public void onLeaderboardUpdate(LeaderboardUpdate update) throws RemoteException {
        clientModel.updateLeaderboard(update.getPlayerPosition(), update.getEntries(), update.getNumPlayers());
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
    public void onGameSuspended() throws RemoteException {
        clientModel.updateGameSuspended();
    }

    @Override
    public void onGameResumed() throws RemoteException {
        clientModel.updateGameResumed();
    }

    @Override
    public void ping() throws RemoteException {
        try {
            // Risposta immediata al server per confermare che siamo vivi
            serverStub.pong();
        } catch (RemoteException | NullPointerException e) {
            // Se non riusciamo a rispondere, il server e' giù
            handleServerCrash();
        }
    }

    /* =====================================================================
     * DISCONNESSIONE VOLONTARIA
     * ===================================================================== */

    @Override
    public void disconnect() {
        try {
            if (serverStub != null) {
                serverStub.disconnect();
            }
        } catch (RemoteException ignored) {}
    }
}