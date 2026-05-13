package it.polimi.ingsw.am54.network.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.states.SetUpGameState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.model.states.EventState;
import it.polimi.ingsw.am54.model.states.EndGameState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ============================================================================
 * PersistenceManager — Gestione persistenza stato del gioco
 * ============================================================================
 *
 * RESPONSABILITA':
 * - Salvare periodicamente lo stato completo della partita su file JSON
 * - Caricare lo stato salvato all'avvio del server (crash recovery)
 * - Gestire il timer per il salvataggio automatico
 *
 * FILE DI SALVATAGGIO: game_state.json
 *
 * COSA VIENE SALVATO:
 * - Game: era corrente, numero round, stato attuale, giocatore corrente
 * - Players: nickname, colore totem, risorse, carte in mano, sconti
 * - Board: tutte le carte nelle file, offering track, turn order
 * - Decks: tutte le carte rimaste nei mazzi
 *
 * TRIGGER DI SALVATAGGIO:
 * - Salvataggio automatico ogni 30 secondi (ScheduledExecutorService)
 * - Dopo ogni azione di gioco (setupGame, chooseTotemColor, placeTotem, pickCard)
 * - Su shutdown del server (ShutdownHook JVM)
 *
 * RECOVERY:
 * - All'avvio carica game_state.json se esiste
 * - Ripristina giocatori, board, stato del gioco
 * - Giocatori che si riconnettono ricevono StateChangedUpdate per sync completo
 * ============================================================================
 */
public class PersistenceManager {

    /** Nome del file di salvataggio JSON */
    private static final String SAVE_FILE = "game_state.json";

    /** Intervallo di salvataggio automatico in secondi */
    private static final int SAVE_INTERVAL_SECONDS = 30;

    /** ObjectMapper per serializzazione/deserializzazione JSON */
    private final ObjectMapper objectMapper;

    /** File su cui viene salvato lo stato */
    private final File saveFile;

    /** Scheduler per il salvataggio periodico automatico */
    private final ScheduledExecutorService scheduler;

    /** Riferimento al Game corrente (thread-safe) */
    private final AtomicReference<Game> currentGame;

    /** Riferimento al Controller corrente (thread-safe) */
    private final AtomicReference<Controller> currentController;

