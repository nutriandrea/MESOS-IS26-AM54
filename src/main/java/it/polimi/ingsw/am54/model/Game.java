package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.SetUpGameState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import it.polimi.ingsw.am54.model.states.StartTurnState;

import java.util.*;


public class Game implements IGame{


    //ATTRIBUTI:

    private Board board;
    private Deck<TribeCard> deckTribu;
    private Deck<BuildingCard> deckBuildings;
    private List<Player> players;

    private int currentEra;
    private int roundCount;
    private Player currentPlayer;

    private GameState currentState;

    private int activePlayerCount = 0;

    private List<IGameObserver> observers;

    // I campi offeringTiles, turnOrderSlots, upperRow, lowerRow sono gestiti da Board
    public Game() {
        this.observers = new ArrayList<>();
        this.players = new ArrayList<>();
        this.currentState = new SetUpGameState();
    }

    // ==========================================
    // METODI DELL'INTERFACCIA IGAME
    // ==========================================

    @Override
    public void joinGame(String nickname) throws Exception {
        currentState.joinGame(this, nickname);
        notifyPlayerJoined(getPlayerByNickname(nickname));
    }

    @Override
    public void chooseTotemColor(String nickname, String color) throws Exception {
        currentState.chooseTotemColor(this, nickname, color);
        notifyTotemColorChosen(getPlayerByNickname(nickname), TotemColors.valueOf(color.toUpperCase()));
    }

    @Override
    public void placeTotem(String nickname, int offerPosition) throws Exception {
        currentState.placeTotem(this, nickname, offerPosition);
    }

    @Override
    public void pickCard(String nickname, String row, int cardIndex) throws Exception {
        currentState.pickCard(this, nickname, row, cardIndex);
    }

