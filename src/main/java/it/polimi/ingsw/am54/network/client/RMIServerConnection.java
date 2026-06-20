package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.rmi.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the client connection via RMI.
     * Connects to the RMIGameServer, obtains a personal handler,
 * and implements RMIClientInterface to receive callbacks from the server.
     * Manages heartbeat (ping/pong) and automatic reconnection.
 */
public class RMIServerConnection extends UnicastRemoteObject implements ServerConnection, RMIClientInterface {

    /** RMI server stub: used to send Requests and respond to pings */
    private RMIClientHandlerInterface serverStub;

    /** Local client model: updated with received updates */
    private ClientModel clientModel;

    /* =====================================================================
     * CONSTRUCTOR
     * ===================================================================== */

    /**
     * Constructs an RMI connection, registers on the server and obtains a
     * personal client handler stub.
     *
     * @param server      the RMI game server stub to connect to
     * @param clientModel the local model to update with incoming updates
     * @throws RemoteException if registration fails
     */
    public RMIServerConnection(RMIGameServer server, ClientModel clientModel) throws RemoteException {
        this.clientModel = clientModel;
        // Register on the server and get our personal handler
        this.serverStub = server.connect(this);
    }

    /**
     * Support method to handle a server crash in RMI.
     * Reports the network drop to the Model in a decoupled way.
     */
    private void handleServerCrash() {
        if (this.serverStub == null) return;
        System.err.println("[RMI] Connessione persa, avviso il modello...");
        clientModel.triggerConnectionLost();
        // Detach the broken stub
        this.serverStub = null;
    }

    /* =====================================================================
     * OUTGOING ACTIONS (From Client to Server) - Implements ServerConnection
     * The player interacts with the GUI, VirtualServer calls these
     * methods, and we forward them to the server via RMI.
     * ===================================================================== */

    /** {@inheritDoc} */
    @Override
    public void joinGame(String nickname) {
        try { serverStub.joinGameRequest(nickname); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    /** {@inheritDoc} */
    @Override
    public void setupGame() {
        try { serverStub.setupGameRequest(); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    /** {@inheritDoc} */
    @Override
    public void chooseTotem(TotemColors color) {
        try { serverStub.chooseTotemColorRequest(color.name()); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    /** {@inheritDoc} */
    @Override
    public void placeTotem(int position) {
        try { serverStub.placeTotemRequest(position); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    /** {@inheritDoc} */
    @Override
    public void pickCard(String row, int index) {
        try { serverStub.pickCardRequest(row, index); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    /** {@inheritDoc} */
    @Override
    public void endTurn() {
        try { serverStub.endTurnRequest(); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    /** {@inheritDoc} */
    @Override
    public void requestLeaderboard(int numPlayers) {
        try { serverStub.getLeaderboardRequest(numPlayers); }
        catch (RemoteException | NullPointerException e) { handleServerCrash(); }
    }

    /* =====================================================================
     * INCOMING EVENTS (From Server to Client) - Implements RMIClientInterface
     * The Server calls these methods directly, no instanceof
     * ===================================================================== */

    /** {@inheritDoc} */
    @Override
    public void onPlayerJoined(String nickname) throws RemoteException {
        clientModel.updatePlayerJoined(nickname);
    }

    /** {@inheritDoc} */
    @Override
    public void onTotemColorChosen(String nickname, TotemColors color) throws RemoteException {
        clientModel.updateTotemColor(nickname, color);
    }

    /** {@inheritDoc} */
    @Override
    public void onTotemPlaced(String nickname, int position) throws RemoteException {
        clientModel.updateTotemPlacement(nickname, position);
    }

    /** {@inheritDoc} */
    @Override
    public void onTotemReturned(String nickname, int position, int food, int aura) throws RemoteException {
        clientModel.updateTotemReturned(nickname, position, food, aura);
    }

    /** {@inheritDoc} */
    @Override
    public void onCardPicked(String nickname, String row, int cardIndex,
                             int newFood, int newMagic, Map<Integer, String> offering) throws RemoteException {
        clientModel.updateCardPicked(nickname, row, cardIndex, newFood, newMagic, offering);
    }

    /** {@inheritDoc} */
    @Override
    public void onTurnChanged(String nickname) throws RemoteException {
        clientModel.updateTurn(nickname);
    }

    /** {@inheritDoc} */
    @Override
    public void onEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats,
                                List<String> charUp, List<String> charLow,
                                List<String> eventUp, List<String> eventLow,
                                List<String> buildUp, List<String> buildLow) throws RemoteException {
        // Rebuild the DTO locally just for convenience
        EventUpdate localSnapshot = new EventUpdate(name,stats, charUp, charLow, eventUp, eventLow, buildUp, buildLow);
        clientModel.updateEventSnapshot(localSnapshot);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public void onEndGame(EndGameUpdate update) throws RemoteException {
        clientModel.updateEndGame(
                update.getWinners(),
                update.getAllScores(),
                update.getNumPlayers()
        );
    }

    /** {@inheritDoc} */
    @Override
    public void onErrorMessage(String message) throws RemoteException {
        clientModel.updateError(message);
    }

    /** {@inheritDoc} */
    @Override
    public void onLeaderboardUpdate(LeaderboardUpdate update) throws RemoteException {
        clientModel.updateLeaderboard(update.getPlayerPosition(), update.getEntries(), update.getNumPlayers());
    }

    /** {@inheritDoc} */
    @Override
    public void onPlayerDisconnected(String nickname) throws RemoteException {
        clientModel.updatePlayerDisconnected(nickname);
    }

    /** {@inheritDoc} */
    @Override
    public void onPlayerReconnected(String nickname) throws RemoteException {
        clientModel.updatePlayerReconnected(nickname);
    }

    /* =====================================================================
     * HEARTBEAT (Ping/Pong Management)
     * ===================================================================== */

    /** {@inheritDoc} */
    @Override
    public void onGameSuspended() throws RemoteException {
        clientModel.updateGameSuspended();
    }

    /** {@inheritDoc} */
    @Override
    public void onGameResumed() throws RemoteException {
        clientModel.updateGameResumed();
    }

    /**
     * Responds to a server heartbeat ping by sending a pong.
     * If the stub is unreachable, triggers server-crash handling.
     *
     * @throws RemoteException if the pong cannot be sent
     */
    @Override
    public void ping() throws RemoteException {
        try {
            // Immediate response to the server confirming we're alive
            serverStub.pong();
        } catch (RemoteException | NullPointerException e) {
            // If we can't respond, the server is down
            handleServerCrash();
        }
    }

    /* =====================================================================
     * VOLUNTARY DISCONNECTION
     * ===================================================================== */

    /** {@inheritDoc} */
    @Override
    public void disconnect() {
        try {
            if (serverStub != null) {
                serverStub.disconnect();
            }
        } catch (RemoteException ignored) {}
    }
}