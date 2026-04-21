package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.network.messages.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SocketClientHandler implements ClientHandler, Runnable {
    private final Socket socket;
    private final Controller controller;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connected = true;
    private String playerNickname = null;

    private static final long PING_INTERVAL_MS = 5000;
    private static final long PING_TIMEOUT_MS = 15000;
    private long lastPongTime = System.currentTimeMillis();
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();

    public SocketClientHandler(Socket socket, Controller controller) {
        this.socket = socket;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server] Nuovo client in ascolto.");

            out.writeObject("CONNECTION_ESTABLISHED");
            out.flush();

            startPingLoop();

            while (connected) {
                Object input = in.readObject();

                if (input instanceof PongResponse pong) {
                    handlePong(pong);
                } else if (input instanceof Request) {
                    handleRequest((Request) input);
                } else {
                    System.err.println("[Server] Messaggio non autorizzato ricevuto.");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] Client disconnesso: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void startPingLoop() {
        pingScheduler.scheduleAtFixedRate(() -> {
            if (!connected) {
                pingScheduler.shutdown();
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastPongTime > PING_TIMEOUT_MS) {
                System.out.println("[Server] Timeout: client non risponde ai ping");
                disconnect();
                return;
            }

            try {
                sendMessage(new PingRequest());
            } catch (Exception e) {
                disconnect();
            }
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void handlePong(PongResponse pong) {
        lastPongTime = System.currentTimeMillis();
    }

    /**
     * Gestisce le richieste (Request) che arrivano dal Client.
     * Traduce i messaggi di rete in chiamate ai metodi del Controller.
     */
    private void handleRequest(Request request) {

        // --- 1. JOIN GAME ---
        if (request instanceof JoinGameRequest req) {
            this.playerNickname = req.getNickname();
            controller.handleJoinGame(this.playerNickname, this);
        }

        // --- 1B. RECONNECT ---
        else if (request instanceof ReconnectRequest req) {
            this.playerNickname = req.getNickname();
            controller.handleReconnect(this.playerNickname, this);
        }

        // --- 2. SETUP GAME ---
        else if (request instanceof SetupGameRequest) {
            checkAndExecute(() -> controller.handleSetupGame(this.playerNickname));
        }

        // --- 3. CHOOSE TOTEM COLOR ---
        else if (request instanceof ChooseTotemRequest req) {
            // Nota: Se la tua ChooseTotemRequest usa l'Enum TotemColors,
            // ma il Controller accetta String, usiamo .toString()
            checkAndExecute(() -> controller.handleChooseTotemColor(this.playerNickname, req.getColor().toString()));
        }

        // --- 4. PLACE TOTEM ---
        else if (request instanceof PlaceTotemRequest req) {
            checkAndExecute(() -> controller.handlePlaceTotem(this.playerNickname, req.getOfferPosition()));
        }

        // --- 5. PICK CARD ---
        else if (request instanceof PickCardRequest req) {
            checkAndExecute(() -> controller.handlePickCard(this.playerNickname, req.getRow(), req.getCardIndex()));
        }

        // --- 6. UNKNOWN REQUEST ---
        else {
            System.err.println("[Server] Messaggio non gestito: " + request.getClass().getSimpleName());
            sendError("Azione non supportata.");
        }
    }

    /**
     * Helper interno per assicurarsi che il giocatore sia identificato prima di agire.
     */
    private void checkAndExecute(Runnable action) {
        if (this.playerNickname != null) {
            action.run();
        } else {
            sendError("Devi prima unirti alla partita con un nickname!");
        }
    }

    @Override
    public synchronized void sendMessage(Update message) { // Deve essere Update!
        if (!connected) return;
        try {
            out.writeObject(message);
            out.flush();
            out.reset(); // Fondamentale per pulire la cache degli oggetti serializzati
        } catch (IOException e) {
            disconnect();
        }
    }

    @Override
    public void sendError(String errorMessage) {
        // ErrorMessage deve implementare Update per funzionare qui!
        sendMessage(new ErrorMessageUpdate(errorMessage));
    }


    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;
        pingScheduler.shutdownNow();
        if (playerNickname != null) {
            controller.markDisconnected(playerNickname);
        }
        try {
            socket.close();
        } catch (IOException e) {
        }
    }
}