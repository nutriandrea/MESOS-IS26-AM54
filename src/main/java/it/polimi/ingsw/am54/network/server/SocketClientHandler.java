package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.messages.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * ============================================================================
 * SocketClientHandler — Gestore di una connessione TCP per un client
 * ============================================================================
 *
 * RUOLO:
 * - Thread separato per ogni client connesso via Socket TCP
 * - Legge le Request dal client e le inoltra al Controller (singola partita)
 * - Implementa ClientConnection (sendUpdate) e ClientHandler
 * - Gestisce il sistema di heartbeat (ping/pong) per rilevare disconnessioni
 *
 * PING-PONG PATTERN:
 * - Ping ogni 5 secondi (PING_INTERVAL_MS)
 * - Se nessun Pong ricevuto entro 15 secondi (PING_TIMEOUT_MS):
 *   1. Chiama controller.markDisconnected() -> notifica gli altri giocatori
 *   2. Chiama disconnect() -> chiude il socket e pulisce
 * ============================================================================
 */
public class SocketClientHandler implements Runnable, ClientHandler, ClientConnection {

    private static final long PING_INTERVAL_MS = 5000;
    private static final long PING_TIMEOUT_MS = 15000;

    private final Socket socket;
    private final Server server;
    private final Controller controller;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connected;
    private String playerNickname;
    private volatile long lastPongTime;
    private ScheduledExecutorService pingScheduler;

    public SocketClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.controller = server.getController();
        this.connected = true;
        this.lastPongTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server TCP] Nuovo client in ascolto.");

            while (connected) {
                Object input = in.readObject();
                if (input instanceof Request) {
                    handleRequest((Request) input);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                System.out.println("[Server TCP] Client disconnesso: " + playerNickname);
            }
        } finally {
            disconnect();
        }
    }

    private void handleRequest(Request request) {
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
<<<<<<< HEAD
} else if (request instanceof PongResponse pong) {
            lastPongTime = System.currentTimeMillis();
        } else if (request instanceof EndTurnRequest) {
            controller.handleEndTurn(this.playerNickname);
        }
    }
    }

    public void startPingScheduler() {
        this.lastPongTime = System.currentTimeMillis();
        this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
        this.pingScheduler.scheduleAtFixedRate(() -> {
            if (!connected) return;

            long timeSinceLastPong = System.currentTimeMillis() - lastPongTime;
            if (timeSinceLastPong > PING_TIMEOUT_MS) {
                System.out.println("[Server TCP] Ping timeout per " + playerNickname + " (" + timeSinceLastPong + "ms)");
                controller.markDisconnected(playerNickname);
                disconnect();
                return;
            }

            sendUpdate(new PingRequest(System.currentTimeMillis()));
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void sendUpdate(Update update) {
        if (!connected) return;
        try {
            out.writeObject(update);
            out.flush();
            out.reset();
        } catch (IOException e) {
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
        connected = false;

        if (pingScheduler != null) {
            pingScheduler.shutdownNow();
        }

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        if (playerNickname != null) {
            controller.removeClient(playerNickname);
        }
    }
}