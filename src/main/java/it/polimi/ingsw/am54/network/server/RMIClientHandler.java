package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface;
import it.polimi.ingsw.am54.network.rmi.RMIClientInterface;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * ============================================================================
 * RMIClientHandler — Handler for an RMI connection for a client
 * ============================================================================
 *
 * ROLE:
 * - Handles RMI requests from a client
 * - Forwards requests to the Controller (single game)
 * - Implements ClientConnection (sendUpdate via RMI callback)
 * - Manages heartbeat (ping/pong)
 * ============================================================================
 */
public class RMIClientHandler extends UnicastRemoteObject implements RMIClientHandlerInterface, ClientConnection {

    private static final long PING_INTERVAL_MS = 5000;
    private static final long PING_TIMEOUT_MS = 15000;

    private final RMIClientInterface clientStub;
    private final Controller controller;
    private boolean connected = true;
    private String playerNickname;
    private volatile long lastPongTime;
    private ScheduledExecutorService pingScheduler;

    public RMIClientHandler(RMIClientInterface clientStub, Server server) throws RemoteException {
        super();
        this.clientStub = clientStub;
        this.controller = server.getController();
        this.lastPongTime = System.currentTimeMillis();

        startPingScheduler();
    }

    /**
     * RMI request to join the game: stores the client's nickname (necessary
     * to identify it in subsequent requests) and forwards the join to the Controller.
     *
     * @param nickname the nickname chosen by the client
     * @throws RemoteException in case of RMI error
     */
    @Override
    public void joinGameRequest(String nickname) throws RemoteException {
        this.playerNickname = nickname;
        controller.handleJoinGame(this.playerNickname, this);
    }
    @Override
    public void setupGameRequest() throws RemoteException {
        controller.handleSetupGame(this.playerNickname);
    }
    @Override
    public void chooseTotemColorRequest(String color) throws RemoteException {
        controller.handleChooseTotemColor(this.playerNickname, color);
    }
    @Override
    public void placeTotemRequest(int offerPosition) throws RemoteException {
        controller.handlePlaceTotem(this.playerNickname, offerPosition);
    }
    @Override
    public void pickCardRequest(String row, int cardIndex) throws RemoteException {
        controller.handlePickCard(this.playerNickname, row, cardIndex);
    }
    @Override
    public void endTurnRequest() throws RemoteException {
        controller.handleEndTurn(this.playerNickname);
    }
    @Override
    public void getLeaderboardRequest(int numPlayers) throws RemoteException {
        controller.handleGetLeaderboard(this, this.playerNickname, numPlayers);
    }
    @Override
    public void pong() throws RemoteException {
        lastPongTime = System.currentTimeMillis();
    }

    /**
     * Starts the RMI heartbeat: sends a ping to the client every {@value #PING_INTERVAL_MS} ms
     * and, if it does not receive a pong within {@value #PING_TIMEOUT_MS} ms, disconnects it.
     */
    public void startPingScheduler() {
        this.lastPongTime = System.currentTimeMillis();
        this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
        this.pingScheduler.scheduleAtFixedRate(() -> {
            if (!connected) return;

            long timeSinceLastPong = System.currentTimeMillis() - lastPongTime;
            if (timeSinceLastPong > PING_TIMEOUT_MS) {
                System.out.println("[Server RMI] Ping timeout per " + playerNickname + " (" + timeSinceLastPong + "ms)");

                disconnect();
                return;
            }

            sendPing();

        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void notifyPlayerJoined(String nickname) {
        try { clientStub.onPlayerJoined(nickname); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyTotemColor(String nickname, TotemColors color) {
        try { clientStub.onTotemColorChosen(nickname, color); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyTotemPlaced(String nickname, int position) {
        try { clientStub.onTotemPlaced(nickname, position); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyCardPicked(String nickname, String row, int index, int food, int magic,
                                 Map<Integer, String> offering) {
        try { clientStub.onCardPicked(nickname, row, index, food, magic, offering); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyTotemReturned(String nickname, int position, int food, int aura) {
        try { clientStub.onTotemReturned(nickname,food, aura); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats,
                                    List<String> charUp, List<String> charLow,
                                    List<String> eventUp, List<String> eventLow,
                                    List<String> buildUp, List<String> buildLow) {
        try { clientStub.onEventResolved(name, stats, charUp, charLow, eventUp, eventLow, buildUp, buildLow); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyTurnChanged(String nickname) {
        try { clientStub.onTurnChanged(nickname); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyStateChanged(String stateName, int currentEra, int currentRound,
                                   Map<String, StateChangedUpdate.PlayerSnapshot> playersStats,
                                   List<String> turnOrderTrack, Map<Integer, String> offeringTrack,
                                   List<String> charUpper, List<String> charLower,
                                   List<String> eventUpper, List<String> eventLower,
                                   List<String> buildUpper, List<String> buildLower) {
        try { clientStub.onStateChanged(stateName, currentEra, currentRound, playersStats, turnOrderTrack, offeringTrack,
                charUpper, charLower, eventUpper, eventLower, buildUpper, buildLower); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyEndGame(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
        try {
            // clientStub represents the remote interface of the client (RMIClientInterface)
            clientStub.onEndGame(new EndGameUpdate(winners, allScores, numPlayers));
        } catch (RemoteException e) {
            disconnect();
        }
    }

    @Override
    public void notifyError(String message) {
        try { clientStub.onErrorMessage(message); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyPlayerDisconnected(String nickname) {
        try { clientStub.onPlayerDisconnected(nickname); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyPlayerReconnected(String nickname) {
        try { clientStub.onPlayerReconnected(nickname); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyGameSuspended() {
        try { clientStub.onGameSuspended(); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void notifyGameResumed() {
        try { clientStub.onGameResumed(); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void sendPing() {
        try { clientStub.ping(); }
        catch (RemoteException e) { disconnect(); }
    }

    @Override
    public void sendLeaderboardUpdate(LeaderboardUpdate update) {
        try { clientStub.onLeaderboardUpdate(update); }
        catch (RemoteException e) { disconnect(); }
    }

    /**
     * Closes the client's RMI connection: stops the heartbeat, notifies the Controller to
     * remove the player and unexports this handler from the RMI registry. Idempotent.
     */
    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;
        if (pingScheduler != null) pingScheduler.shutdownNow();
        if (playerNickname != null) {
            controller.removeClient(playerNickname);
        }

        try {
            // "Disconnect" this handler from the RMI network -> otherwise they were never actually closed
            UnicastRemoteObject.unexportObject(this, true);
            // true = forces disconnection even if there are calls in progress.
        } catch (NoSuchObjectException e) {
            // Already unexported, we can ignore it
        }
    }

}