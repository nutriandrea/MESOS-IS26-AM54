package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.network.messages.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * VirtualView — Pattern Proxy tra Model e rete
 * ============================================================================
 *
 * RUOLO:
 * Il Game (Model) non sa nulla della rete. Ha una lista di observer (IGameObserver)
 * e notifica un evento chiamando un metodo (es. onCardPicked) su ogni observer.
 *
 * La VirtualView si registra come observer del Game. Quando il Game la notifica,
 * la VirtualView traduce la chiamata in un messaggio di rete e lo invia
 * sulla sua ClientConnection (Socket o RMI, non importa).
 *
 * COME AVVIENE IL BROADCAST:
 * Il broadcast NON e' responsabilita' della VirtualView. Ogni VirtualView ha
 * UNA SOLA ClientConnection e manda SOLO al proprio client.
 * Il broadcast avviene nel Game: quando il Game notifica un evento, lo fa
 * su TUTTI gli observer registrati (un for-loop). Quindi:
 *
 *   Game.pickCard("Alice") -> notifyCardPicked() ->
 *     for (IGameObserver obs : observers) obs.onCardPicked()
 *       -> VV_Alice.connection.sendUpdate(CardPickedUpdate("Alice"))
 *       -> VV_Bob.connection.sendUpdate(CardPickedUpdate("Alice"))
 *       -> VV_Carlo.connection.sendUpdate(CardPickedUpdate("Alice"))
 *
 * Tutti i client ricevono l'aggiornamento, tutti vedono la board aggiornata.
 *
 * DIFF vs FULL STATE:
 * Gli update sono gia' "diff" perche' contengono SOLO i dati cambiati:
 *   - CardPickedUpdate: nickname, row, cardIndex, newFoodBalance
 *   - TotemPlacedUpdate: nickname, position
 *   - TurnChangedUpdate: nickname
 *   ecc.
 * Non viene mai inviato l'intero stato del gioco, solo la delta.
 *
 * FLUSSO COMPLETO:
 *   Client Request -> SocketClientHandler/RMIClientHandler -> Controller -> Game
 *                                                                             |
 *                                                            notifyXxx() (for-loop)
 *                                                                             |
 *                                                  +----------------+----------------+
 *                                                  |                |                |
 *                                              VirtualView      VirtualView      VirtualView
 *                                                 (Alice)          (Bob)          (Carlo)
 *                                                  |                |                |
 *                                                  v                v                v
 *                                               Update           Update           Update
 * ============================================================================
 */
public class VirtualView implements IGameObserver {

    /** Canale di rete verso il client associato a questa VirtualView */
    private final ClientConnection connection;

    /** Nickname del giocatore proprietario di questa connessione */
    private final String nickname;

    /**
     * Costruttore: crea la VirtualView quando un giocatore entra in partita.
     * Viene chiamata dal Controller.handleJoinGame() dopo la creazione della connessione.
     *
     * @param connection il canale di rete (Socket o RMI) del giocatore
     * @param nickname il nickname del giocatore
     */
    public VirtualView(ClientConnection connection, String nickname) {
        this.connection = connection;
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    /**
     * Invia un messaggio di errore SOLO a questo giocatore.
     * Usata dal Controller.signalError() quando un giocatore fa una mossa non valida.
     * Esempio: Bob pesca fuori turno -> solo Bob riceve l'errore.
     */
    public void showError(String errorMessage) {
        connection.sendUpdate(new ErrorMessageUpdate(errorMessage));
    }

    /**
     * Invia un Update a questo client.
     * Usata dal Controller per eventi di sistema (PlayerDisconnectedUpdate, PlayerReconnectedUpdate).
     * NON usata per eventi di gioco (quelli arrivano tramite i metodi observer).
     */
    public void sendUpdate(Update update) {
        connection.sendUpdate(update);
    }

    /* =====================================================================
     * --- METODI DELL'OBSERVER ---
     *
     * Il Game chiama questi metodi su TUTTI gli observer registrati.
     * Ogni VirtualView inoltra l'Update alla propria connessione.
     *
     * Esempio: quando Alice pesca una carta:
     *   1. Game.pickCard("Alice", "upper", 2)
     *   2. Game -> notifyCardPicked(p=Alice, row="upper", index=2, food=5)
     *   3. for (obs : observers) obs.onCardPicked(game, Alice, "upper", 2, 5)
     *      3a. VV_Alice.onCardPicked() -> connection.sendUpdate(CardPickedUpdate)
     *      3b. VV_Bob.onCardPicked()   -> connection.sendUpdate(CardPickedUpdate)
     *      3c. VV_Carlo.onCardPicked() -> connection.sendUpdate(CardPickedUpdate)
     *
     * Risultato: TUTTI i client vedono Alice pescare la carta e la loro
     * board locale viene aggiornata di conseguenza.
     * ===================================================================== */

    @Override
    public void onPlayerJoined(Game game, Player player) {
        // Tutti i giocatori vedono che un nuovo giocatore si e' unito
        connection.sendUpdate(new PlayerJoinedUpdate(player.getNickname()));
    }

    @Override
    public void onTotemColorChosen(Game game, Player player, TotemColors color) {
        // Tutti vedono il colore scelto dal giocatore
        connection.sendUpdate(new TotemColorUpdate(player.getNickname(), color));
    }

    @Override
    public void onTotemPlaced(Game game, Player player, int offerPosition) {
        // Tutti vedono il totem piazzato su una tessera offerta
        // Il client rimuovera' il giocatore dal turnOrderTrack e lo mettera' nell'offeringTrack
        connection.sendUpdate(new TotemPlacedUpdate(player.getNickname(), offerPosition));
    }

@Override
    public void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance) {
        // Tutti vedono la carta pescata. Il client:
        //   - Rimuove la carta dalla riga corretta (upper/lower)
        //   - Aggiunge la carta alla tribu' del giocatore
        //   - Aggiorna il cibo del giocatore
        //   - Rimette il giocatore nel turnOrderTrack
        connection.sendUpdate(new CardPickedUpdate(player.getNickname(), row, cardIndex, newFoodBalance));
    }

@Override
    public void onTotemReturned(Game game, Player player, int position, int newFoodBalance, int newAuraBalance) {
        Update update = new TotemReturnedUpdate(
                player.getNickname(),
                position,
                newFoodBalance,
                newAuraBalance
        );
        connection.sendUpdate(update);
    }

