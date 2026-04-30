package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface;
import it.polimi.ingsw.am54.network.rmi.RMIClientInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
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
public class RMIClientHandler extends UnicastRemoteObject implements RMIClientHandlerInterface, ClientConnection, ClientHandler {

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
    }

    @Override
    public void handleRequest(Request request) throws RemoteException {
        if (request instanceof JoinGameRequest req) {
            this.playerNickname = req.getNickname();
            controller.handleJoinGame(this.playerNickname, this);
        } else if (request instanceof SetupGameRequest) {
            controller.handleSetupGame(this.playerNickname);
        } else if (request instanceof ChooseTotemRequest req) {
            controller.handleChooseTotemColor(this.playerNickname, req.getColor().name());
        } else if (request instanceof PlaceTotemRequest req) {
            controller.handlePlaceTotem(this.playerNickname, req.getOfferPosition());
        } else if (request instanceof PickCardRequest req) {
            controller.handlePickCard(this.playerNickname, req.getRow(), req.getCardIndex());
        } else if (request instanceof PongResponse pong) {
            lastPongTime = System.currentTimeMillis();
        }
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

            sendUpdate(new PingRequest(System.currentTimeMillis()));
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void sendUpdate(Update update) {
        if (!connected) return;
        try {
            clientStub.receiveUpdate(update);
        } catch (RemoteException e) {
            disconnect();
        }
    }

    @Override
    public void sendError(String errorMessage) {
        sendUpdate(new ErrorMessageUpdate(errorMessage));
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        this.connected = false;

        if (pingScheduler != null) {
            pingScheduler.shutdownNow();
        }

        if (playerNickname != null) {
            controller.removeClient(playerNickname);
        }
    }
}