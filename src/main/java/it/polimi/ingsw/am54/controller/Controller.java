package it.polimi.ingsw.am54.controller;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.network.server.ClientHandler;
import it.polimi.ingsw.am54.network.server.VirtualView;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controller lato Server.
 * Gestisce i comandi inviati dalla View e modifica il Model[cite: 48, 156].
 */
public class Controller {
    private final Game game;
    // Associa il nome del giocatore al suo "tubo" di rete
    private final Map<String, ClientHandler> clientHandlers = new HashMap<>();
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
     * Gestisce l'unione di un nuovo giocatore alla partita. Accetta anche l'handler e lo registra.
     */
    public void handleJoinGame(String nickname, ClientHandler handler) {
        synchronized (clientHandlers) {
            clientHandlers.put(nickname, handler);
        }

        // 1. Creiamo la vista virtuale per questo specifico giocatore
        VirtualView playerView = new VirtualView(handler);
        // 2. Iscriviamo la vista al Model così riceverà tutti i broadcast!
        game.addObserver(playerView);

        executeCommand(nickname, () -> game.joinGame(nickname));
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
     * Gestisce la pesca di una carta (Personaggio o Edificio)[cite: 37, 43].
     */
    public void handlePickCard(String nickname, String row, int cardIndex) {
        executeCommand(nickname, () -> game.pickCard(nickname, row, cardIndex));
    }

    /**
     * Metodo helper per centralizzare la sincronizzazione e la gestione errori[cite: 268].
     * Implementa il flusso: Comando -> Validazione -> [on ok] Update / [on error] SignalError[cite: 26, 27].
     */
    private void executeCommand(String nickname, GameAction action) {
        try {
            // Sincronizzazione sulla risorsa condivisa (il Model)[cite: 268, 270].
            synchronized (game) {
                action.execute();
            }
            // Se l'azione ha successo, il Model notificherà gli Observer (Virtual Views)[cite: 11, 152].
            LOGGER.info("Comando eseguito con successo per: " + nickname);

        } catch (Exception e) {
            // Se il Model lancia un'eccezione (es. IllegalStateException dallo Stato),
            // catturiamo l'errore e lo segnaliamo al client[cite: 27, 41, 188].
            signalError(nickname, e.getMessage());
        }
    }

    /**
     * Recupera l'handler dalla mappa e invia l'errore.
     */
    private void signalError(String nickname, String message) {
        LOGGER.warning("Segnalazione errore a " + nickname + ": " + message);

        ClientHandler handler;
        synchronized (clientHandlers) {
            handler = clientHandlers.get(nickname);
        }

        if (handler != null) {
            handler.sendError(message);
        } else {
            LOGGER.severe("Impossibile inviare errore: handler non trovato per " + nickname);
        }
    }

    // Per pulire il registro quando un client si disconnette
    public void removeClient(String nickname) {
        synchronized (clientHandlers) {
            clientHandlers.remove(nickname);
        }
    }
}
