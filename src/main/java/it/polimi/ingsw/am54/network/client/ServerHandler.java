package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.view.CLIView;
import java.io.ObjectInputStream;
import java.util.List;

public class ServerHandler implements Runnable {

    private ObjectInputStream in;
    private volatile boolean running;
    private final Client client;
    private CLIView cliView;

    public ServerHandler(ObjectInputStream in, Client client) {
        this.in = in;
        this.client = client;
        this.running = true;
    }

    public void setCLIView(CLIView view) {
        this.cliView = view;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Update message = (Update) in.readObject();
                handleUpdate(message);
            } catch (Exception e) {
                if (running) {
                    System.err.println("[ServerHandler] Connessione col server persa: " + e.getMessage());
                    stop();
                }
            }
        }
    }

    private void handleUpdate(Update message) {
        if (message instanceof PingRequest ping) {
            client.sendPong(ping.getTimestamp());
            return;
        }

        if (cliView == null) {
            System.out.println("[ServerHandler] Ricevuto: " + message.getClass().getSimpleName() + " (CLIView null)");
            return;
        }
        System.out.println("[ServerHandler] Elaboro: " + message.getClass().getSimpleName());

        if (message instanceof PlayerJoinedUpdate update) {
            cliView.updatePlayerJoined(update.getNickname());
        } else if (message instanceof PlayerReconnectedUpdate update) {
            cliView.updatePlayerReconnected(update.getNickname());
        } else if (message instanceof TotemColorUpdate update) {
            cliView.updateTotemChosen(update.getNickname(), update.getColor().name());
        } else if (message instanceof TotemPlacedUpdate update) {
            cliView.updateTotemPlaced(update.getNickname(), update.getOfferPosition());
        } else if (message instanceof CardPickedUpdate update) {
            cliView.updateCardPicked(update.getNickname(), update.getRow(), update.getCardIndex());
        } else if (message instanceof TurnChangedUpdate update) {
            cliView.updateTurnChanged(update.getNickname());
        } else if (message instanceof StateChangedUpdate update) {
            cliView.updateStateChanged(update.getStateName());
        } else if (message instanceof BoardStateUpdate update) {
            cliView.updateGameSetup(
                update.getCurrentEra(),
                update.getPlayerNames().size(),
                update.getPlayerNames(),
                update.getOfferingPositions(),
                update.getOfferingFood(),
                update.getUpperRowCards(),
                update.getLowerRowCards(),
                update.getUpperBuildings(),
                update.getLowerBuildings()
            );
        } else if (message instanceof EndGameUpdate update) {
            cliView.updateEndGame(update.getWinners());
        } else if (message instanceof ErrorMessageUpdate update) {
            System.err.println("\n❌ ERRORE: " + update.getMessage() + "\n");
        } else {
            System.out.println("[ServerHandler] Ricevuto: " + message.getClass().getSimpleName());
        }
    }

    public void stop() {
        this.running = false;
    }
}