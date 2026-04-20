package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import java.io.ObjectInputStream;

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

    private void handleUpdate(Update message) {
        // Uso i getter definiti nei tuoi messaggi (quelli del primo prompt)
        if (message instanceof PlayerJoinedUpdate msg) {
            clientModel.updatePlayerJoined(msg.getNickname());
        }
        else if (message instanceof TotemPlacedUpdate msg) {
            // Qui uso getOfferPosition() perché è un int, come nel tuo messaggio
            clientModel.updateTotemPlacement(msg.getNickname(), msg.getOfferPosition());
        }
        else if (message instanceof CardPickedUpdate msg) {
            // Qui uso i getter che mi hai appena mandato: getRow() e getCardIndex()
            clientModel.updateCardPicked(msg.getNickname(), msg.getRow(), msg.getCardIndex());
        }
        else if (message instanceof TurnChangedUpdate msg) {
            clientModel.updateTurn(msg.getNickname());
        }
        else if (message instanceof StateChangedUpdate msg) {
            // Se il tuo messaggio ha getState() invece di getNewState()
            // clientModel.updateState(msg.getState());
        }
    }

    public void stop() { this.running = false; }
}