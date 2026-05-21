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
 * RMIClientHandler — Gestore di una connessione RMI per un client
 * ============================================================================
 *
 * RUOLO:
 * - Gestisce le richieste RMI da un client
 * - Inoltra le richieste al Controller (singola partita)
 * - Implementa ClientConnection (sendUpdate via callback RMI)
 * - Gestisce heartbeat (ping/pong)
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
    public void pong() throws RemoteException {
        lastPongTime = System.currentTimeMillis();
    }

    public void startPingScheduler() {
        this.lastPongTime = System.currentTimeMillis();
        this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
        this.pingScheduler.scheduleAtFixedRate(() -> {
            if (!connected) return;

            long timeSinceLastPong = System.currentTimeMillis() - lastPongTime;
            if (timeSinceLastPong > PING_TIMEOUT_MS) {
                System.out.println("[Server RMI] Ping timeout per " + playerNickname + " (" + timeSinceLastPong + "ms)");

                controller.markDisconnected(playerNickname);
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
            // clientStub rappresenta l'interfaccia remota del client (RMIClientInterface)
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
    public void disconnect() {
        if (!connected) return;
        connected = false;
        if (pingScheduler != null) pingScheduler.shutdownNow();
        if (playerNickname != null) {
            controller.removeClient(playerNickname);
        }

        // --- FIX MEMORY LEAK ---
        try {
            // "Scollega" questo handler dalla rete RMI -> sennò non venivano mai effettivamente chiuse
            UnicastRemoteObject.unexportObject(this, true);
            // true = forza la disconnessione anche se ci sono chiamate in corso.
        } catch (NoSuchObjectException e) {
            // Già de-esportato, possiamo ignorarlo
        }
    }

}