    @Override
    public void addObserver(IGameObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(IGameObserver observer) {
        observers.remove(observer);
    }

    // ==========================================
    // FASE DI SETUP (Rifattorizzata)
    // ==========================================

    public void setBoard(Board board) { this.board = board; } // per i test

    public void setupGame() throws Exception {
        this.currentEra = 1;
        this.roundCount = 1; // Corretto: il gioco inizia dal Round 1, non da 0

        // 1. Creazione Mazzi (Delegata alla Factory)
        this.deckTribu = DeckFactory.createTribeDeck(
                "data/CharacterCardsSetup.json",
                "data/EventCardsSetup.json",
                players.size()
        );
        this.deckBuildings = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", players.size());

        // 2. Inizializzazione Board
        this.board = new Board();
        this.board.initializeLayout(players.size());

        // 3. Popolamento iniziale delle carte (Il Game agisce da Orchestratore)
        fillInitialTribeRows();
        fillInitialBuildingRow();

        // 4. Calcolo ordine di turno casuale e distribuzione Cibo iniziale
        List<Player> tempPlayers = new ArrayList<>(this.players);
        Collections.shuffle(tempPlayers);
        this.board.setupInitialTurnOrder(tempPlayers);
        distributeInitialFood(tempPlayers);

        // 5. Imposta esplicitamente il PRIMO giocatore prima di cambiare stato (Risolve il bug di avvio)
        this.setCurrentPlayer(tempPlayers.get(0));

        // 6. Passaggio allo stato di Inizio Turno
        this.setState(new StartTurnState());
    }

    private void fillInitialTribeRows() {
        int numPlayers = players.size();

        // REGOLE: Fila in basso = Giocatori + 1
        int requiredCharactersLower = numPlayers + 1;
        while (board.getCharacterRowSize("lower") < requiredCharactersLower) {
            TribeCard card = deckTribu.drawCard();
            if (card != null) {
                // Il metodo reveal manderà automaticamente i Personaggi giù (gli eventi tornano su se pescati, come da tua logica)
                card.reveal(board, "lower");
            } else break;
        }

        // REGOLE: Fila in alto = Giocatori + 4 (Somma di Personaggi + Eventi)
        int requiredTotalUpper = numPlayers + 4;
        while ((board.getCharacterRowSize("upper") + board.getEventRowSize("upper")) < requiredTotalUpper) {
            TribeCard card = deckTribu.drawCard();
            if (card != null) {
                card.reveal(board, "upper");
            } else break;
        }
    }

    private void fillInitialBuildingRow() {
        int numPlayers = players.size();
        int cardsToDraw = 0;

        // Leggiamo la configurazione JSON degli edifici dall'interno del Model, non dalla Board
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode countsRoot = mapper.readTree(new File("data/EraCounts.json"));
            JsonNode buildingCounts = countsRoot.get("buildingCounts").get(String.valueOf(numPlayers));

            if (buildingCounts != null && buildingCounts.isArray()) {
                cardsToDraw = buildingCounts.get(currentEra - 1).asInt();
            } else {
                throw new IllegalStateException("Nodo 'buildingCounts' mancante o non valido.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile leggere il file EraCounts.json: " + e.getMessage(), e);
        }

        // Pescaggio e posizionamento Edifici
        for (int i = 0; i < cardsToDraw; i++) {
            BuildingCard card = deckBuildings.drawCard();
            if (card != null) {
                board.addBuildingCard(card, "upper");
            }
        }
    }


    private void distributeInitialFood(List<Player> orderedPlayers) {

        // 1°: 2 Cibo
        // 2°-3°: 3 Cibo
        // 4°-5°: 4 Cibo
        for (int i = 0; i < orderedPlayers.size(); i++) {
            Player player = orderedPlayers.get(i);
            int foodAmount = 0;
            
            if (i == 0) {
                foodAmount = 2;
            } else if (i == 1 || i == 2) {
                foodAmount = 3;
            } else if (i == 3 || i == 4) {
                foodAmount = 4;
            }

            // MEME
            if (player.getNickname().equals("gabs")) {
                foodAmount = 1000;
            }
            // FINE MEME

            player.modifyFood(foodAmount);
        }

        /*
        // Configurazione di sblocco totale per il testing sul campo
        for (Player player : orderedPlayers) {
            player.modifyFood(10000);       // Cibo infinito per comprare tutto
            player.modifyAuraPoints(-1000);   // Punti Aura per testare la stabilità dei tracciati
            player.modifyMagicPoints(3);  // Punti Magia pronti all'uso
        }
         */
    }

    public void setState(GameState newState) {
        this.currentState = newState;
        notifyStateChanged(newState);
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void nextPlayer() {
        List<TurnOrderSlot> turnTrack = board.getTurnOrderTile();

        // troviamo in quale slot si trova il giocatore corrente
        int currentIndex = 0;
        boolean foundCurrent = false;

        while (currentIndex < turnTrack.size() && !foundCurrent) {
            if (currentPlayer.equals(turnTrack.get(currentIndex).getOccupiedBy())) {
                foundCurrent = true;
            } else {
                currentIndex++;
            }
        }

        // passiamo al prossimo slot occupato
        boolean nextFound = false;
        int offset = 1;

        while (offset <= turnTrack.size() && !nextFound) {
            int nextIndex = (currentIndex + offset) % turnTrack.size();
            Player nextP = turnTrack.get(nextIndex).getOccupiedBy();

            // Se in questo slot c'è seduto qualcuno, e non ha ancora piazzato il totem
            if (nextP != null && !nextP.hasPlacedTotem()) {
                this.currentPlayer = nextP; // aggiorniamo il giocatore corrente
                nextFound = true; // condizione soddisfatta
            } else {
                offset++; //posto successivo
            }
        }
        // Se abbiamo effettivamente trovato e cambiato il giocatore, avvisiamo la grafica
        if (nextFound) {
            //nessun print, sarà la view dopo essere notificata a farlo
            notifyTurnChanged(currentPlayer);
        }
    }

    public void advanceEra(){this.currentEra++;}
    public void advanceRound(){this.roundCount++;}


    // ==========================================
    // METODI DI REFILL FINE ROUND
    // ==========================================

    public void refillUpperTribeRow() {
        int targetSize = players.size() + 4;

        // Continua a pescare finché la somma di Personaggi + Eventi in alto non raggiunge il target
        while ((board.getCharacterRowSize("upper") + board.getEventRowSize("upper")) < targetSize) {
            TribeCard card = deckTribu.drawCard();
            if (card == null) break; // Mazzo finito, ci fermiamo
            card.reveal(board, "upper");
        }
    }

    public void refillUpperBuildings() {
        int cardsToDraw = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode countsRoot = mapper.readTree(new File("data/EraCounts.json"));
            JsonNode buildingCounts = countsRoot.get("buildingCounts").get(String.valueOf(players.size()));

            if (buildingCounts != null && buildingCounts.isArray()) {
                cardsToDraw = buildingCounts.get(currentEra - 1).asInt();
            }
        } catch (Exception e) {
            System.err.println("Errore lettura EraCounts: " + e.getMessage());
        }

        for (int i = 0; i < cardsToDraw; i++) {
            BuildingCard card = deckBuildings.drawCard();
            if (card != null) {
                board.addBuildingCard(card, "upper");
            }
        }
    }

    public Player getPlayerByNickname(String nickname) throws Exception {
        return this.players.stream()
                .filter(p -> p.getNickname().equalsIgnoreCase(nickname))
                .findFirst()
                .orElseThrow(() -> new Exception("Errore: Giocatore '" + nickname + "' non trovato."));
    }

    private void notifyPlayerJoined(Player player) {
        for (IGameObserver obs : observers) obs.onPlayerJoined(this, player);
    }

    private void notifyTotemColorChosen(Player player, TotemColors color) {
        for (IGameObserver obs : observers) obs.onTotemColorChosen(this, player, color);
    }

    public void notifyTotemPlaced(Player player, int offerPosition) {
        for (IGameObserver obs : observers) obs.onTotemPlaced(this, player, offerPosition);
    }

    public void notifyCardPicked(Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {
        for (IGameObserver obs : observers) obs.onCardPicked(this, player, row, cardIndex, newFoodBalance, newMagicBalance);
    }

    private void notifyTurnChanged(Player player) {
        for (IGameObserver obs : observers) obs.onTurnChanged(this, player);
    }

    private void notifyStateChanged(GameState newState) {
        for (IGameObserver obs : observers) obs.onStateChanged(this, newState);
    }

    // Dentro Game.java
    public void notifyTotemReturned(Player player, int position) {
        // Leggo i saldi definitivi dal server!
        int finalFood = player.getFood();
        int finalAura = player.getAuraPoints();

        for (IGameObserver obs : observers) {
            obs.onTotemReturned(this, player, position, finalFood, finalAura);
        }
    }

    public void notifyEventResolved(EventCard eventCard) {
        for (IGameObserver obs : observers) {
            obs.onEventResolved(this, eventCard);
        }
    }

    @Override
    public void endTurn(String nickname) throws Exception {
        currentState.endTurn(this, nickname);
    }


    // ==========================================
    // GETTERS & SETTERS ESSENZIALI
    // ==========================================

    public List<Player> getPlayers() { return players; }
    public int getPlayersNumber() { return players.size(); }

    public Board getBoard() { return this.board; }
    public Deck<TribeCard> getTribeDeck() { return deckTribu; }
    public Deck<BuildingCard> getBuildingDeck() { return deckBuildings; }
    public void setTribeDeck(Deck<TribeCard> deck) { this.deckTribu = deck; }
    public void setBuildingDeck(Deck<BuildingCard> deck) { this.deckBuildings = deck; }

    public int getCurrentEra() { return currentEra; }
    public int getRoundCount() { return roundCount; }

    /**
     * Imposta l'era corrente.
     * Utilizzato dal Server per ripristinare lo stato del gioco dopo un crash.
     *
     * @param era l'era da impostare (1-6)
     */
    public void setCurrentEra(int era) { this.currentEra = era; }

    /**
     * Imposta il numero del round corrente.
     * Utilizzato dal Server per ripristinare lo stato del gioco dopo un crash.
     *
     * @param round il numero del round da impostare
     */
    public void setRoundCount(int round) { this.roundCount = round; }

    public Player getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        notifyTurnChanged(player);
    }

    /**
     * Imposta il numero di giocatori attivi per il round corrente.
     * Usato dal Controller per informare il Model di quanti giocatori
     * stanno effettivamente partecipando (escludendo i disconnessi).
     */
    public void setActivePlayerCount(int count) { this.activePlayerCount = count; }

    /**
     * Restituisce il numero di giocatori attivi per il round corrente.
     * Se non impostato (0), usa il numero totale di giocatori.
     */
    public int getActivePlayerCount() {
        return (activePlayerCount > 0) ? activePlayerCount : players.size();
    }

    public Player getCurrentPicker() {
        List<OfferingTile> tiles = board.getOfferingTrack();
        
        for (OfferingTile tile : tiles) {
            Player occupant = tile.getOccupiedBy();
            if (occupant != null && !isInTurnOrder(occupant)) {
                return occupant;
            }
        }
        return null;
    }

    private boolean isInTurnOrder(Player p) {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (p.equals(slot.getOccupiedBy())) {
                return true;
            }
        }
        return false;
    }

