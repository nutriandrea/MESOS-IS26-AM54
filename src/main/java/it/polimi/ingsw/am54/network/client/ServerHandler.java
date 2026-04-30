package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import java.io.ObjectInputStream;

/**
 * È un Thread indipendente che rimane in ascolto continuo dei messaggi asincroni (Update)
 * provenienti dal server.
 * Viene utilizzato principalmente in accoppiata con i Socket TCP.
 */
public class ServerHandler implements Runnable {
    private final ObjectInputStream in;
    private final ClientModel clientModel;
    private volatile boolean running = true;

    public ServerHandler(ObjectInputStream in, ClientModel clientModel) {
        this.in = in;
        this.clientModel = clientModel;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Update message = (Update) in.readObject();
                handleUpdate(message);
            } catch (Exception e) {
                if (running) stop();
            }
        }
    }

    public void stop() { this.running = false; }

    // Modifica il clientModel in base al messaggio ricevuto
    private void handleUpdate(Update message) {
        if (message instanceof PlayerJoinedUpdate msg) {
            clientModel.updatePlayerJoined(msg.getNickname());
        }
        else if (message instanceof TotemPlacedUpdate msg) {
            clientModel.updateTotemPlacement(msg.getNickname(), msg.getOfferPosition());
        }
        else if (message instanceof CardPickedUpdate msg) {
            clientModel.updateCardPicked(msg.getNickname(), msg.getRow(), msg.getCardIndex(), msg.getNewFoodBalance());
        }
        else if (message instanceof TurnChangedUpdate msg) {
            clientModel.updateTurn(msg.getNickname());
        }
        else if (message instanceof TotemReturnedUpdate msg) {
            clientModel.updateTotemReturned(msg.getNickname(), msg.getNewFoodBalance(), msg.getNewAuraBalance());
        }
        else if (message instanceof StateChangedUpdate msg) {
            clientModel.updateState(msg);
        }
        else if (message instanceof TotemColorUpdate msg) {
            clientModel.updateTotemColor(msg.getNickname(), msg.getColor());
        }
        else if (message instanceof EndGameUpdate msg) {
            clientModel.updateEndGame(msg.getWinners());
        }
        else if (message instanceof ErrorMessageUpdate msg) {
            clientModel.updateError(msg.getMessage());
        }
        else if (message instanceof EventUpdate msg) {
            clientModel.updateEventSnapshot(msg);
        }
    }
}