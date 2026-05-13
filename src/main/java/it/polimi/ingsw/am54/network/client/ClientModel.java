package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.EventUpdate;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ============================================================================
 * ClientModel — Modello leggero lato client
 * ============================================================================
 *
 * RUOLO:
 * Mantieni uno stato locale del gioco sincronizzato con il server.
 * Riceve gli Update dal ServerHandler (TCP) o RMIServerConnection (RMI)
 * e aggiorna il proprio stato di conseguenza.
 *
 * PERCHE' ESISTE:
 * Il Model lato server (Game.java) e' pesante: contiene la logica di gioco,
 * le carte, gli effetti, ecc. Il client non ha bisogno di tutto questo.
 * Il ClientModel contiene SOLO i dati necessari per visualizzare lo stato
 * del gioco: nomi giocatori, carte sul tavolo, colori, turni, ecc.
 *
 * SINCRONIZZAZIONE:
 * Tutti i metodi di aggiornamento sono synchronized per garantire thread-safety.
 * Il ServerHandler gira su un thread separato e aggiorna il ClientModel
 * in modo concorrente rispetto al thread della UI.
 *
 * STRUTTURA:
 * 1. STATO GLOBALE: round, era, stato corrente, giocatore di turno
 * 2. TABELLONE: carte sul tavolo, turn order, offering track
 * 3. GIOCATORI: nickname, colori, risorse (food, aura), inventario carte
 * 4. EXTRA: vincitori, errori, giocatori disconnessi
 * ============================================================================
 */
public class ClientModel {

    //------------------------------------------------------------------------------
    //Attributi e metodi per notificare UI di cambiamenti
    private Runnable onModelUpdatedCallback;
    private Timer debounceTimer;

    private String lastSystemMessage;

    public void setOnModelUpdatedCallback(Runnable callback) {
        this.onModelUpdatedCallback = callback;
    }