    public TurnOrderSlot assignPickerToTurnOrder(Player p) {
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (slot.getOccupiedBy() == null) {
                slot.occupy(p);
                return slot;
            }
        }
        return null;
    }

    public boolean isPickingComplete() {
        int needed = (activePlayerCount > 0) ? activePlayerCount : players.size();
        int filled = 0;
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            if (slot.getOccupiedBy() != null) filled++;
        }
        return filled >= needed;
    }

    public boolean isTribeDeckEmpty() {
        return deckTribu.isEmpty();
    }

    // True se la prossima carta del deck è di un'era successiva
    public boolean isEraTransition() {
        TribeCard nextCard = deckTribu.peek();
        if (nextCard == null) return false;
        return nextCard.getEra() > currentEra;
    }

    // ... [Tutto il codice sopra rimane identico fino a isEraTransition()]

    public int calculatePlayerScore(Player p) {
        int score = 0;

        // 1. Punti Aura base (inclusi quelli appena aggiunti dinamicamente da EndGameState)
        score += p.getAuraPoints();

        // 2. Bonus Inventori
        long inventors = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR).count();
        long uniqueInventions = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR)
                .mapToInt(c -> c.getLevel())
                .distinct().count();
        score += (int) (inventors * uniqueInventions);

        // 3. Bonus Artisti
        long artists = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.ARTIST).count();
        score += (int) (artists / 2) * 10;

        // 4. Bonus Costruttori
        long CONSTRUCTORs = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.CONSTRUCTOR)
                .mapToInt(CharacterCard::getAura).sum();
        score += (int) CONSTRUCTORs;

        // 5. Punti base stampati sulle carte Edificio
        score += p.getBuildings().stream()
                .mapToInt(BuildingCard::getFinalAura).sum();

        return score;
    }

    public List<Player> determineWinners() {
        // NESSUN EFFETTO CHIAMATO QUI.
        // EndGameState ha già provveduto a chiamare applyOnEndGame prima di invocare questo metodo!

        List<Player> winners = new ArrayList<>();
        int maxScore = -999;

        for (Player p : players) {
            int score = calculatePlayerScore(p);
            if (score > maxScore) {
                maxScore = score;
                winners.clear();
                winners.add(p);
            } else if (score == maxScore) {
                winners.add(p);
            }
        }

        // Spareggio sul cibo
        if (winners.size() > 1) {
            int maxFood = 0;
            for (Player p : winners) {
                maxFood = Math.max(maxFood, p.getFood());
            }
            final int finalMaxFood = maxFood;
            winners = winners.stream()
                    .filter(p -> p.getFood() == finalMaxFood)
                    .toList();
        }

        return winners;
    }

    public void notifyEndGame(List<Player> winners) {
        // anche qui il model si limita ad avvisare tutti gli osservatori (le view)
        for (IGameObserver obs : observers) {
            obs.onEndGame(this, winners);
        }
    }
}