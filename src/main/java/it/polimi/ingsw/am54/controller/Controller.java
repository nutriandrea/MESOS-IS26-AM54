package it.polimi.ingsw.am54.controller;

import it.polimi.ingsw.am54.model.Game;
import java.util.logging.Logger;

/**
 * Controller lato Server.
 * Gestisce i comandi inviati dalla View e modifica il Model[cite: 48, 156].
 */
public class Controller {
    private final Game game;
    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    public Controller(Game game) {
        this.game = game;
    }

    /**
     * Gestisce l'unione di un nuovo giocatore alla partita.
     */
    public void handleJoinGame(String nickname) {
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
     * Invia un segnale di errore alla View del client specifico[cite: 39, 41].
     */
    private void signalError(String nickname, String message) {
        LOGGER.warning("Segnalazione errore a " + nickname + ": " + message);
        // Qui si integrerà il middleware di rete (Socket/RMI) per spedire il messaggio[cite: 17, 18].
    }

    /**
     * Interfaccia funzionale interna per rappresentare un'azione sul Model.
     */
    @FunctionalInterface
    private interface GameAction {
        void execute() throws Exception;
    }
}