    private void notifyUI() {
        if (onModelUpdatedCallback != null) {
            // Se c'è già un aggiornamento in coda, lo annulliamo
            if (debounceTimer != null) {
                debounceTimer.cancel();
            }
            // Ne creiamo uno nuovo che aspetterà 100 millisecondi prima di ridisegnare
            debounceTimer = new Timer(true); // "true" significa che non bloccherà la chiusura del gioco
            debounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    onModelUpdatedCallback.run();
                }
            }, 100);
        }
    }
    //------------------------------------------------------------------------------

    // --- 1. STATO GLOBALE ---
    /** Round corrente (inizia da 1 dopo il setup) */
    private int currentRound = 1;

    /** Era corrente (inizia da 1) */
    private int currentEra = 1;

    /** Stato corrente del gioco (es. "Lobby", "SetUpGameState", "PickingState") */
    private String currentState = "Lobby";

    /** Nickname del giocatore il cui turno e' attivo */
    private String currentPlayer;

    // --- 2. TABELLONE (BOARD) ---
    /** Ordine di turno: lista di nickname in ordine di turno */
    private final List<String> turnOrderTrack = new ArrayList<>();

    /** Tessere offerta: mappa posizione -> nickname del giocatore che l'ha occupata */
    private final Map<Integer, String> offeringTrack = new TreeMap<>();

    /** Carte personaggio nella fila superiore */
    private final List<String> characterUpperRow = new ArrayList<>();

    /** Carte personaggio nella fila inferiore */
    private final List<String> characterLowerRow = new ArrayList<>();

    /** Carte evento nella fila superiore */
    private final List<String> eventUpperRow = new ArrayList<>();

    /** Carte evento nella fila inferiore */
    private final List<String> eventLowerRow = new ArrayList<>();

    /** Carte edificio nella fila superiore */
    private final List<String> upperBuildingRow = new ArrayList<>();

    /** Carte edificio nella fila inferiore */
    private final List<String> lowerBuildingRow = new ArrayList<>();

    // --- 3. STATO GIOCATORI (MAPPE indicizzate per Nickname) ---
    /** Lista di tutti i giocatori in partita */
    private final List<String> players = new ArrayList<>();

    /** Mappa nickname -> colore del totem */
    private final Map<String, TotemColors> playerColors = new HashMap<>();

    /** Mappa nickname -> cibo disponibile */
    private final Map<String, Integer> playerFood = new HashMap<>();

    /** Mappa nickname -> punti aura */
    private final Map<String, Integer> playerAura = new HashMap<>();

    /** Mappa nickname -> lista delle carte tribu' (personaggi) possedute */
    private final Map<String, List<String>> playerTribe = new HashMap<>();

    /** Mappa nickname -> lista delle carte edificio possedute */
    private final Map<String, List<String>> playerBuildings = new HashMap<>();

    // --- 4. EXTRA ---
    /** Lista dei vincitori (popolata alla fine della partita) */
    private List<String> winners = new ArrayList<>();

    /** Ultimo messaggio di errore ricevuto */
    private String lastErrorMessage;

    /**
     * Set di nickname dei giocatori temporaneamente disconnessi.
     * Popolato quando si riceve PlayerDisconnectedUpdate.
     * Svuotato quando si riceve PlayerReconnectedUpdate o PlayerJoinedUpdate.
     *
     * La UI puo' usare questo set per mostrare un indicatore visivo
     * (es. icona grigia) accanto al nome del giocatore disconnesso.
     */
    private final Set<String> disconnectedPlayers = new HashSet<>();

    // =====================================================================
    // METODI DI AGGIORNAMENTO (chiamati dal ServerHandler / RMIServerConnection)
    // Tutti synchronized per thread-safety
    // =====================================================================

    /**
     * Aggiornato quando un nuovo giocatore si unisce alla partita.
     *
     * COSA FA:
     * - Aggiunge il giocatore alla lista players
     * - Aggiunge il giocatore al turnOrderTrack (ordine di turno)
     * - Rimuove il giocatore dal set dei disconnessi (se era presente)
     *
     * NOTA: se un giocatore si riconnette, questo metodo viene chiamato
     * anche per lui, quindi il set dei disconnessi viene automaticamente pulito.
     */
    public synchronized void updatePlayerJoined(String nickname) {
        if (!players.contains(nickname)) {
            players.add(nickname);
            turnOrderTrack.add(nickname);
        }
        disconnectedPlayers.remove(nickname);

        this.lastSystemMessage = "👋 NUOVO GIOCATORE: " + nickname.toUpperCase() + " è entrato nella lobby!";
        notifyUI();
    }

    /**
     * Aggiornato quando un giocatore viene marcato come disconnesso.
     *
     * COSA FA:
     * - Aggiunge il nickname al set disconnectedPlayers
     *
     * LA UI DOVREBBE:
     * - Mostrare un indicatore visivo (es. icona "offline")
     * - Disabilitare le interazioni con il giocatore disconnesso
     * - Continuare a mostrare il suo stato (carte, risorse, posizione)
     */
    public synchronized void updatePlayerDisconnected(String nickname) {
        disconnectedPlayers.add(nickname);
        // AGGIUNTA: Messaggio di sistema per le cadute
        this.lastSystemMessage = "⚠️ ATTENZIONE: " + nickname.toUpperCase() + " si è disconnesso!";
        notifyUI();
    }

    /**
     * Aggiornato quando un giocatore si riconnette.
     *
     * COSA FA:
     * - Rimuove il nickname dal set disconnectedPlayers
     *
     * LA UI DOVREBBE:
     * - Rimuovere l'indicatore di disconnessione
     * - Riabilitare le interazioni con il giocatore
     */
    public synchronized void updatePlayerReconnected(String nickname) {
        disconnectedPlayers.remove(nickname);
        // AGGIUNTA: Messaggio di sistema per il rientro
        this.lastSystemMessage = "🟢 BENTORNATO: " + nickname.toUpperCase() + " è tornato in partita!";
        notifyUI();
    }

    /** Controlla se un giocatore e' attualmente disconnesso */
    public synchronized boolean isPlayerDisconnected(String nickname) {
        return disconnectedPlayers.contains(nickname);
    }

    /** Restituisce una copia del set dei giocatori disconnessi */
    public synchronized Set<String> getDisconnectedPlayers() {
        return new HashSet<>(disconnectedPlayers);
    }


    /** Aggiornato quando cambia il giocatore di turno */
    public synchronized void updateTurn(String nickname) {
        this.currentPlayer = nickname;
        notifyUI();
    }

    /**
     * Aggiornato quando un giocatore piazza il totem su una tessera offerta.
     *
     * COSA FA:
     * - Rimuove il giocatore dal turnOrderTrack (non e' piu' in ordine di turno)
     * - Aggiunge il giocatore all'offeringTrack alla posizione specificata
     */
    public synchronized void updateTotemPlacement(String nickname, int position) {
        turnOrderTrack.removeIf(s -> s.contains(nickname));
        String oldTile = offeringTrack.get(position);
        if (oldTile != null) {
            offeringTrack.put(position, oldTile.replace("VUOTO", nickname));
        }
        notifyUI();
    }

    /**
     * Aggiornato quando un giocatore pesca una carta.
     *
     * LOGICA COMPLESSA:
     * 1. Determina se la carta e' nella fila upper o lower
     * 2. Trova l'indice corretto nella riga giusta
     * 3. Rimuove la carta dalla board (setta a null)
     * 4. Aggiunge la carta alla tribu' o agli edifici del giocatore
     * 5. Aggiorna il cibo del giocatore (se ha pescato un edificio)
     * 6. Rimette il giocatore nel turnOrderTrack (il totem torna disponibile)
     * 7. Rimuove il giocatore dall'offeringTrack
     *
     * @param nickname il giocatore che ha pescato
     * @param row "upper" o "lower"
     * @param index indice della carta nella riga
     * @param newFoodBalance nuovo bilancio cibo del giocatore
     */
    public synchronized void updateCardPicked(String nickname, String row, int index, int newFoodBalance, java.util.Map<Integer, String> updatedOffering) {
        boolean isUpper = row.equalsIgnoreCase("upper");
        if(isUpper){
            if(index < characterUpperRow.size()) {
                // Carta personaggio dalla fila superiore
                String temp = characterUpperRow.get(index);
                characterUpperRow.remove(index);
                playerTribe.get(nickname).add(temp);
            } else {
                // Carta edificio dalla fila superiore
                int buildingIndex = index - characterUpperRow.size();
                String temp = upperBuildingRow.get(buildingIndex);
                upperBuildingRow.remove(buildingIndex);
                playerFood.put(nickname, newFoodBalance);
                playerBuildings.get(nickname).add(temp);
            }
        } else {
            if(index < characterLowerRow.size()) {
                // Carta personaggio dalla fila inferiore
                String temp = characterLowerRow.get(index);
                characterLowerRow.remove(index);
                playerTribe.get(nickname).add(temp);
            } else {
                // Carta edificio dalla fila inferiore
                int buildingIndex = index - characterLowerRow.size();
                String temp = lowerBuildingRow.get(buildingIndex);
                lowerBuildingRow.remove(buildingIndex);
                playerFood.put(nickname, newFoodBalance);
                playerBuildings.get(nickname).add(temp);
            }
        }

        playerFood.put(nickname, newFoodBalance);
        this.offeringTrack.clear();
        this.offeringTrack.putAll(updatedOffering);
        notifyUI();
    }

    public synchronized void updateTotemReturned(String nickname, int newFoodBalance, int newAuraBalance) {
        // Rimuove il totem dalle offerte
        offeringTrack.values().remove(nickname);

        // Lo rimette in fondo all'ordine di turno per il prossimo round
        if (!turnOrderTrack.contains(nickname)) {
            turnOrderTrack.add(nickname);
        }
        playerFood.put(nickname, newFoodBalance);
        playerAura.put(nickname, newAuraBalance);
        notifyUI();
    }

    public synchronized void updateEventSnapshot(EventUpdate msg) {
        // Aggiorna Risorse di TUTTI i Giocatori simultaneamente
        msg.getPlayersStats().forEach((nick, stats) -> {
            playerFood.put(nick, stats.food);
            playerAura.put(nick, stats.aura);
            playerTribe.put(nick, new ArrayList<>(stats.tribe));
            playerBuildings.put(nick, new ArrayList<>(stats.buildings));
            // playerMagicPoints.put(nick, stats.magicPoints); // (Se serve per la GUI)
        });

        // Pialla e Ricostruisci la Board in un colpo solo
        characterUpperRow.clear(); characterUpperRow.addAll(msg.getCharUpper());
        characterLowerRow.clear(); characterLowerRow.addAll(msg.getCharLower());
        eventUpperRow.clear();     eventUpperRow.addAll(msg.getEventUpper());
        eventLowerRow.clear();     eventLowerRow.addAll(msg.getEventLower());
        upperBuildingRow.clear();  upperBuildingRow.addAll(msg.getBuildUpper());
        lowerBuildingRow.clear();  lowerBuildingRow.addAll(msg.getBuildLower());
        notifyUI();
    }

    public synchronized void updateState(StateChangedUpdate msg) {
        // 1. Aggiorna lo stato testuale
        this.currentState = msg.getStateName();
        this.currentEra = msg.getCurrentEra();
        this.currentRound = msg.getCurrentRound();

        // 2. Sincronizza giocatori, colori e statistiche
        msg.getPlayersStats().forEach((nick, stats) -> {
            if (!players.contains(nick)) players.add(nick);
            playerColors.put(nick, stats.color);
            playerFood.put(nick, stats.food);
            playerAura.put(nick, stats.aura);
            playerTribe.put(nick, new ArrayList<>(stats.tribe));
            playerBuildings.put(nick, new ArrayList<>(stats.buildings));
        });

        // 3. Sincronizza posizioni dei totem
        turnOrderTrack.clear(); turnOrderTrack.addAll(msg.getTurnOrderTrack());
        offeringTrack.clear();  offeringTrack.putAll(msg.getOfferingTrack());

        // 4. Sincronizza plancia di gioco
        characterUpperRow.clear(); characterUpperRow.addAll(msg.getCharUpper());
        characterLowerRow.clear(); characterLowerRow.addAll(msg.getCharLower());
        eventUpperRow.clear();     eventUpperRow.addAll(msg.getEventUpper());
        eventLowerRow.clear();     eventLowerRow.addAll(msg.getEventLower());
        upperBuildingRow.clear();  upperBuildingRow.addAll(msg.getBuildUpper());
        lowerBuildingRow.clear();  lowerBuildingRow.addAll(msg.getBuildLower());
        notifyUI();
    }

    /** Aggiornato quando un giocatore sceglie il colore del totem */
    public synchronized void updateTotemColor(String nickname, TotemColors color) {
        playerColors.put(nickname, color);
        notifyUI();
    }

    /** Aggiornato quando la partita finisce */
    public synchronized void updateEndGame(List<String> winners) {
        this.winners = winners;
        notifyUI();
    }

    /** Aggiornato quando si riceve un messaggio di errore */
    public synchronized void updateError(String message) {
        this.lastErrorMessage = message;
        notifyUI();
    }

    public synchronized void clearError() {
        this.lastErrorMessage = null;
    }

    public synchronized void clearSystemMessage() { this.lastSystemMessage = null; }

    /* =====================================================================
     * CARICAMENTO STATO COMPLETO (per riconnessione)
     * ===================================================================== */

    /**
     * Carica lo stato completo del gioco da StateChangedUpdate.
     * Chiamato quando un giocatore si riconnette.
     * DA IMPLEMENTARE: usare StateChangedUpdate per ricostruire lo stato completo.
     *
     * @param update lo StateChangedUpdate ricevuto dal server
     */
    // TODO: implementare loadFullState con StateChangedUpdate
    // public synchronized void loadFullState(StateChangedUpdate update) { ... }
    
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(ClientModel.class.getName());

    // --- GETTER ---
    public synchronized String getLastSystemMessage() { return lastSystemMessage; }
    public synchronized String getCurrentState() { return currentState; }
    public synchronized Map<String, TotemColors> getPlayerColors() { return playerColors; }
    public synchronized List<String> getWinners() { return winners; }
    public synchronized String getLastErrorMessage() { return lastErrorMessage; }
    public synchronized Map<Integer, String> getOfferingTrack() { return offeringTrack; }
    public synchronized List<String> getTurnOrderTrack() { return turnOrderTrack; }
    public synchronized List<String> getCharacterUpperRow() { return characterUpperRow; }
    public synchronized List<String> getCharacterLowerRow() { return characterLowerRow; }
    public synchronized List<String> getEventUpperRow() { return eventUpperRow; }
    public synchronized List<String> getEventLowerRow() { return eventLowerRow; }
    public synchronized List<String> getUpperBuildingRow() { return upperBuildingRow; }
    public synchronized List<String> getLowerBuildingRow() { return lowerBuildingRow; }
    public synchronized String getCurrentPlayer() { return currentPlayer; }
    public synchronized List<String> getPlayers() { return players; }
    public synchronized int getCurrentEra() { return currentEra; }
    public synchronized Map<String, Integer> getPlayerFood() { return playerFood; }
    public synchronized Map<String, Integer> getPlayerAura() { return playerAura; }
    public synchronized Map<String, List<String>> getPlayerTribe() { return playerTribe; }
    public synchronized Map<String, List<String>> getPlayerBuildings() { return playerBuildings; }
    public synchronized int getRoundCount() { return currentRound; }
}
