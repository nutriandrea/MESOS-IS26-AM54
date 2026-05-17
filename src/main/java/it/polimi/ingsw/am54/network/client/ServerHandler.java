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
                if (message instanceof PingRequest ping) {
                    handlePing(ping);
                } else if (message instanceof Update update) {
                    handleUpdate(update);
                }
            } catch (Exception e) {
                if (running) stop();
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
            stop();
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
        // StateChangedUpdate può contenere stato completo per riconnessione
        if (message instanceof StateChangedUpdate stateUpdate) {
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
            return;
        }

        // Gestione dei normali update
        if (message instanceof PlayerJoinedUpdate msg) {
            clientModel.updatePlayerJoined(msg.getNickname());
        }
        else if (message instanceof PlayerDisconnectedUpdate msg) {
            clientModel.updatePlayerDisconnected(msg.getNickname());
        }
        else if (message instanceof PlayerReconnectedUpdate msg) {
            clientModel.updatePlayerReconnected(msg.getNickname());
        }
        else if (message instanceof TotemPlacedUpdate msg) {
            clientModel.updateTotemPlacement(msg.getNickname(), msg.getOfferPosition());
        }
        else if (message instanceof CardPickedUpdate msg) {
            clientModel.updateCardPicked(msg.getNickname(), msg.getRow(), msg.getCardIndex(), msg.getNewFoodBalance(), msg.getUpdatedOfferingTrack());
        }
        else if (message instanceof TurnChangedUpdate msg) {
            clientModel.updateTurn(msg.getNickname());
        }
        else if (message instanceof TotemReturnedUpdate msg) {
            clientModel.updateTotemReturned(msg.getNickname(), msg.getNewFoodBalance(), msg.getNewAuraBalance());
        }
        else if (message instanceof TotemColorUpdate msg) {
            clientModel.updateTotemColor(msg.getNickname(), msg.getColor());
        }
        else if (message instanceof EndGameUpdate msg) {
            clientModel.updateEndGame(msg.getWinners());
            if (msg.getAllScores() != null && !msg.getAllScores().isEmpty()) {
                clientModel.setLastMatchScores(msg.getAllScores(), msg.getNumPlayers());
            }
        }
        else if (message instanceof LeaderboardUpdate msg) {
            clientModel.updateLeaderboard(msg.getPlayerPosition(), msg.getEntries(), msg.getNumPlayers());
        }
        else if (message instanceof ErrorMessageUpdate msg) {
            clientModel.updateError(msg.getMessage());
        }
        else if (message instanceof EventUpdate msg) {
            clientModel.updateEventSnapshot(msg);
        }
        else if (message instanceof GameSuspendedUpdate) {
            clientModel.updateGameSuspended();
        }
        else if (message instanceof GameResumedUpdate) {
            clientModel.updateGameResumed();
        }
    }
}
