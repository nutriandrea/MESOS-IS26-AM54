package it.polimi.ingsw.am54.controller;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import it.polimi.ingsw.am54.network.server.VirtualView;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controller lato Server.
 * Gestisce i comandi inviati dalla View e modifica il Model.
 */
public class Controller {
    private final Game game;
    // Il Controller memorizza ESCLUSIVAMENTE le Viste. Ignora l'esistenza dei Socket o di RMI.
    private final Map<String, VirtualView> virtualViews = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    public Controller(Game game) {
        this.game = game;
    }

    /**
     * Interfaccia funzionale interna per rappresentare un'azione sul Model.
     */
    @FunctionalInterface
    private interface GameAction {
        void execute() throws Exception;
    }

    /**
     * Gestisce l'unione di un nuovo giocatore alla partita.
     */
    public void handleJoinGame(String nickname, ClientConnection connection) {
        // 1. Creiamo la vista (ma non sappiamo ancora se il Model l'accetterà)
        VirtualView playerView = new VirtualView(connection, nickname);

        // La agganciamo temporaneamente per permetterle di ricevere eventuali messaggi
        synchronized (virtualViews) {
            virtualViews.put(nickname, playerView);
        }
        game.addObserver(playerView);

        // 2. Passiamo la palla al MODEL
        executeCommand(nickname, () -> {
            try {

                // IL MODEL DECIDE QUI:
                // Se il nome è già preso, joinGame lancerà un'eccezione!
                game.joinGame(nickname);

            } catch (Exception e) {
                // IL MODEL HA DETTO NO! (Nome duplicato, partita piena, ecc.)
                // Dobbiamo fare "marcia indietro" e pulire il Controller.
                synchronized (virtualViews) {
                    virtualViews.remove(nickname);
                    game.removeObserver(playerView);
                }

                // Rilanciamo l'eccezione così executeCommand la cattura
                // e chiama in automatico signalError() per avvisare il client!
                throw e;
            }
        });
    }

    /**
     * Gestisce la scelta del colore del totem.
     */
    public void handleChooseTotemColor(String nickname, String color) {
        executeCommand(nickname, () -> game.chooseTotemColor(nickname, color));
    }

    /**
     * Avvia il setup del gioco (solo se le condizioni sono soddisfatte).
     */
    public void handleSetupGame(String nickname) {
        executeCommand(nickname, game::setupGame);
    }

    /**
     * Gestisce il piazzamento del totem sulle tessere offerta.
     */
    public void handlePlaceTotem(String nickname, int offerPosition) {
        executeCommand(nickname, () -> game.placeTotem(nickname, offerPosition));
    }

    /**
     * Gestisce la pesca di una carta (Personaggio o Edificio).
     */
    public void handlePickCard(String nickname, String row, int cardIndex) {
        executeCommand(nickname, () -> game.pickCard(nickname, row, cardIndex));
    }

    /**
     * Metodo helper per centralizzare la sincronizzazione e la gestione errori.
     * Implementa il flusso: Comando -> Validazione -> [on ok] Update / [on error] SignalError.
     */
    private void executeCommand(String nickname, GameAction action) {
        try {
            // Sincronizzazione sulla risorsa condivisa (il Model).
            synchronized (game) {
                action.execute();
            }
            // Se l'azione ha successo, il Model notificherà gli Observer (Virtual Views) in automatico.
            LOGGER.info("Comando eseguito con successo per: " + nickname);

        } catch (Exception e) {
            // Se il Model lancia un'eccezione (es. "Non è il tuo turno!"), la catturiamo.
            signalError(nickname, e.getMessage());
        }
    }

    /**
     * Recupera la VirtualView dalla mappa e le chiede di inviare l'errore.
     */
    private void signalError(String nickname, String message) {
        LOGGER.warning("Segnalazione errore a " + nickname + ": " + message);

        VirtualView view;
        synchronized (virtualViews) {
            // CORREZIONE: Usiamo la mappa corretta!
            view = virtualViews.get(nickname);
        }

        if (view != null) {
            // CORREZIONE: Chiamiamo il metodo della VirtualView!
            view.showError(message);
        } else {
            LOGGER.severe("Impossibile inviare errore: vista non trovata per " + nickname);
        }
    }

    /**
     * Pulisce il registro quando un client si disconnette.
     */
    public void removeClient(String nickname) {
        synchronized (virtualViews) {
            // CORREZIONE: Rimuoviamo la vista dalla mappa corretta
            VirtualView view = virtualViews.remove(nickname);

            // CORREZIONE VITALE: Dobbiamo dire al Model di smettere di parlare con questa Vista!
            if (view != null) {
                game.removeObserver(view);
                LOGGER.info("Giocatore " + nickname + " rimosso dal server.");
            }
        }
    }
}