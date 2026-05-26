package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ============================================================================
 * ServerHandler — Thread di ascolto asincrono lato client (Socket TCP)
 * ============================================================================
 *
 * RUOLO:
 * Thread indipendente che rimane in ascolto continuo dei messaggi provenienti
 * dal server sulla connessione Socket TCP.
 *
 * COSA FA:
 * 1. Legge oggetti dallo stream di input del socket
 * 2. Se e' un PingRequest: risponde AUTOMATICAMENTE con un PongResponse
 *    (il client non deve fare nulla, e' tutto automatico)
 * 3. Se e' un Update: aggiorna il ClientModel di conseguenza
 * 4. Se il socket si chiude: ferma il thread
 *
 * GESTIONE PING (trasparente per il client):
 * Quando il server invia un PingRequest, il client risponde immediatamente
 * con un PongResponse contenente lo stesso timestamp. Questo permette al
 * server di calcolare il round-trip time e rilevare disconnessioni.
 *
 * ============================================================================
 */
public class ServerHandler implements Runnable {

    /** Stream di input per ricevere messaggi dal server */
    private final ObjectInputStream in;

    /** Stream di output per rispondere ai ping */
    private final ObjectOutputStream out;

    /** Modello locale del client: viene aggiornato con gli update ricevuti */
    private final ClientModel clientModel;

    /** Flag di stato: true = in ascolto, false = fermato */
    private volatile boolean running = true;

    /**
     * Costruttore.
     *
     * @param in stream di input dal server
     * @param out stream di output verso il server (usato per rispondere ai ping)
     * @param clientModel modello locale da aggiornare
     */
    public ServerHandler(ObjectInputStream in, ObjectOutputStream out, ClientModel clientModel) {
        this.in = in;
        this.out = out;
        this.clientModel = clientModel;
    }

    /**
     * Loop principale: legge messaggi dal server finche' running == true.
     *
     * Per ogni messaggio ricevuto:
     * - Se e' un PingRequest -> rispondi con PongResponse (automatico)
     * - Se e' un Update -> aggiorna il ClientModel
     * - Se eccezione -> ferma il thread
     */
    @Override
    public void run() {
        while (running) {
            try {
                Object message = in.readObject();
                if (message instanceof Update update) {
                    if (PingRequest.TYPE.equals(update.getType())) {
                        handlePing((PingRequest) update);
                    } else {
                        handleUpdate(update);
                    }
                }
            } catch (Exception e) {
                // SE IL SERVER CADE, ENTRA QUI:
                if (running) {
                    System.err.println("[SOCKET] Connessione persa, avvio auto-reconnect...");
                    clientModel.triggerConnectionLost();
                    stop();
                }
            }
        }
    }

    /**
     * Risponde automaticamente a un PingRequest del server.
     * Invia un PongResponse con lo stesso timestamp ricevuto.
     *
     * Questo e' completamente trasparente per il client: il giocatore
     * non vede nulla, il ping/pong avviene in background.
     */
    private void handlePing(PingRequest ping) {
        try {
            out.writeObject(new PongResponse(ping.getTimestamp()));
            out.flush();
            out.reset();
        } catch (Exception e) {
            if (running) {
                System.err.println("[SOCKET] Connessione persa al ping, avvio auto-reconnect...");
                clientModel.triggerConnectionLost();
                stop();
            }
        }
    }

    /** Ferma il thread di ascolto */
    public void stop() { this.running = false; }

    /**
     * Smista gli Update ricevuti dal server al ClientModel.
     *
     * Ogni tipo di Update chiama un metodo specifico del ClientModel:
     * - PlayerJoinedUpdate -> updatePlayerJoined()
     * - PlayerDisconnectedUpdate -> updatePlayerDisconnected()
     * - PlayerReconnectedUpdate -> updatePlayerReconnected()
     * - TotemPlacedUpdate -> updateTotemPlacement()
     * - CardPickedUpdate -> updateCardPicked()
     * - TurnChangedUpdate -> updateTurn()
     * - StateChangedUpdate -> updateState() [ora contiene stato completo]
     * - TotemColorUpdate -> updateTotemColor()
     * - EndGameUpdate -> updateEndGame()
     * - ErrorMessageUpdate -> updateError()
     */
    private void handleUpdate(Update message) {
        switch (message.getType()) {
            case StateChangedUpdate.TYPE -> {
                StateChangedUpdate stateUpdate = (StateChangedUpdate) message;
                String oldState = clientModel.getCurrentState();
                clientModel.updateState(stateUpdate);
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
            case PlayerJoinedUpdate.TYPE -> {
                PlayerJoinedUpdate msg = (PlayerJoinedUpdate) message;
                clientModel.updatePlayerJoined(msg.getNickname());
            }
            case PlayerDisconnectedUpdate.TYPE -> {
                PlayerDisconnectedUpdate msg = (PlayerDisconnectedUpdate) message;
                clientModel.updatePlayerDisconnected(msg.getNickname());
            }
            case PlayerReconnectedUpdate.TYPE -> {
                PlayerReconnectedUpdate msg = (PlayerReconnectedUpdate) message;
                clientModel.updatePlayerReconnected(msg.getNickname());
            }
            case TotemPlacedUpdate.TYPE -> {
                TotemPlacedUpdate msg = (TotemPlacedUpdate) message;
                clientModel.updateTotemPlacement(msg.getNickname(), msg.getOfferPosition());
            }
            case CardPickedUpdate.TYPE -> {
                CardPickedUpdate msg = (CardPickedUpdate) message;
                clientModel.updateCardPicked(msg.getNickname(), msg.getRow(), msg.getCardIndex(), msg.getNewFoodBalance(), msg.getNewMagicBalance(), msg.getUpdatedOfferingTrack());
            }
            case TurnChangedUpdate.TYPE -> {
                TurnChangedUpdate msg = (TurnChangedUpdate) message;
                clientModel.updateTurn(msg.getNickname());
            }
            case TotemReturnedUpdate.TYPE -> {
                TotemReturnedUpdate msg = (TotemReturnedUpdate) message;
                clientModel.updateTotemReturned(msg.getNickname(), msg.getNewFoodBalance(), msg.getNewAuraBalance());
            }
            case TotemColorUpdate.TYPE -> {
                TotemColorUpdate msg = (TotemColorUpdate) message;
                clientModel.updateTotemColor(msg.getNickname(), msg.getColor());
            }
            case EndGameUpdate.TYPE -> {
                EndGameUpdate update = (EndGameUpdate) message;
                clientModel.updateEndGame(
                        update.getWinners(),
                        update.getAllScores(),
                        update.getNumPlayers()
                );
            }
            case LeaderboardUpdate.TYPE -> {
                LeaderboardUpdate msg = (LeaderboardUpdate) message;
                clientModel.updateLeaderboard(msg.getPlayerPosition(), msg.getEntries(), msg.getNumPlayers());
            }
            case ErrorMessageUpdate.TYPE -> {
                ErrorMessageUpdate msg = (ErrorMessageUpdate) message;
                clientModel.updateError(msg.getMessage());
            }
            case EventUpdate.TYPE -> {
                EventUpdate msg = (EventUpdate) message;
                clientModel.updateEventSnapshot(msg);
            }
            case GameSuspendedUpdate.TYPE -> clientModel.updateGameSuspended();
            case GameResumedUpdate.TYPE -> clientModel.updateGameResumed();
        }
    }
}
