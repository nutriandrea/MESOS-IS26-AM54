package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * ============================================================================
 * SocketClientHandler — Handler for a TCP connection for a client
 * ============================================================================
 *
 * ROLE::
 * - Separate thread for each client connected via TCP Socket
 * - Reads Requests from the client and forwards them to the Controller (single game)
 * - Implements ClientConnection (sendUpdate) and ClientHandler
 * - Manages the heartbeat (ping/pong) system to detect disconnections
 *
 * PING-PONG PATTERN:
 * - Ping every 5 seconds (PING_INTERVAL_MS)
 * - If no Pong received within 15 seconds (PING_TIMEOUT_MS):
 *   1. Calls controller.markDisconnected() -> notifies other players
 * 2. Calls disconnect() -> closes the socket and cleans up
 * ============================================================================
 */
public class SocketClientHandler implements Runnable, ClientConnection {

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

    /**
     * Constructs a socket client handler for the given connection.
     *
     * @param socket the client TCP socket
     * @param server the game server instance
     */
    public SocketClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.controller = server.getController();
        this.connected = true;
        this.lastPongTime = System.currentTimeMillis();

        startPingScheduler();
    }

    /**
     * Main loop: reads objects from the socket, dispatching pongs,
     * join requests and other game requests to the controller.
     */
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server TCP] Nuovo client in ascolto.");

            while (connected) {
                Object input = in.readObject();
                /*
                 * TWO-LEVEL DISPATCH:
                 * 1. PongResponse e JoinGameRequest come early instanceof
                 *    (socket-level operations / need context before dispatch)
                 * 2. All other Requests via polymorphic execute()
                 */
                if (input instanceof PongResponse) {
                    lastPongTime = System.currentTimeMillis();
                } else if (input instanceof JoinGameRequest r) {
                    this.playerNickname = r.getNickname();
                    controller.handleJoinGame(this.playerNickname, this);
                } else if (input instanceof Request request) {
                    request.execute(controller, playerNickname, this);
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

    /**
     * Starts a scheduled executor that sends pings every 5 seconds and
     * disconnects the client if no pong is received within 15 seconds.
     */
    public void startPingScheduler() {
        this.lastPongTime = System.currentTimeMillis();
        this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
        this.pingScheduler.scheduleAtFixedRate(() -> {
            if (!connected) return;

            long timeSinceLastPong = System.currentTimeMillis() - lastPongTime;
            if (timeSinceLastPong > PING_TIMEOUT_MS) {
                System.out.println("[Server TCP] Ping timeout per " + playerNickname + " (" + timeSinceLastPong + "ms)");

                disconnect();
                return;
            }

            writeToStream(new PingRequest(System.currentTimeMillis()));
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Private helper to write on the network stream safely.
     */
    private synchronized void writeToStream(Update update) {
        if (!connected) return;
        try {
            out.writeObject(update);
            out.flush();
            out.reset();
        } catch (IOException e) {
            disconnect();
        }
    }

    /* =====================================================================
     * IMPLEMENTATION ClientConnection
     * Socket "packages" the received parameters into a DTO.
     * ===================================================================== */

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerJoined(String nickname) {
        writeToStream(new PlayerJoinedUpdate(nickname));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTotemColor(String nickname, TotemColors color) {
        writeToStream(new TotemColorUpdate(nickname, color));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTotemPlaced(String nickname, int position) {
        writeToStream(new TotemPlacedUpdate(nickname, position));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyCardPicked(String nickname, String row, int index, int food, int magic, Map<Integer, String> offering) {
        writeToStream(new CardPickedUpdate(nickname, row, index, food, magic, offering));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTotemReturned(String nickname, int position, int food, int aura) {
        writeToStream(new TotemReturnedUpdate(nickname, position, food, aura));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats, List<String> charUp, List<String> charLow, List<String> eventUp, List<String> eventLow, List<String> buildUp, List<String> buildLow) {
        writeToStream(new EventUpdate(name, stats, charUp, charLow, eventUp, eventLow, buildUp, buildLow));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTurnChanged(String nickname) {
        writeToStream(new TurnChangedUpdate(nickname));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyStateChanged(String stateName, int currentEra, int currentRound, Map<String, StateChangedUpdate.PlayerSnapshot> playersStats, List<String> turnOrderTrack, Map<Integer, String> offeringTrack, List<String> charUpper, List<String> charLower, List<String> eventUpper, List<String> eventLower, List<String> buildUpper, List<String> buildLower) {
        writeToStream(new StateChangedUpdate(stateName, currentEra, currentRound, playersStats, turnOrderTrack, offeringTrack, charUpper, charLower, eventUpper, eventLower, buildUpper, buildLower));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEndGame(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
        EndGameUpdate updateMessage = new EndGameUpdate(winners, allScores, numPlayers);

        writeToStream(updateMessage);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyError(String message) {
        writeToStream(new ErrorMessageUpdate(message));
    }

    /** {@inheritDoc} */
    @Override
    public void sendLeaderboardUpdate(LeaderboardUpdate update) {
        writeToStream(update);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerDisconnected(String nickname) {
        writeToStream(new PlayerDisconnectedUpdate(nickname));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerReconnected(String nickname) {
        writeToStream(new PlayerReconnectedUpdate(nickname));
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGameSuspended() {
        writeToStream(new GameSuspendedUpdate());
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGameResumed() {
        writeToStream(new GameResumedUpdate());
    }

    /** {@inheritDoc} */
    @Override
    public void sendPing() {
        writeToStream(new PingRequest(System.currentTimeMillis()));
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;
        if (pingScheduler != null) pingScheduler.shutdownNow();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        if (playerNickname != null) {
            controller.removeClient(playerNickname);
        }
    }

}