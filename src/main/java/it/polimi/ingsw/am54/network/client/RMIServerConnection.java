package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.rmi.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RMIServerConnection extends UnicastRemoteObject implements ServerConnection, RMIClientInterface {

    private RMIClientHandlerInterface serverStub;
    private ClientModel clientModel;

    // Timer to proactively check if the server is alive
    private Timer connectionWatcher;

    /**
     * Establishes the RMI connection: registers with the server passing itself as a
     * callback stub, receives its personal handler and starts the watcher that monitors
     * the server's vitality.
     *
     * @param server      the remote stub of the game server
     * @param clientModel the local model to update with received callbacks
     * @throws RemoteException in case of RMI error
     */
    public RMIServerConnection(RMIGameServer server, ClientModel clientModel) throws RemoteException {
        this.clientModel = clientModel;
        this.serverStub = server.connect(this);

        // Start the Watcher
        startConnectionWatcher();
    }

    /**
     * Starts a periodic check (every 2 s) that "pings" the server: if the remote
     * call fails, the server is considered down and the crash is handled.
     */
    private void startConnectionWatcher() {
        connectionWatcher = new Timer("RMI-Watcher", true);
        connectionWatcher.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (serverStub != null) {
                    try {
                        // Dummy ping to the server. If the server is dead,
                        // this will throw a RemoteException and we will catch the crash!
                        serverStub.pong(); // We use 'pong()' or a generic empty method on the server
                    } catch (RemoteException e) {
                        handleServerCrash();
                        connectionWatcher.cancel();
                    }
                } else {
                    connectionWatcher.cancel();
                }
            }
        }, 2000, 2000); // Check every 2 seconds
    }

    /**
     * Handles the server crash: notifies the local model (which will start
     * auto-reconnection), discards the remote stub and stops the watcher.
     */
    private void handleServerCrash() {
        if (this.serverStub == null) return;
        System.err.println("[RMI] Connessione persa, avviso il modello...");
        clientModel.triggerConnectionLost();
        this.serverStub = null;
        if(connectionWatcher != null) connectionWatcher.cancel();
    }

    /* =====================================================================
     * OUTGOING ACTIONS (From Client to Server) - Implements ServerConnection
     * The player interacts with the GUI, the VirtualServer calls these
     * methods, and we forward them to the server via RMI.
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
     * INCOMING EVENTS (From Server to Client) - Implements RMIClientInterface
     * The Server calls these methods directly, no instanceof
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
        // We reconstruct the DTO locally just for convenience
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

        if (oldState != null && newState != null &&
                it.polimi.ingsw.am54.gui.states.ClientStateRegistry.getHandler(oldState).isLobbyState() &&
                !it.polimi.ingsw.am54.gui.states.ClientStateRegistry.getHandler(newState).isLobbyState()) {
            try {
                it.polimi.ingsw.am54.gui.GUIApp.loadBoardScene();
            } catch (Exception | NoClassDefFoundError e) {
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
     * HEARTBEAT (Ping/Pong Management)
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
            // Immediate response to the server to confirm we are alive
            serverStub.pong();
        } catch (RemoteException | NullPointerException e) {
            // If we fail to respond, the server is down
            handleServerCrash();
        }
    }

    /* =====================================================================
     * VOLUNTARY DISCONNECTION
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