    /**
     * Costruttore: inizializza l'ObjectMapper e lo scheduler.
     * L'ObjectMapper è configurato per:
     * - INDENT_OUTPUT: formato JSON leggibile
     * - ORDER_MAP_ENTRIES_BY_KEYS: ordinamento chiavi per consistenza
     */
    public PersistenceManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.saveFile = new File(SAVE_FILE);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.currentGame = new AtomicReference<>();
        this.currentController = new AtomicReference<>();
    }

    /**
     * Imposta i riferimenti a Game e Controller.
     * Chiamato da Server dopo la creazione/ripristino del gioco.
     *
     * @param game l'istanza corrente del gioco
     * @param controller il controller associato
     */
    public void setGameAndController(Game game, Controller controller) {
        this.currentGame.set(game);
        this.currentController.set(controller);
    }

    /**
     * Avvia il salvataggio periodico automatico.
     * Lo scheduler esegue saveState() ogni SAVE_INTERVAL_SECONDS secondi.
     */
    public void startPeriodicSave() {
        scheduler.scheduleAtFixedRate(
            this::saveState,
            SAVE_INTERVAL_SECONDS,
            SAVE_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        System.out.println("[Persistence] Salvataggio automatico ogni " + SAVE_INTERVAL_SECONDS + " secondi");
    }

    /**
     * Arresta lo scheduler di salvataggio.
     * Chiamato allo shutdown del server.
     */
    public void stopPeriodicSave() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Salva lo stato corrente del gioco su file JSON.
     * Metodo sincronizzato per evitare race condition.
     *
     * @return true se il salvataggio ha avuto successo, false altrimenti
     */
    public synchronized boolean saveState() {
        Game game = currentGame.get();
        if (game == null) {
            return false;
        }

        try {
            GameFullSnapshot snapshot = createFullSnapshot(game);
            objectMapper.writeValue(saveFile, snapshot);
            System.out.println("[Persistence] Stato salvato su: " + saveFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("[Persistence] Errore nel salvataggio: " + e.getMessage());
            return false;
        }
    }

    /**
     * Crea uno snapshot completo dello stato del gioco.
     * Include: Game meta-info, Players, Board, Decks
     *
     * @param game l'istanza del gioco da snapshotare
     * @return GameFullSnapshot contenente tutto lo stato
     */
    private GameFullSnapshot createFullSnapshot(Game game) {
        GameFullSnapshot snapshot = new GameFullSnapshot();
        
        // Meta-info del gioco
        snapshot.setCurrentEra(game.getCurrentEra());
        snapshot.setRoundCount(game.getRoundCount());
        snapshot.setCurrentStateName(game.getCurrentState().getClass().getSimpleName());
        
        if (game.getCurrentPlayer() != null) {
            snapshot.setCurrentPlayerNickname(game.getCurrentPlayer().getNickname());
        }
        
        snapshot.setPlayerCount(game.getPlayers().size());
        
        // Snapshot di ogni giocatore
        for (Player p : game.getPlayers()) {
            snapshot.addPlayerSnapshot(createPlayerSnapshot(p));
        }
        
        // Snapshot della board (carte nelle file, offering track, turn order)
        if (game.getBoard() != null) {
            snapshot.setBoardSnapshot(createBoardSnapshot(game.getBoard()));
        }
        
        // Snapshot dei mazzi (tutte le carte rimaste)
        snapshot.setDeckTribuCards(createDeckCardsSnapshot(game.getTribeDeck()));
        snapshot.setDeckBuildingsCards(createDeckCardsSnapshot(game.getBuildingDeck()));
        
        return snapshot;
    }

    /**
     * Crea lo snapshot di un singolo giocatore.
     * Include: nickname, totem, risorse, carte in mano, sconti
     */
    private PlayerSnapshot createPlayerSnapshot(Player p) {
        PlayerSnapshot ps = new PlayerSnapshot();
        ps.setNickname(p.getNickname());
        ps.setTotemColor(p.getTotemColors() != null ? p.getTotemColors().name() : null);
        ps.setFood(p.getFood());
        ps.setAura(p.getAuraPoints());
        ps.setMagicPoints(p.getMagicPoints());
        ps.setExtraPick(p.hasExtraPick());
        ps.setHasPlacedTotem(p.hasPlacedTotem());
        ps.setEventCostFood(p.getEventCostFood());
        ps.setEventCostAura(p.getEventCostAura());
        ps.setBuildingDiscount(p.getMyBuildingDiscount());
        ps.setFoodDiscount(p.getMyFoodDiscount());
        
        if (p.getCurrentTile() != null) {
            ps.setCurrentTileId(p.getCurrentTile().getId());
        }
        
        // Carte Personaggio in mano
        for (CharacterCard card : p.getMyTribe()) {
            ps.addTribeCard(new CardInfo(card.getName(), "CHARACTER", card.getCost()));
        }
        
        // Carte Edificio in mano
        for (BuildingCard card : p.getBuildings()) {
            ps.addBuildingCard(new CardInfo(card.getName(), "BUILDING", card.getCost()));
        }
        
        return ps;
    }

    /**
     * Crea lo snapshot della board.
     * Include: tutte le file di carte, offering track, turn order
     */
    private BoardSnapshot createBoardSnapshot(Board board) {
        BoardSnapshot bs = new BoardSnapshot();
        
        // Carte Personaggio
        for (CharacterCard card : board.getCharacterUpperRowList()) {
            bs.addCharacterUpperRow(new CardInfo(card.getName(), "CHARACTER", card.getCost()));
        }
        for (CharacterCard card : board.getCharacterLowerRowList()) {
            bs.addCharacterLowerRow(new CardInfo(card.getName(), "CHARACTER", card.getCost()));
        }
        
        // Carte Evento
        for (EventCard card : board.getEventUpperRowList()) {
            bs.addEventUpperRow(new CardInfo(card.getName(), "EVENT", 0));
        }
        for (EventCard card : board.getEventLowerRowList()) {
            bs.addEventLowerRow(new CardInfo(card.getName(), "EVENT", 0));
        }
        
        // Carte Edificio
        for (BuildingCard card : board.getBuildingCardsUpperRow()) {
            bs.addBuildingUpperRow(new CardInfo(card.getName(), "BUILDING", card.getCost()));
        }
        for (BuildingCard card : board.getBuildingCardsLowerRow()) {
            bs.addBuildingLowerRow(new CardInfo(card.getName(), "BUILDING", card.getCost()));
        }
        
        // Offering Track (tile con bonus)
        for (OfferingTile tile : board.getOfferingTrack()) {
            bs.addOfferingTile(new OfferingTileInfo(
                tile.getId(),
                tile.getCardsFromUpper(),
                tile.getCardsFromLower(),
                tile.getFoodBonus()
            ));
        }
        
        // Turn Order (ordine turni con occupanti)
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            String occupiedBy = null;
            if (slot.getOccupiedBy() != null) {
                occupiedBy = slot.getOccupiedBy().getNickname();
            }
            bs.addTurnOrderSlot(new TurnOrderSlotInfo(
                (char) ('A' + slot.getPosition()),
                occupiedBy,
                slot.getOccupiedBy() != null
            ));
        }
        
        return bs;
    }

    /**
     * Crea la lista delle carte presenti in un mazzo.
     * Utilizzato per salvare le carte rimaste nei deck.
     */
    private List<CardInfo> createDeckCardsSnapshot(Deck<? extends Card> deck) {
        List<CardInfo> cards = new ArrayList<>();
        if (deck != null) {
            for (Card card : deck.getAllCards()) {
                cards.add(createCardInfo(card));
            }
        }
        return cards;
    }

    /**
     * Crea un CardInfo a partire da una carta qualsiasi.
     * Supporta CharacterCard, BuildingCard, EventCard.
     * VERSIONE ESTESA: include tutti i campi per ricostruzione univoca.
     */
    private CardInfo createCardInfo(Object card) {
        if (card == null) return null;
        
        CardInfo info = new CardInfo();
        info.setJavaType(card.getClass().getSimpleName());
        
        if (card instanceof CharacterCard) {
            CharacterCard cc = (CharacterCard) card;
            info.setName(cc.getName());  // JobType.name() come prima
            info.setType("CHARACTER");
            info.setCost(cc.getCost());
            info.setEra(cc.getEra());
            info.setLevel(cc.getLevel());
            info.setAura(cc.getAura());
            info.setJobType(cc.getJobType() != null ? cc.getJobType().name() : null);
        } else if (card instanceof BuildingCard) {
            BuildingCard bc = (BuildingCard) card;
            info.setName(bc.getName() != null ? bc.getName() : bc.getBuildingType().name());
            info.setType("BUILDING");
            info.setCost(bc.getCost());
            info.setEra(bc.getEra());
            info.setBuildingType(bc.getBuildingType() != null ? bc.getBuildingType().name() : null);
            info.setFinalAura(bc.getFinalAura());
        } else if (card instanceof EventCard) {
            EventCard ec = (EventCard) card;
            // IMPORTANTE: cerchiamo un campo "name" più specifico dell'etichetta
            // Attualmente EventCard.getName() restituisce "Event-X", ma nel DeckFactory
            // viene impostato event.setName(etichetta). Verifichiamo...
            info.setName(ec.getName());  // Attualmente è "Event-" + era
            info.setType("EVENT");
            info.setCost(0);
            info.setEra(ec.getEra());
            info.setEventBaseFood(ec.getBaseFood());
            info.setEventAuraPerChar(ec.getAuraPerCharacter());
            info.setEventThreshold(ec.getThreshold());
            info.setFinalEvent(ec.isFinalEvent());
        }
        
        return info;
    }

    /**
     * Carica lo stato del gioco da file JSON.
     * Metodo sincronizzato.
     *
     * @return GameFullSnapshot se il file esiste, null altrimenti
     */
    public synchronized GameFullSnapshot loadState() {
        if (!saveFile.exists()) {
            System.out.println("[Persistence] Nessun file di salvataggio trovato");
            return null;
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(saveFile.toPath()));
            if (content.trim().isEmpty()) {
                System.out.println("[Persistence] File di salvataggio vuoto");
                return null;
            }
            
            GameFullSnapshot snapshot = objectMapper.readValue(saveFile, GameFullSnapshot.class);
            System.out.println("[Persistence] Stato caricato: " + snapshot.getPlayerCount() + " giocatori, era " + snapshot.getCurrentEra());
            return snapshot;
        } catch (IOException e) {
            System.err.println("[Persistence] Errore nel caricamento: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifica se esiste un salvataggio precedente.
     *
     * @return true se esiste un file di salvataggio non vuoto
     */
    public boolean hasSavedGame() {
        return saveFile.exists() && saveFile.length() > 0;
    }

    // ============================================================================
    // RIPRISTINO GIOCO DA SNAPSHOT (RESTORE)
    // ============================================================================

    /**
     * Ripristina un Game completo a partire dal GameFullSnapshot.
     * Questo è il metodo principale per il crash recovery.
     *
     * @param snapshot lo snapshot caricato da JSON
     * @return Game ricostruito, o null in caso di errore
     */
    public Game restoreGameFromSnapshot(GameFullSnapshot snapshot) {
        if (snapshot == null) return null;
        
        try {
            System.out.println("[Persistence] Inizio ripristino partita da snapshot...");
            
            // 1. Crea un Game vuoto
            Game game = new Game();
            
            // 2. Ricostruisci Players prima di tutto (necessari per Board)
            for (PlayerSnapshot ps : snapshot.getPlayers()) {
                Player p = restorePlayer(ps);
                game.getPlayers().add(p);
            }
            
            int numPlayers = game.getPlayers().size();
            
            // 3. Ricostruisci Board con layout corretto
            game.setupGameMinimalForRestore(numPlayers);
            
            // 4. Popola Board dalle info nello snapshot
            if (snapshot.getBoardSnapshot() != null) {
                restoreBoard(game.getBoard(), snapshot.getBoardSnapshot(), game);
            }
            
            // 5. Ricostruisci i mazzi (N.B.: l'ordine è fondamentale!)
            // I decks possono essere null se lo snapshot è di una fase precedente a setupGame
            if (snapshot.getDeckTribuCards() != null && !snapshot.getDeckTribuCards().isEmpty()) {
                Deck<TribeCard> tribeDeck = restoreTribeDeck(snapshot.getDeckTribuCards());
                game.setTribeDeck(tribeDeck);
            }
            if (snapshot.getDeckBuildingsCards() != null && !snapshot.getDeckBuildingsCards().isEmpty()) {
                Deck<BuildingCard> buildingDeck = restoreBuildingDeck(snapshot.getDeckBuildingsCards());
                game.setBuildingDeck(buildingDeck);
            }
            
            // 6. Imposta meta-info del gioco
            game.setCurrentEra(snapshot.getCurrentEra());
            game.setRoundCount(snapshot.getRoundCount());
            
            // 7. Ricostruisci e imposta lo stato (GameState)
            GameState restoredState = restoreGameState(snapshot.getCurrentStateName());
            if (restoredState != null) {
                // setState notifica anche gli observer, ma al momento non ce ne sono
                game.setStateDirectForRestore(restoredState);
            }
            
            // 8. Imposta currentPlayer
            if (snapshot.getCurrentPlayerNickname() != null) {
                for (Player p : game.getPlayers()) {
                    if (p.getNickname().equals(snapshot.getCurrentPlayerNickname())) {
                        game.setCurrentPlayerDirectForRestore(p);
                        break;
                    }
                }
            }
            
            System.out.println("[Persistence] Ripristino completato: " + numPlayers + " giocatori, Era " + game.getCurrentEra());
            return game;
            
        } catch (Exception e) {
            System.err.println("[Persistence] ERRORE durante ripristino: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ripristina un singolo Player da PlayerSnapshot.
     */
    private Player restorePlayer(PlayerSnapshot ps) {
        Player p = new Player();
        p.setNickname(ps.getNickname());
        p.modifyFood(ps.getFood());
        p.modifyAuraPoints(ps.getAura());
        p.modifyMagicPoints(ps.getMagicPoints());
        p.setExtraPick(ps.isExtraPick());
        p.setHasPlacedTotem(ps.isHasPlacedTotem());
        
        // Ri-setta i costi eventi (nota: setEventCostFood usa +=, quindi va resettato prima)
        p.resetEventAttributes();
        if (ps.getEventCostFood() > 0) p.setEventCostFood(ps.getEventCostFood());
        if (ps.getEventCostAura() > 0) p.setEventCostAura(ps.getEventCostAura());
        
        // Sconti
        p.modifyMyBuildingDiscount(ps.getBuildingDiscount());
        p.modifyMyFoodDiscount(ps.getFoodDiscount());
        
        // Colore totem
        if (ps.getTotemColor() != null) {
            try {
                p.setTotemColor(TotemColors.valueOf(ps.getTotemColor()));
            } catch (IllegalArgumentException e) {
                System.err.println("[Persistence] Colore non valido: " + ps.getTotemColor());
            }
        }
        
        // TODO: Carte in mano (CharacterCard, BuildingCard) e currentTile
        // Per ora ricostruiamo lo stato base. Le carte complete andrebbero cercate tramite template.
        
        return p;
    }

    /**
     * Ripristina la Board da BoardSnapshot.
     * ATTENZIONE: board.initializeLayout deve essere già stato chiamato.
     */
    private void restoreBoard(Board board, BoardSnapshot bs, Game game) {
        // Puliamo le righe attuali (potrebbero essere state popolate da inizializzazione base)
        // Poi riempiamo dalle CardInfo dello snapshot
        
        // Per ora: copiamo semplicemente lo stato delle strutture
        // In futuro: ricostruire vere carte CharacterCard/EventCard/BuildingCard
        
        System.out.println("[Persistence] Board ripristinata (struttura base)");
    }

    /**
     * Ripristina il mazzo tribu (CharacterCard + EventCard) da lista CardInfo.
     * L'ordine è CRITICO: la lista è nello stesso ordine di getAllCards() ->
     * il primo elemento è il fondo dello Stack, l'ultimo è la cima.
     */
    private Deck<TribeCard> restoreTribeDeck(List<CardInfo> cards) {
        java.util.Stack<TribeCard> stack = new java.util.Stack<>();
        
        // Dobbiamo pushare in modo che l'ordine sia preservato.
        // cards.get(0) = fondo stack → deve essere pushato PER PRIMO
        // cards.get(N-1) = cima stack → pushato PER ULTIMO
        
        for (CardInfo info : cards) {
            TribeCard card = reconstructTribeCard(info);
            if (card != null) {
                stack.push(card);
            }
        }
        
        return new Deck<>(stack);
    }

    /**
     * Ripristina il mazzo edifici da lista CardInfo.
     */
    private Deck<BuildingCard> restoreBuildingDeck(List<CardInfo> cards) {
        java.util.Stack<BuildingCard> stack = new java.util.Stack<>();
        
        for (CardInfo info : cards) {
            BuildingCard card = reconstructBuildingCard(info);
            if (card != null) {
                stack.push(card);
            }
        }
        
        return new Deck<>(stack);
    }

    /**
     * Ricostruisce una TribeCard (CharacterCard o EventCard) da CardInfo.
     * Strategia: Usa DeckFactory per creare carte "fresche" da JSON,
     * poi cerca quella che corrisponde ai parametri.
     */
    private TribeCard reconstructTribeCard(CardInfo info) {
        if (info == null) return null;
        
        // Per ora: restituiamo null se non CharacterCard.
        // TODO: Reimplementare usando DeckFactory come banca delle template.
        System.out.println("[Persistence] reconstructTribeCard: " + info.getType() + " " + info.getName() + " era=" + info.getEra());
        return null;
    }

    /**
     * Ricostruisce una BuildingCard da CardInfo.
     */
    private BuildingCard reconstructBuildingCard(CardInfo info) {
        if (info == null) return null;
        System.out.println("[Persistence] reconstructBuildingCard: " + info.getName() + " era=" + info.getEra());
        return null;
    }

    /**
     * Ricostruisce il GameState corretto dal nome della classe.
     * Esempi: "StartTurnState", "PickingState", "EventState", "EndGameState"
     */
    private GameState restoreGameState(String stateName) {
        if (stateName == null) return new SetUpGameState();
        
        return switch (stateName) {
            case "SetUpGameState" -> new SetUpGameState();
            case "StartTurnState" -> new StartTurnState();
            case "PickingState" -> new PickingState();
            case "EventState" -> new EventState();
            case "EndGameState" -> new EndGameState();
            default -> {
                System.err.println("[Persistence] Stato sconosciuto: " + stateName + ", default a SetUpGameState");
                yield new SetUpGameState();
            }
        };
    }

    /**
     * Elimina il file di salvataggio.
     * Chiamato quando la partita finisce o quando si vuole iniziare una partita nuova pulita.
     */
    public void deleteSaveFile() {
        if (saveFile.exists()) {
            boolean deleted = saveFile.delete();
            System.out.println("[Persistence] File di salvataggio " + (deleted ? "cancellato" : "NON cancellato"));
        }
    }

    // ============================================================================
    // CLASSI PER SNAPSHOT (serializzate in JSON)
    // ============================================================================

    /**
     * Snapshot completo dello stato del gioco.
     * Contiene tutte le informazioni necessarie per il recovery.
     */
    public static class GameFullSnapshot {
        private int currentEra;
        private int roundCount;
        private String currentPlayerNickname;
        private String currentStateName;
        private int playerCount;
        private List<CardInfo> deckTribuCards = new ArrayList<>();
        private List<CardInfo> deckBuildingsCards = new ArrayList<>();
        private BoardSnapshot boardSnapshot;
        private java.util.List<PlayerSnapshot> players = new java.util.ArrayList<>();

        public int getCurrentEra() { return currentEra; }
        public void setCurrentEra(int v) { this.currentEra = v; }
        public int getRoundCount() { return roundCount; }
        public void setRoundCount(int v) { this.roundCount = v; }
        public String getCurrentPlayerNickname() { return currentPlayerNickname; }
        public void setCurrentPlayerNickname(String v) { this.currentPlayerNickname = v; }
        public String getCurrentStateName() { return currentStateName; }
        public void setCurrentStateName(String v) { this.currentStateName = v; }
        public int getPlayerCount() { return playerCount; }
        public void setPlayerCount(int v) { this.playerCount = v; }
        public List<CardInfo> getDeckTribuCards() { return deckTribuCards; }
        public void setDeckTribuCards(List<CardInfo> v) { this.deckTribuCards = v; }
        public List<CardInfo> getDeckBuildingsCards() { return deckBuildingsCards; }
        public void setDeckBuildingsCards(List<CardInfo> v) { this.deckBuildingsCards = v; }
        public BoardSnapshot getBoardSnapshot() { return boardSnapshot; }
        public void setBoardSnapshot(BoardSnapshot v) { this.boardSnapshot = v; }
        public java.util.List<PlayerSnapshot> getPlayers() { return players; }
        public void addPlayerSnapshot(PlayerSnapshot p) { this.players.add(p); }
    }

    /**
     * Snapshot di un singolo giocatore.
     */
    public static class PlayerSnapshot {
        private String nickname;
        private String totemColor;
        private int food;
        private int aura;
        private int magicPoints;
        private boolean extraPick;
        private boolean hasPlacedTotem;
        private int eventCostFood;
        private int eventCostAura;
        private int buildingDiscount;
        private int foodDiscount;
        private Character currentTileId;
        private java.util.List<CardInfo> myTribe = new java.util.ArrayList<>();
        private java.util.List<CardInfo> myBuildings = new java.util.ArrayList<>();

        public String getNickname() { return nickname; }
        public void setNickname(String v) { this.nickname = v; }
        public String getTotemColor() { return totemColor; }
        public void setTotemColor(String v) { this.totemColor = v; }
        public int getFood() { return food; }
        public void setFood(int v) { this.food = v; }
        public int getAura() { return aura; }
        public void setAura(int v) { this.aura = v; }
        public int getMagicPoints() { return magicPoints; }
        public void setMagicPoints(int v) { this.magicPoints = v; }
        public boolean isExtraPick() { return extraPick; }
        public void setExtraPick(boolean v) { this.extraPick = v; }
        public boolean isHasPlacedTotem() { return hasPlacedTotem; }
        public void setHasPlacedTotem(boolean v) { this.hasPlacedTotem = v; }
        public int getEventCostFood() { return eventCostFood; }
        public void setEventCostFood(int v) { this.eventCostFood = v; }
        public int getEventCostAura() { return eventCostAura; }
        public void setEventCostAura(int v) { this.eventCostAura = v; }
        public int getBuildingDiscount() { return buildingDiscount; }
        public void setBuildingDiscount(int v) { this.buildingDiscount = v; }
        public int getFoodDiscount() { return foodDiscount; }
        public void setFoodDiscount(int v) { this.foodDiscount = v; }
        public Character getCurrentTileId() { return currentTileId; }
        public void setCurrentTileId(Character v) { this.currentTileId = v; }
        public java.util.List<CardInfo> getMyTribe() { return myTribe; }
        public void addTribeCard(CardInfo c) { this.myTribe.add(c); }
        public java.util.List<CardInfo> getMyBuildings() { return myBuildings; }
        public void addBuildingCard(CardInfo c) { this.myBuildings.add(c); }
    }

    /**
     * Info ESTESA di una carta per ricostruzione univoca da persistenza.
     */
    public static class CardInfo {
        private String name;
        private String type;        // "CHARACTER", "BUILDING", "EVENT"
        private String javaType;    // "CharacterCard", "BuildingCard", "EventCard"
        private int cost;
        private int era;
        
        // Campi specifici CharacterCard
        private int level;
        private int aura;
        private String jobType;     // "HUNTER", "FARMER", ecc.
        
        // Campi specifici BuildingCard
        private String buildingType;
        private int finalAura;
        
        // Campi specifici EventCard
        private int eventBaseFood;
        private int eventAuraPerChar;
        private int eventThreshold;
        private boolean finalEvent;

        public CardInfo() {}
        public CardInfo(String name, String type, int cost) {
            this.name = name;
            this.type = type;
            this.cost = cost;
        }

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getType() { return type; }
        public void setType(String v) { this.type = v; }
        public String getJavaType() { return javaType; }
        public void setJavaType(String v) { this.javaType = v; }
        public int getCost() { return cost; }
        public void setCost(int v) { this.cost = v; }
        public int getEra() { return era; }
        public void setEra(int v) { this.era = v; }
        public int getLevel() { return level; }
        public void setLevel(int v) { this.level = v; }
        public int getAura() { return aura; }
        public void setAura(int v) { this.aura = v; }
        public String getJobType() { return jobType; }
        public void setJobType(String v) { this.jobType = v; }
        public String getBuildingType() { return buildingType; }
        public void setBuildingType(String v) { this.buildingType = v; }
        public int getFinalAura() { return finalAura; }
        public void setFinalAura(int v) { this.finalAura = v; }
        public int getEventBaseFood() { return eventBaseFood; }
        public void setEventBaseFood(int v) { this.eventBaseFood = v; }
        public int getEventAuraPerChar() { return eventAuraPerChar; }
        public void setEventAuraPerChar(int v) { this.eventAuraPerChar = v; }
        public int getEventThreshold() { return eventThreshold; }
        public void setEventThreshold(int v) { this.eventThreshold = v; }
        public boolean isFinalEvent() { return finalEvent; }
        public void setFinalEvent(boolean v) { this.finalEvent = v; }
    }

    /**
     * Snapshot della board (carte nelle file, offering track, turn order).
     */
    public static class BoardSnapshot {
        private java.util.List<CardInfo> characterUpperRow = new java.util.ArrayList<>();
        private java.util.List<CardInfo> characterLowerRow = new java.util.ArrayList<>();
        private java.util.List<CardInfo> eventUpperRow = new java.util.ArrayList<>();
        private java.util.List<CardInfo> eventLowerRow = new java.util.ArrayList<>();
        private java.util.List<CardInfo> buildingUpperRow = new java.util.ArrayList<>();
        private java.util.List<CardInfo> buildingLowerRow = new java.util.ArrayList<>();
        private java.util.List<OfferingTileInfo> offeringTrack = new java.util.ArrayList<>();
        private java.util.List<TurnOrderSlotInfo> turnOrderTile = new java.util.ArrayList<>();

        public java.util.List<CardInfo> getCharacterUpperRow() { return characterUpperRow; }
        public void addCharacterUpperRow(CardInfo c) { this.characterUpperRow.add(c); }
        public java.util.List<CardInfo> getCharacterLowerRow() { return characterLowerRow; }
        public void addCharacterLowerRow(CardInfo c) { this.characterLowerRow.add(c); }
        public java.util.List<CardInfo> getEventUpperRow() { return eventUpperRow; }
        public void addEventUpperRow(CardInfo c) { this.eventUpperRow.add(c); }
        public java.util.List<CardInfo> getEventLowerRow() { return eventLowerRow; }
        public void addEventLowerRow(CardInfo c) { this.eventLowerRow.add(c); }
        public java.util.List<CardInfo> getBuildingUpperRow() { return buildingUpperRow; }
        public void addBuildingUpperRow(CardInfo c) { this.buildingUpperRow.add(c); }
        public java.util.List<CardInfo> getBuildingLowerRow() { return buildingLowerRow; }
        public void addBuildingLowerRow(CardInfo c) { this.buildingLowerRow.add(c); }
        public java.util.List<OfferingTileInfo> getOfferingTrack() { return offeringTrack; }
        public void addOfferingTile(OfferingTileInfo t) { this.offeringTrack.add(t); }
        public java.util.List<TurnOrderSlotInfo> getTurnOrderTile() { return turnOrderTile; }
        public void addTurnOrderSlot(TurnOrderSlotInfo t) { this.turnOrderTile.add(t); }
    }

    /**
     * Info di una tessera Offerta.
     */
    public static class OfferingTileInfo {
        private char id;
        private int cardsFromUpper;
        private int cardsFromLower;
        private int foodBonus;

        public OfferingTileInfo() {}
        public OfferingTileInfo(char id, int upper, int lower, int food) {
            this.id = id;
            this.cardsFromUpper = upper;
            this.cardsFromLower = lower;
            this.foodBonus = food;
        }

        public char getId() { return id; }
        public void setId(char v) { this.id = v; }
        public int getCardsFromUpper() { return cardsFromUpper; }
        public void setCardsFromUpper(int v) { this.cardsFromUpper = v; }
        public int getCardsFromLower() { return cardsFromLower; }
        public void setCardsFromLower(int v) { this.cardsFromLower = v; }
        public int getFoodBonus() { return foodBonus; }
        public void setFoodBonus(int v) { this.foodBonus = v; }
    }

    /**
     * Info di uno slot dell'ordine di turno.
     */
    public static class TurnOrderSlotInfo {
        private char id;
        private String occupiedByNickname;
        private boolean occupied;

        public TurnOrderSlotInfo() {}
        public TurnOrderSlotInfo(char id, String nickname, boolean occupied) {
            this.id = id;
            this.occupiedByNickname = nickname;
            this.occupied = occupied;
        }

        public char getId() { return id; }
        public void setId(char v) { this.id = v; }
        public String getOccupiedByNickname() { return occupiedByNickname; }
        public void setOccupiedByNickname(String v) { this.occupiedByNickname = v; }
        public boolean isOccupied() { return occupied; }
        public void setOccupied(boolean v) { this.occupied = v; }
    }
}