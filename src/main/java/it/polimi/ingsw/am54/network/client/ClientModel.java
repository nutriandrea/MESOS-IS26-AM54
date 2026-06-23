package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
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

    private final java.util.List<EventReport> pendingEventReports = new java.util.ArrayList<>();
    private java.util.function.Consumer<java.util.List<EventReport>> onEventResolvedCallback;

    private boolean isEndGamePending = false;
    private List<String> pendingWinners = null;
    private Map<String, Integer> pendingLastMatchScores = null;
    private int pendingLastMatchNumPlayers = 0;

    public void setOnEventResolvedCallback(java.util.function.Consumer<java.util.List<EventReport>> callback) {
        this.onEventResolvedCallback = callback;
    }

    public synchronized void clearPendingEvents() {
        this.pendingEventReports.clear();

        if (isEndGamePending) {
            this.winners = pendingWinners;
            this.lastMatchScores = pendingLastMatchScores;
            this.lastMatchNumPlayers = pendingLastMatchNumPlayers;
            this.isEndGamePending = false;
            notifyUI();
        }
    }

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

    private final Map<String, Integer> playerMagic = new HashMap<>();

    /** Mappa nickname -> lista delle carte tribu' (personaggi) possedute */
    private final Map<String, List<String>> playerTribe = new HashMap<>();

    /** Mappa nickname -> lista delle carte edificio possedute */
    private final Map<String, List<String>> playerBuildings = new HashMap<>();

    // --- 4. EXTRA ---
    /** Lista dei vincitori (popolata alla fine della partita) */
    private List<String> winners = new ArrayList<>();

    /** Mappa nickname -> punteggio finale dell'ultima partita */
    private Map<String, Integer> lastMatchScores = Map.of();

    /** Numero di giocatori dell'ultima partita */
    private int lastMatchNumPlayers = 0;

    //--- 5. CLASSIFICA (LEADERBOARD) ---
    /** Posizione del giocatore nella classifica per il numero di giocatori specificato */
    private int leaderboardPosition = -1;

    /** Lista completa delle entry della classifica */
    private List<LeaderboardEntry> leaderboardEntries = List.of();

    /** Numero di giocatori per cui e' stata richiesta la classifica */
    private int leaderboardNumPlayers = 0;

    /** Ultimo messaggio di errore ricevuto */
    private String lastErrorMessage;

    private boolean isReconnecting = false;
    private Runnable onReconnectingChangedCallback;
    private Runnable onConnectionLostCallback;

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
     * Aggiorna il modello all'ingresso di un giocatore: lo aggiunge alla lista e
     * all'ordine di turno, lo toglie dai disconnessi (utile in caso di riconnessione) e
     * imposta un messaggio di sistema di benvenuto.
     *
     * @param nickname il giocatore entrato
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
     * Marca un giocatore come disconnesso (resta visibile con il suo stato) e imposta un
     * messaggio di sistema di avviso.
     *
     * @param nickname il giocatore disconnesso
     */
    public synchronized void updatePlayerDisconnected(String nickname) {
        disconnectedPlayers.add(nickname);
        // AGGIUNTA: Messaggio di sistema per le cadute
        this.lastSystemMessage = "⚠️ ATTENZIONE: " + nickname.toUpperCase() + " si è disconnesso!";
        notifyUI();
    }

    /**
     * Riporta un giocatore allo stato "online" togliendolo dai disconnessi e imposta un
     * messaggio di sistema di rientro.
     *
     * @param nickname il giocatore riconnesso
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


    /**
     * Aggiorna il giocatore attualmente di turno.
     *
     * @param nickname il nuovo giocatore di turno (o {@code null} tra una fase e l'altra)
     */
    public synchronized void updateTurn(String nickname) {
        this.currentPlayer = nickname;
        notifyUI();
    }

    /**
     * Riflette il piazzamento di un totem: libera la posizione del giocatore nell'ordine
     * di turno (segnandola "VUOTO" per non far scalare gli altri) e lo inserisce sulla
     * tessera offerta indicata.
     *
     * @param nickname il giocatore che ha piazzato il totem
     * @param position l'indice della tessera offerta
     */
    public synchronized void updateTotemPlacement(String nickname, int position) {
        for (int i = 0; i < turnOrderTrack.size(); i++) {
            String s = turnOrderTrack.get(i);
            String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;

            if (name.equalsIgnoreCase(nickname)) {
                if (s.contains(" -> ")) {
                    // Mantiene "[Slot X | Cibo: +Y] -> " e aggiunge "VUOTO"
                    String prefix = s.substring(0, s.lastIndexOf("->") + 2);
                    turnOrderTrack.set(i, prefix + " VUOTO");
                } else {
                    turnOrderTrack.set(i, "VUOTO");
                }
                break;
            }
        }

        String oldTile = offeringTrack.get(position);
        if (oldTile != null) {
            offeringTrack.put(position, oldTile.replace("VUOTO", nickname));
        }
        notifyUI();
    }

    /**
     * Riflette la pesca di una carta: rimuove la carta dalla fila corretta (personaggio o
     * edificio in base all'indice), la aggiunge alla tribù/edifici del giocatore, aggiorna
     * cibo e magia e sostituisce il tracciato offerte con quello aggiornato dal server.
     *
     * @param nickname        il giocatore che ha pescato
     * @param row             la fila ("upper" o "lower")
     * @param index           l'indice della carta (personaggi seguiti dagli edifici)
     * @param newFoodBalance  il nuovo saldo di cibo
     * @param newMagicBalance il nuovo saldo di magia
     * @param updatedOffering il tracciato offerte aggiornato
     */
    public synchronized void updateCardPicked(String nickname, String row, int index, int newFoodBalance, int newMagicBalance, java.util.Map<Integer, String> updatedOffering) {
        boolean isUpper = row.equalsIgnoreCase("upper");
        if (isUpper) {
            if (index < characterUpperRow.size()) {
                String temp = characterUpperRow.get(index);
                characterUpperRow.remove(index);
                playerTribe.computeIfAbsent(nickname, k -> new java.util.ArrayList<>()).add(temp);
            } else {
                int buildingIndex = index - characterUpperRow.size();
                if (buildingIndex < upperBuildingRow.size()) {
                    String temp = upperBuildingRow.get(buildingIndex);
                    upperBuildingRow.remove(buildingIndex);
                    playerBuildings.computeIfAbsent(nickname, k -> new java.util.ArrayList<>()).add(temp);
                }
            }
        } else {
            if (index < characterLowerRow.size()) {
                String temp = characterLowerRow.get(index);
                characterLowerRow.remove(index);
                playerTribe.computeIfAbsent(nickname, k -> new java.util.ArrayList<>()).add(temp);
            } else {
                int buildingIndex = index - characterLowerRow.size();
                if (buildingIndex < lowerBuildingRow.size()) {
                    String temp = lowerBuildingRow.get(buildingIndex);
                    lowerBuildingRow.remove(buildingIndex);
                    playerBuildings.computeIfAbsent(nickname, k -> new java.util.ArrayList<>()).add(temp);
                }
            }
        }

        playerFood.put(nickname, newFoodBalance);
        playerMagic.put(nickname, newMagicBalance); // Aggiornamento istantaneo del contatore Magia!

        this.offeringTrack.clear();
        this.offeringTrack.putAll(updatedOffering);
        notifyUI();
    }

    /**
     * Riflette il rientro del totem di un giocatore dalla tessera offerta all'ordine di
     * turno: libera la sua tessera (segnandola "VUOTO" senza perdere bonus/ID), lo
     * reinserisce nel primo slot realmente disponibile (saltando i "fantasmi") e aggiorna
     * cibo e aura.
     *
     * @param nickname       il giocatore il cui totem rientra
     * @param newFoodBalance il nuovo saldo di cibo
     * @param newAuraBalance il nuovo saldo di aura
     */
    public synchronized void updateTotemReturned(String nickname, int newFoodBalance, int newAuraBalance) {
        // 1. FIX: Invece di cancellare fisicamente la tessera dalla mappa (facendola sparire nella CLI),
        // la cerchiamo e sostituiamo il nome dell'occupante con "VUOTO", mantenendo intatti i bonus e l'ID!
        int targetKey = -1;
        String targetPrefix = "";

        for (Map.Entry<Integer, String> entry : offeringTrack.entrySet()) {
            String occupier = entry.getValue();
            String name = occupier.contains(" -> ") ? occupier.split(" -> ")[occupier.split(" -> ").length - 1].trim() : occupier;
            if (name.equalsIgnoreCase(nickname)) {
                targetKey = entry.getKey();
                targetPrefix = occupier.contains(" -> ") ? occupier.substring(0, occupier.lastIndexOf("->") + 2) : "";
                break; // Trovato il totem, fermiamo il ciclo
            }
        }

        // Se l'abbiamo trovato, aggiorniamo la mappa in modo sicuro
        if (targetKey != -1) {
            if (!targetPrefix.isEmpty()) {
                offeringTrack.put(targetKey, targetPrefix + " VUOTO");
            } else {
                offeringTrack.put(targetKey, "VUOTO");
            }
        }

        // 2. FIX GRAFICO/LOGICO: Rimuoviamo preventivamente il giocatore dalla sua vecchia
        // posizione "fantasma" nell'ordine di turno per forzare il ricalcolo.
        for (int i = 0; i < turnOrderTrack.size(); i++) {
            String s = turnOrderTrack.get(i);
            String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;
            if (name.equalsIgnoreCase(nickname)) {
                if (s.contains(" -> ")) {
                    String prefix = s.substring(0, s.lastIndexOf("->") + 2);
                    turnOrderTrack.set(i, prefix + " VUOTO");
                } else {
                    turnOrderTrack.set(i, "VUOTO");
                }
            }
        }

        // 3. Inseriamo il giocatore nel PRIMO slot veramente disponibile partendo dall'alto.
        // Uno slot è disponibile se è VUOTO, NULL, oppure se è occupato da un "Fantasma".
        boolean placed = false;
        for (int i = 0; i < turnOrderTrack.size(); i++) {
            String s = turnOrderTrack.get(i);
            String name = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;

            // Determiniamo se l'occupante attuale dello slot è in realtà un fantasma
            boolean isGhost = disconnectedPlayers.contains(name);
            if (!isGhost && !name.equalsIgnoreCase("VUOTO") && !name.equalsIgnoreCase("NULL")) {
                for (String occupier : offeringTrack.values()) {
                    String offName = occupier.contains(" -> ") ? occupier.split(" -> ")[occupier.split(" -> ").length - 1].trim() : occupier;
                    if (offName.equalsIgnoreCase(name)) {
                        isGhost = true;
                        break;
                    }
                }
            }

            // Se lo slot è libero o fantasma, lo sovrascriviamo con il giocatore attivo!
            if (name.equalsIgnoreCase("VUOTO") || name.equalsIgnoreCase("NULL") || isGhost) {
                if (s.contains(" -> ")) {
                    String prefix = s.substring(0, s.lastIndexOf("->") + 2);
                    turnOrderTrack.set(i, prefix + " " + nickname);
                } else {
                    turnOrderTrack.set(i, nickname);
                }
                placed = true;
                break;
            }
        }

        // Fallback di sicurezza
        if (!placed) {
            turnOrderTrack.add(nickname);
        }

        playerFood.put(nickname, newFoodBalance);
        playerAura.put(nickname, newAuraBalance);
        notifyUI();
    }

    /**
     * Applica lo snapshot di un evento risolto: per ogni giocatore calcola il delta di
     * risorse (per il riepilogo a schermo) e ne aggiorna cibo, aura, magia, tribù ed
     * edifici, quindi ricostruisce le file del tabellone e notifica la UI.
     *
     * @param msg l'aggiornamento dell'evento ricevuto dal server
     */
    public synchronized void updateEventSnapshot(EventUpdate msg) {
        EventReport report = new EventReport();
        report.eventName = msg.getEventName() != null ? msg.getEventName() : "Risoluzione Eventi";

        // Aggiorna Risorse di TUTTI i Giocatori simultaneamente (LA TUA LOGICA INTATTA)
        msg.getPlayersStats().forEach((nick, stats) -> {

            // <-- CATTURA DATI PRIMA DELLA SOVRASCRITTURA -->
            PlayerDelta pd = new PlayerDelta();
            pd.nickname = nick;
            pd.oldFood = playerFood.getOrDefault(nick, 0);
            pd.oldAura = playerAura.getOrDefault(nick, 0);
            pd.oldMagic = playerMagic.getOrDefault(nick, 0);
            pd.diffFood = stats.food - pd.oldFood;
            pd.diffAura = stats.aura - pd.oldAura;
            pd.diffMagic = stats.magicPoints - pd.oldMagic;
            report.playerDeltas.add(pd);
            // <--------------------------------------------->

            playerFood.put(nick, stats.food);
            playerAura.put(nick, stats.aura);
            playerMagic.put(nick, stats.magicPoints);
            playerTribe.put(nick, new java.util.ArrayList<>(stats.tribe));
            playerBuildings.put(nick, new java.util.ArrayList<>(stats.buildings));
        });

        // Memorizza l'evento e notifica la GUI
        pendingEventReports.add(report);
        if (onEventResolvedCallback != null) {
            onEventResolvedCallback.accept(new java.util.ArrayList<>(pendingEventReports));
        }

        // Pialla e Ricostruisci la Board in un colpo solo (LA TUA LOGICA INTATTA)
        characterUpperRow.clear(); characterUpperRow.addAll(msg.getCharUpper());
        characterLowerRow.clear(); characterLowerRow.addAll(msg.getCharLower());
        eventUpperRow.clear();     eventUpperRow.addAll(msg.getEventUpper());
        eventLowerRow.clear();     eventLowerRow.addAll(msg.getEventLower());
        upperBuildingRow.clear();  upperBuildingRow.addAll(msg.getBuildUpper());
        lowerBuildingRow.clear();  lowerBuildingRow.addAll(msg.getBuildLower());
        notifyUI();
    }

    /**
     * Applica lo snapshot COMPLETO dello stato ricevuto a ogni cambio di fase o alla
     * riconnessione: azzera tutte le strutture locali (per non lasciare "fantasmi" di
     * giocatori usciti) e le ripopola con stato, statistiche dei giocatori, ordine di
     * turno, tracciato offerte e file del tabellone.
     *
     * @param msg lo snapshot di stato ricevuto dal server
     */
    public synchronized void updateState(StateChangedUpdate msg) {
        this.currentState = msg.getStateName();
        this.currentEra = msg.getCurrentEra();
        this.currentRound = msg.getCurrentRound();

        // 1. PULIZIA TOTALE: Dobbiamo svuotare TUTTO prima di riempire.
        // Se non svuotiamo queste mappe, i dati dei giocatori disconnessi rimangono "fantasmi".
        this.players.clear();
        this.playerColors.clear();
        this.playerFood.clear();
        this.playerAura.clear();
        this.playerMagic.clear();
        this.playerTribe.clear();
        this.playerBuildings.clear();

        // 2. Ricarica i dati puliti inviati dal server
        this.players.addAll(msg.getPlayersStats().keySet());

        msg.getPlayersStats().forEach((nick, stats) -> {
            playerColors.put(nick, stats.color);
            playerFood.put(nick, stats.food);
            playerAura.put(nick, stats.aura);
            playerMagic.put(nick, stats.magicPoints);
            playerTribe.put(nick, new ArrayList<>(stats.tribe));
            playerBuildings.put(nick, new ArrayList<>(stats.buildings));
        });

        // 3. Sincronizzazione strutture di gioco rimanenti
        turnOrderTrack.clear();
        turnOrderTrack.addAll(msg.getTurnOrderTrack());

        offeringTrack.clear();
        offeringTrack.putAll(msg.getOfferingTrack());

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

    /**
     * Registra il risultato finale (vincitori e punteggi). I dati vengono assegnati subito
     * così non sono mai vuoti; se ci sono ancora report eventi da mostrare, l'apertura
     * della schermata di fine partita viene posticipata fino al loro consumo.
     *
     * @param winners    i vincitori
     * @param allScores  i punteggi finali di tutti i giocatori
     * @param numPlayers il numero di giocatori della partita
     */
    public synchronized void updateEndGame(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
        // FIX ASSOLUTO: Assegnazione immediata e incondizionata dei risultati.
        // Così model.getWinners() non sarà MAI vuoto quando la CLI andrà a stamparlo.
        this.winners = winners;
        this.lastMatchScores = allScores;
        this.lastMatchNumPlayers = numPlayers;

        if (!pendingEventReports.isEmpty() && onEventResolvedCallback != null) {
            this.isEndGamePending = true;
            this.pendingWinners = winners;
            this.pendingLastMatchScores = allScores;
            this.pendingLastMatchNumPlayers = numPlayers;
        } else {
            notifyUI();
        }
    }

    // ==========================================
    // CALCOLO BONUS STATICI (DA GAME.JAVA)
    // ==========================================

    /**
     * Ricalcola lato client il bonus Artisti di un giocatore (10 aura per coppia),
     * leggendo le carte serializzate della sua tribù. Usato per mostrare il dettaglio
     * del punteggio finale.
     *
     * @param pName il nickname del giocatore
     * @return l'aura bonus dagli artisti
     */
    public int getArtistBonus(String pName) {
        int artistCount = 0;
        List<String> tribe = this.playerTribe.getOrDefault(pName, new ArrayList<>());
        for (String card : tribe) {
            String[] parts = card.split("\\|");
            if (parts.length > 3 && parts[3].equalsIgnoreCase("ARTIST")) {
                artistCount++;
            }
        }
        return (artistCount / 2) * 10;
    }

    /**
     * Ricalcola lato client il bonus Inventori (numero di inventori per invenzioni
     * distinte), leggendo le carte serializzate della tribù.
     *
     * @param pName il nickname del giocatore
     * @return l'aura bonus dagli inventori
     */
    public int getInventorBonus(String pName) {
        int inventorCount = 0;
        java.util.Set<Integer> uniqueInventions = new java.util.HashSet<>();
        List<String> tribe = this.playerTribe.getOrDefault(pName, new ArrayList<>());
        for (String card : tribe) {
            String[] parts = card.split("\\|");
            if (parts.length > 4 && parts[3].equalsIgnoreCase("INVENTOR")) {
                inventorCount++;
                try { uniqueInventions.add(Integer.parseInt(parts[4])); } catch (Exception ignored) {}
            }
        }
        return inventorCount * uniqueInventions.size();
    }

    /**
     * Ricalcola lato client l'aura dei Costruttori (somma delle loro aura base),
     * leggendo le carte serializzate della tribù.
     *
     * @param pName il nickname del giocatore
     * @return l'aura bonus dai costruttori
     */
    public int getConstructorBonus(String pName) {
        int constructorAura = 0;
        List<String> tribe = this.playerTribe.getOrDefault(pName, new ArrayList<>());
        for (String card : tribe) {
            String[] parts = card.split("\\|");
            if (parts.length > 5 && parts[3].equalsIgnoreCase("CONSTRUCTOR")) {
                try { constructorAura += Integer.parseInt(parts[5]); } catch (Exception ignored) {}
            }
        }
        return constructorAura;
    }

    /**
     * Ricalcola lato client l'aura base degli Edifici (somma delle aura finali),
     * leggendo le carte serializzate del giocatore.
     *
     * @param pName il nickname del giocatore
     * @return l'aura base dagli edifici
     */
    public int getBuildingBaseAura(String pName) {
        int buildingBaseAura = 0;
        List<String> buildings = this.playerBuildings.getOrDefault(pName, new ArrayList<>());
        for (String bld : buildings) {
            String[] parts = bld.split("\\|");
            if (parts.length > 5) {
                try { buildingBaseAura += Integer.parseInt(parts[5]); } catch (Exception ignored) {}
            }
        }
        return buildingBaseAura;
    }

    /**
     * Memorizza la classifica ricevuta dal server (posizione del giocatore ed elenco
     * completo) per la sua visualizzazione.
     *
     * @param playerPosition la posizione del giocatore in classifica
     * @param entries        le voci della classifica
     * @param numPlayers     il filtro sul numero di giocatori usato
     */
    public synchronized void updateLeaderboard(int playerPosition, List<LeaderboardEntry> entries, int numPlayers) {
        this.leaderboardPosition = playerPosition;
        this.leaderboardEntries = entries;
        this.leaderboardNumPlayers = numPlayers;
        notifyUI();
    }

    /** Aggiornato quando si riceve un messaggio di errore */
    public synchronized void updateError(String message) {
        this.lastErrorMessage = message;
        notifyUI();
    }

    /** Aggiornato quando il gioco viene sospeso (1 solo giocatore attivo) */
    public synchronized void updateGameSuspended() {
        this.lastSystemMessage = "⏸️ GIOCO SOSPESO: in attesa di riconnessioni...";
        notifyUI();
    }

    /** Aggiornato quando il gioco viene ripreso */
    public synchronized void updateGameResumed() {
        this.lastSystemMessage = "▶️ GIOCO RIPRESO: almeno 2 giocatori attivi!";
        notifyUI();
    }

    public synchronized void clearError() {
        this.lastErrorMessage = null;
    }

    public synchronized void clearSystemMessage() { this.lastSystemMessage = null; }

    public boolean isReconnecting() { return isReconnecting; }

    public void setReconnectingStatus(boolean status) {
        this.isReconnecting = status;
        if (onReconnectingChangedCallback != null) {
            onReconnectingChangedCallback.run();
        }
    }

    public void setOnReconnectingChangedCallback(Runnable callback) {
        this.onReconnectingChangedCallback = callback;
    }

    public void setOnConnectionLostCallback(Runnable callback) {
        this.onConnectionLostCallback = callback;
    }

    /**
     * Segnala la perdita di connessione col server: imposta un messaggio di sistema e
     * invoca l'eventuale callback (es. per avviare l'auto-riconnessione lato UI).
     */
    public void triggerConnectionLost() {
        // AGGIUNTA: Forza un messaggio di sistema per la CLI (e GUI)
        this.lastSystemMessage = "❌ ATTENZIONE: CONNESSIONE CON IL SERVER PERSA! Tentativo di riconnessione in corso...";
        notifyUI();

        if (onConnectionLostCallback != null) {
            onConnectionLostCallback.run();
        }
    }

    public synchronized void triggerSystemMessage(String msg) {
        this.lastSystemMessage = msg;
        notifyUI();
    }

    // --- GETTER ---
    public synchronized String getLastSystemMessage() { return lastSystemMessage; }
    public synchronized String getCurrentState() { return currentState; }
    public synchronized Map<String, TotemColors> getPlayerColors() { return playerColors; }
    public synchronized List<String> getWinners() { return winners; }
    public synchronized Map<String, Integer> getLastMatchScores() { return lastMatchScores; }
    public synchronized int getLastMatchNumPlayers() { return lastMatchNumPlayers; }
    public synchronized int getLeaderboardPosition() { return leaderboardPosition; }
    public synchronized List<LeaderboardEntry> getLeaderboardEntries() { return leaderboardEntries; }
    public synchronized int getLeaderboardNumPlayers() { return leaderboardNumPlayers; }
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
    public synchronized Map<String, Integer> getPlayerMagic() { return playerMagic; }
    public synchronized Map<String, List<String>> getPlayerTribe() { return playerTribe; }
    public synchronized Map<String, List<String>> getPlayerBuildings() { return playerBuildings; }
    public synchronized int getRoundCount() { return currentRound; }
}
