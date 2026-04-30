package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.rmi.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * ============================================================================
 * RMIServerConnection — Connessione RMI lato client
 * ============================================================================
 *
 * RUOLO:
 * Implementa ServerConnection usando RMI invece di Socket TCP.
 * Estende UnicastRemoteObject e implementa RMIClientInterface per ricevere
 * callback dal server.
 *
 * FLUSSO:
 * 1. Il client crea un RMIServerConnection con il server RMI remoto
 * 2. Si registra sul server tramite server.connect(this)
 * 3. Il server restituisce uno stub (RMIClientHandlerInterface)
 * 4. Il client usa lo stub per inviare Request
 * 5. Il server usa la callback receiveUpdate() per inviare Update
 *
 * GESTIONE PING:
 * Quando il server invia un PingRequest tramite receiveUpdate(), il client:
 * 1. Riconosce che e' un PingRequest (non un Update normale)
 * 2. Crea un PongResponse con lo stesso timestamp
 * 3. Lo invia al server tramite serverStub.handleRequest(pong)
 *
 * Questo e' completamente trasparente per il client.
 * ============================================================================
 */
public class RMIServerConnection extends UnicastRemoteObject implements ServerConnection, RMIClientInterface {

    /** Stub RMI del server: usato per inviare Request e rispondere ai ping */
    private RMIClientHandlerInterface serverStub;

    /** Modello locale del client: viene aggiornato con gli update ricevuti */
    private ClientModel clientModel;

    /**
     * Costruttore: si connette al server RMI e si registra.
     *
     * @param server il server RMI remoto (ottenuto tramite lookup)
     * @param clientModel il modello locale da aggiornare con gli update
     */
    public RMIServerConnection(RMIGameServer server, ClientModel clientModel) throws RemoteException {
        this.clientModel = clientModel;
        // Ci registriamo sul server e otteniamo il nostro handler personale
        this.serverStub = server.connect(this);
    }

    /**
     * Invia una Request al server via RMI.
     * Se la chiamata fallisce (RemoteException), disconnette il client.
     */
    @Override
    public void sendRequest(Request request) {
        try {
            serverStub.handleRequest(request);
        } catch (RemoteException e) {
            disconnect();
        }
    }

    /**
     * Callback RMI: chiamata dal server per inviare un Update al client.
     *
     * SMISTAMENTO:
     * - Se e' un PingRequest -> rispondi con PongResponse (automatico)
     * - Altrimenti -> aggiorna il ClientModel
     *
     * Questo metodo e' l'equivalente RMI del loop di lettura del ServerHandler.
     * Invece di leggere da uno stream, il server chiama questo metodo via RMI.
     */
    @Override
    public void receiveUpdate(Update update) throws RemoteException {
        // I PingRequest richiedono una risposta immediata
        if (update instanceof PingRequest ping) {
            handlePing(ping);
            return;
        }

        // StateChangedUpdate può contenere stato completo per riconnessione
        if (update instanceof StateChangedUpdate stateUpdate) {
            clientModel.updateState(stateUpdate);
            return;
        }

        // Tutti gli altri update vengono passati al ClientModel
        if (update instanceof PlayerJoinedUpdate msg) {
            clientModel.updatePlayerJoined(msg.getNickname());
        }
        else if (update instanceof PlayerDisconnectedUpdate msg) {
            clientModel.updatePlayerDisconnected(msg.getNickname());
        }
        else if (update instanceof PlayerReconnectedUpdate msg) {
            clientModel.updatePlayerReconnected(msg.getNickname());
        }
        else if (update instanceof TotemPlacedUpdate msg) {
            clientModel.updateTotemPlacement(msg.getNickname(), msg.getOfferPosition());
        }
        else if (update instanceof CardPickedUpdate msg) {
            clientModel.updateCardPicked(msg.getNickname(), msg.getRow(), msg.getCardIndex(), msg.getNewFoodBalance());
        }
        else if (update instanceof TurnChangedUpdate msg) {
            clientModel.updateTurn(msg.getNickname());
        }
        else if (update instanceof StateChangedUpdate msg) {
            clientModel.updateState(msg);
        }
        else if (update instanceof TotemColorUpdate msg) {
            clientModel.updateTotemColor(msg.getNickname(), msg.getColor());
        }
        else if (update instanceof EndGameUpdate msg) {
            clientModel.updateEndGame(msg.getWinners());
        }
        else if (update instanceof ErrorMessageUpdate msg) {
            clientModel.updateError(msg.getMessage());
        }
        else if (update instanceof EventUpdate msg) {
            clientModel.updateEventSnapshot(msg);
        }
    }

    /**
     * Risponde automaticamente a un PingRequest del server.
     * Invia un PongResponse con lo stesso timestamp tramite RMI.
     */
    private void handlePing(PingRequest ping) throws RemoteException {
        try {
            PongResponse pong = new PongResponse(ping.getTimestamp());
            serverStub.handleRequest(pong);
        } catch (RemoteException e) {
            throw e;
        }
    }

    /**
     * Chiude la connessione RMI.
     * Chiama disconnect() sullo stub del server.
     */
    @Override
    public void disconnect() {
        try {
            serverStub.disconnect();
        } catch (RemoteException ignored) {}
    }
}