    @Override
    public void onEventResolved(Game game) {
        Board b = game.getBoard();

        // 1. Mappatura statistiche giocatori
        java.util.Map<String, EventUpdate.PlayerSnapshot> stats = new java.util.HashMap<>();
        for (Player p : game.getPlayers()) {
            stats.put(p.getNickname(), new EventUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints()
            ));
        }

        // 2. Trasformazione delle carte in Stringhe (Nomi o identificativi)
        List<String> charUp = b.getCharacterUpperRow().stream().map(c -> c.getName()).collect(Collectors.toList());
        List<String> charLow = b.getCharacterLowerRow().stream().map(c -> c.getName()).collect(Collectors.toList());
        List<String> eventUp = b.getEventUpperRow().stream().map(c -> "EVENT").collect(Collectors.toList());
        List<String> eventLow = b.getEventLowerRow().stream().map(c -> "EVENT").collect(Collectors.toList());
        List<String> buildUp = b.getBuildingCardsUpperRow().stream().map(c -> c.getBuildingType() != null ? c.getBuildingType().name() : "BUILDING").collect(Collectors.toList());
        List<String> buildLow = b.getBuildingCardsLowerRow().stream().map(c -> c.getBuildingType() != null ? c.getBuildingType().name() : "BUILDING").collect(Collectors.toList());

        // 3. Spedisci lo Snapshot completo!
        connection.sendUpdate(new EventUpdate(
                stats, charUp, charLow, eventUp, eventLow, buildUp, buildLow
        ));
    }

    /**
     * COSA FA: Notifica a tutti chi è il nuovo giocatore di turno.
     * PERCHÉ LO FA: Permette all'interfaccia grafica di attivare i pulsanti solo per quel giocatore, e mostrare agli altri la scritta "In attesa di...".
     */
    @Override
    public void onTurnChanged(Game game, Player newCurrentPlayer) {
        // Tutti vedono chi e' il nuovo giocatore di turno.
        // Se newCurrentPlayer e' null (tra una fase e l'altra), invia null.
        String turnOwner = (newCurrentPlayer != null) ? newCurrentPlayer.getNickname() : null;
        connection.sendUpdate(new TurnChangedUpdate(turnOwner));
    }

    @Override
    public void onStateChanged(Game game, GameState newState) {
        Board b = game.getBoard();

        // 1. Statistiche e Colori Giocatori
        java.util.Map<String, StateChangedUpdate.PlayerSnapshot> stats = new java.util.HashMap<>();
        for (Player p : game.getPlayers()) {
            stats.put(p.getNickname(), new StateChangedUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints(), p.getTotemColors()
            ));
        }

        // 2. Ordine di turno e Tracciato Offerte
        List<String> turnOrder = b.getTurnOrderTile().stream()
                .map(slot -> slot.getOccupiedBy() != null ? slot.getOccupiedBy().getNickname() : null)
                .collect(Collectors.toList());

        java.util.Map<Integer, String> offering = new java.util.HashMap<>();
        for (int i = 0; i < b.getOfferingTrack().size(); i++) {
            Player occ = b.getOfferingTrack().get(i).getOccupiedBy();
            if (occ != null) {
                offering.put(i, occ.getNickname());
            }
        }

        // 3. Carte sul tavolo
        List<String> charUp = b.getCharacterUpperRow().stream().map(c -> c.getName()).collect(Collectors.toList());
        List<String> charLow = b.getCharacterLowerRow().stream().map(c -> c.getName()).collect(Collectors.toList());
        List<String> eventUp = b.getEventUpperRow().stream().map(c -> "EVENT").collect(Collectors.toList());
        List<String> eventLow = b.getEventLowerRow().stream().map(c -> "EVENT").collect(Collectors.toList());
        List<String> buildUp = b.getBuildingCardsUpperRow().stream().map(c -> c.getBuildingType() != null ? c.getBuildingType().name() : "BUILDING").collect(Collectors.toList());
        List<String> buildLow = b.getBuildingCardsLowerRow().stream().map(c -> c.getBuildingType() != null ? c.getBuildingType().name() : "BUILDING").collect(Collectors.toList());

        // Spedisci il super-pacchetto!
        connection.sendUpdate(new StateChangedUpdate(
                newState.getClass().getSimpleName(),
                stats, turnOrder, offering,
                charUp, charLow, eventUp, eventLow, buildUp, buildLow
        ));
    }

    @Override
    public void onEndGame(Game game, List<Player> winners) {
        // Tutti vedono la fine della partita e i vincitori.
        // Convertiamo Player -> String per non serializzare oggetti pesanti su rete
        List<String> winnerNames = winners.stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());
        connection.sendUpdate(new EndGameUpdate(winnerNames));
    }
}
