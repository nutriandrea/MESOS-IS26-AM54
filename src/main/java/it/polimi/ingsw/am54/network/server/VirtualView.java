package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.network.messages.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** Riferimento al Controller per comunicazioni inverse */
    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

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
     */
    public void showError(String errorMessage) {
        connection.notifyError(errorMessage);
    }

    /**
     * Notifica a questa vista che un altro giocatore si è disconnesso.
     */
    public void showPlayerDisconnected(String disconnectedNickname) {
        connection.notifyPlayerDisconnected(disconnectedNickname);
    }

    /**
     * Notifica a questa vista che un altro giocatore si è riconnesso.
     */
    public void showPlayerReconnected(String reconnectedNickname) {
        connection.notifyPlayerReconnected(reconnectedNickname);
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
        connection.notifyPlayerJoined(player.getNickname());
    }

    @Override
    public void onTotemColorChosen(Game game, Player player, TotemColors color) {
        // Tutti vedono il colore scelto dal giocatore
        connection.notifyTotemColor(player.getNickname(), color);
    }

    @Override
    public void onTotemPlaced(Game game, Player player, int offerPosition) {
        // Tutti vedono il totem piazzato su una tessera offerta
        // Il client rimuovera' il giocatore dal turnOrderTrack e lo mettera' nell'offeringTrack
        connection.notifyTotemPlaced(player.getNickname(), offerPosition);}

    private Map<Integer, String> generateOfferingSnapshot(Board b) {
        Map<Integer, String> offering = new LinkedHashMap<>();
        for (int i = 0; i < b.getOfferingTrack().size(); i++) {
            OfferingTile tile = b.getOfferingTrack().get(i);
            String occName = (tile.getOccupiedBy() != null) ? tile.getOccupiedBy().getNickname() : "VUOTO";

            // Se la tessera è vuota mostriamo il totale (cardsFromUpper), se è occupata mostriamo i rimanenti (remainingUpperPicks)
            int up = tile.getRemainingUpperPicks();
            int low = tile.getRemainingLowerPicks();

            offering.put(i, "[Tessera " + tile.getId() + " | Pick Su:" + up + " Giù:" + low + " | Bonus 🍖:+" + tile.getFoodBonus() + "] -> " + occName);
        }
        return offering;
    }

    @Override
    public void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {
        // Tutti vedono la carta pescata. Il client:
        //   - Rimuove la carta dalla riga corretta (upper/lower)
        //   - Aggiunge la carta alla tribu' del giocatore
        //   - Aggiorna il cibo del giocatore
        //   - Rimette il giocatore nel turnOrderTrack
        Map<Integer, String> currentOffering = generateOfferingSnapshot(game.getBoard());

        connection.notifyCardPicked(player.getNickname(), row, cardIndex, newFoodBalance, newMagicBalance, currentOffering);
   }

    @Override
    public void onTotemReturned(Game game, Player player, int position, int newFoodBalance, int newAuraBalance) {
        connection.notifyTotemReturned(player.getNickname(), position, newFoodBalance, newAuraBalance);
    }

    @Override
    public void onEventResolved(Game game, EventCard resolvedEvent) {
        Board b = game.getBoard();

        // 0. Recupera il vero nome dell'evento!
        String eventName = resolvedEvent.getName();

        // 1. Mappatura statistiche giocatori
        Map<String, EventUpdate.PlayerSnapshot> stats = new HashMap<>();
        for (Player p : game.getPlayers()) {
            List<String> tribeStr = p.getMyTribe().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
            List<String> buildStr = p.getBuildings().stream().map(this::formatBuildingForClient).collect(Collectors.toList());
            stats.put(p.getNickname(), new EventUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints(), tribeStr, buildStr
            ));
        }

        // 2. Trasformazione delle carte in Stringhe
        List<String> charUp = b.getCharacterUpperRow().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
        List<String> charLow = b.getCharacterLowerRow().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
        List<String> eventUp = b.getEventUpperRow().stream().map(this::formatEventForClient).collect(Collectors.toList());
        List<String> eventLow = b.getEventLowerRow().stream().map(this::formatEventForClient).collect(Collectors.toList());
        List<String> buildUp = b.getBuildingCardsUpperRow().stream().map(this::formatBuildingForClient).collect(Collectors.toList());
        List<String> buildLow = b.getBuildingCardsLowerRow().stream().map(this::formatBuildingForClient).collect(Collectors.toList());

        // 3. Spedisci lo Snapshot completo aggiungendo il nome in cima!
        connection.notifyEventResolved(eventName, stats, charUp, charLow, eventUp, eventLow, buildUp, buildLow);
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
        connection.notifyTurnChanged(turnOwner);
    }

    @Override
    public void onStateChanged(Game game, GameState newState) {
        // 1. Statistiche e Carte Giocatori (sempre valide)
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new java.util.LinkedHashMap<>();
        for (Player p : game.getPlayers()) {
            List<String> tribeStr = p.getMyTribe().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
            List<String> buildStr = p.getBuildings().stream().map(this::formatBuildingForClient).collect(Collectors.toList());
            stats.put(p.getNickname(), new StateChangedUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints(), p.getTotemColors(), tribeStr, buildStr
            ));
        }

        // Inizializziamo le strutture della Board a vuote
        List<String> turnOrder = new java.util.ArrayList<>();
        Map<Integer, String> offering = new java.util.LinkedHashMap<>();
        List<String> charUp = new java.util.ArrayList<>();
        List<String> charLow = new java.util.ArrayList<>();
        List<String> eventUp = new java.util.ArrayList<>();
        List<String> eventLow = new java.util.ArrayList<>();
        List<String> buildUp = new java.util.ArrayList<>();
        List<String> buildLow = new java.util.ArrayList<>();

        Board b = game.getBoard();

        // =========================================================
        // FIX: Se la Board esiste (siamo in gioco), raccogliamo i dati.
        // Se è null (siamo in Lobby), skippiamo per evitare il NullPointer!
        // =========================================================
        if (b != null) {
            turnOrder = b.getTurnOrderTile().stream()
                    .map(slot -> {
                        int bonus = slot.getBonusFood();
                        String foodStr = (bonus > 0 ? "+" : "") + bonus;
                        String auraWarning = (bonus < 0) ? " (-2 ✨ se non hai cibo)" : "";
                        String occName = (slot.getOccupiedBy() != null) ? slot.getOccupiedBy().getNickname() : "VUOTO";
                        return "[Slot " + slot.getPosition() + " | Cibo: " + foodStr + auraWarning + "] -> " + occName;
                    })
                    .collect(Collectors.toList());

            for (int i = 0; i < b.getOfferingTrack().size(); i++) {
                OfferingTile tile = b.getOfferingTrack().get(i);
                String occName = (tile.getOccupiedBy() != null) ? tile.getOccupiedBy().getNickname() : "VUOTO";
                offering.put(i, "[Tessera " + tile.getId() + " | Pick Su:" + tile.getRemainingUpperPicks() + " Giù:" + tile.getRemainingLowerPicks() + " | Bonus 🍖:+" + tile.getFoodBonus() + "] -> " + occName);
            }

            charUp = b.getCharacterUpperRow().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
            charLow = b.getCharacterLowerRow().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
            eventUp = b.getEventUpperRow().stream().map(this::formatEventForClient).collect(Collectors.toList());
            eventLow = b.getEventLowerRow().stream().map(this::formatEventForClient).collect(Collectors.toList());
            buildUp = b.getBuildingCardsUpperRow().stream().map(this::formatBuildingForClient).collect(Collectors.toList());
            buildLow = b.getBuildingCardsLowerRow().stream().map(this::formatBuildingForClient).collect(Collectors.toList());
        }

        // Spedisci il pacchetto (sicuro!)
        connection.notifyStateChanged(
                newState.getClass().getSimpleName(), game.getCurrentEra(), game.getRoundCount(),
                stats, turnOrder, offering,
                charUp, charLow, eventUp, eventLow, buildUp, buildLow
        );
    }

    @Override
    public void onEndGame(Game game, List<Player> winners) {
        List<String> winnerNames = winners.stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());

        // Calcoliamo la mappa dei punteggi reali qui sul Server
        java.util.Map<String, Integer> finalScores = new java.util.HashMap<>();
        for (Player p : game.getPlayers()) {
            finalScores.put(p.getNickname(), game.calculatePlayerScore(p));
        }

        // Inviamo i dati completi alla connessione
        connection.notifyEndGame(winnerNames, finalScores, game.getPlayers().size());
    }

    //PER LA RECONNECT
    public void sendFullStateOnReconnect(Game game) {
        // 1. Statistiche e Carte Giocatori
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new java.util.LinkedHashMap<>();
        for (Player p : game.getPlayers()) {
            List<String> tribeStr = p.getMyTribe().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
            List<String> buildStr = p.getBuildings().stream().map(this::formatBuildingForClient).collect(Collectors.toList());
            stats.put(p.getNickname(), new StateChangedUpdate.PlayerSnapshot(
                    p.getFood(), p.getAuraPoints(), p.getMagicPoints(), p.getTotemColors(), tribeStr, buildStr
            ));
        }

        List<String> turnOrder = new java.util.ArrayList<>();
        Map<Integer, String> offering = new java.util.LinkedHashMap<>();
        List<String> charUp = new java.util.ArrayList<>();
        List<String> charLow = new java.util.ArrayList<>();
        List<String> eventUp = new java.util.ArrayList<>();
        List<String> eventLow = new java.util.ArrayList<>();
        List<String> buildUp = new java.util.ArrayList<>();
        List<String> buildLow = new java.util.ArrayList<>();

        Board b = game.getBoard();

        // Protezione Board
        if (b != null) {
            turnOrder = b.getTurnOrderTile().stream()
                    .map(slot -> {
                        int bonus = slot.getBonusFood();
                        String foodStr = (bonus > 0 ? "+" : "") + bonus;
                        String auraWarning = (bonus < 0) ? " (-2 ✨ se non hai cibo)" : "";
                        String occName = (slot.getOccupiedBy() != null) ? slot.getOccupiedBy().getNickname() : "VUOTO";
                        return "[Slot " + slot.getPosition() + " | Cibo: " + foodStr + auraWarning + "] -> " + occName;
                    })
                    .collect(Collectors.toList());

            for (int i = 0; i < b.getOfferingTrack().size(); i++) {
                OfferingTile tile = b.getOfferingTrack().get(i);
                String occName = (tile.getOccupiedBy() != null) ? tile.getOccupiedBy().getNickname() : "VUOTO";
                offering.put(i, "[Tessera " + tile.getId() + " | Pick Su:" + tile.getRemainingUpperPicks() + " Giù:" + tile.getRemainingLowerPicks() + " | Bonus 🍖:+" + tile.getFoodBonus() + "] -> " + occName);
            }

            charUp = b.getCharacterUpperRow().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
            charLow = b.getCharacterLowerRow().stream().map(this::formatCharacterForClient).collect(Collectors.toList());
            eventUp = b.getEventUpperRow().stream().map(this::formatEventForClient).collect(Collectors.toList());
            eventLow = b.getEventLowerRow().stream().map(this::formatEventForClient).collect(Collectors.toList());
            buildUp = b.getBuildingCardsUpperRow().stream().map(this::formatBuildingForClient).collect(Collectors.toList());
            buildLow = b.getBuildingCardsLowerRow().stream().map(this::formatBuildingForClient).collect(Collectors.toList());
        }

        // Manda il pacchetto solo a QUESTA connessione
        connection.notifyStateChanged(
                game.getCurrentState().getClass().getSimpleName(),
                game.getCurrentEra(), game.getRoundCount(),
                stats, turnOrder, offering,
                charUp, charLow, eventUp, eventLow, buildUp, buildLow
        );
    }

    private String formatCharacterForClient(CharacterCard c) {
        if (c == null) return null;
        // Esempio formato standard unificato: "8|1|2|HUNTER|0|1" (ID|Era|MinGiocatori|Nome|Livello|Aura)
        return c.getImageId() + "|" + c.getEra() + "|" + c.getMinPlayers() + "|" + c.getName() + "|" + c.getLevel() + "|" + c.getAura();
    }

    private String formatBuildingForClient(BuildingCard b) {
        if (b == null) return null;
        String nomeEdificio = b.getName() != null ? b.getName() : "Edificio Speciale";
        // Esempio formato standard unificato: "115|3|0|BUILDING|3|2|NomeEdificio"
        return b.getImageId() + "|" + b.getEra() + "|0|BUILDING|" + b.getCost() + "|" + b.getFinalAura() + "|" + nomeEdificio;
    }

    private String formatEventForClient(EventCard e) {
        if (e == null) return null;
        // Esempio formato standard unificato: "84|1|0|EVENT|NomeEvento"
        return e.getImageId() + "|" + e.getEra() + "|0|EVENT|" + e.getName();
    }
    /**
     * Invia notifica di sospensione SOLO a questo giocatore.
     * Usata dal Controller per broadcast.
     */
    public void showGameSuspended() {
        connection.notifyGameSuspended();
    }

    /**
     * Invia notifica di ripresa SOLO a questo giocatore.
     * Usata dal Controller per broadcast.
     */
    public void showGameResumed() {
        connection.notifyGameResumed();
    }
}
