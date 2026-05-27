package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.SetUpGameState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
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

    private transient List<IGameObserver> observers = new ArrayList<>();

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
        if (!getSafeObservers().contains(observer)) {
            getSafeObservers().add(observer);
        }
    }

    @Override
    public void removeObserver(IGameObserver observer) {
        getSafeObservers().remove(observer);
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
            JsonObject countsRoot = JsonParser.parseReader(new FileReader("data/EraCounts.json")).getAsJsonObject();
            JsonArray buildingCounts = countsRoot.getAsJsonObject("buildingCounts").getAsJsonArray(String.valueOf(numPlayers));

            if (buildingCounts != null) {
                cardsToDraw = buildingCounts.get(currentEra - 1).getAsInt();
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

            /*// MEME
            if (player.getNickname().equals("tricheco")|| player.getNickname().equalsIgnoreCase("Tester")|| player.getNickname().equalsIgnoreCase("Tester2")) {
                foodAmount = 1000;
            }
            // FINE MEME*/

            player.modifyFood(foodAmount);
        }


        /*
        // Configurazione di sblocco totale per il testing sul campo
        for (Player player : orderedPlayers) {
            player.modifyFood(10000);       // Cibo infinito per comprare tutto
            player.modifyAuraPoints(0);   // Punti Aura per testare la stabilità dei tracciati
            player.modifyMagicPoints(0);  // Punti Magia pronti all'uso
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
        int offset = 1;

        while (offset <= turnTrack.size()) {
            int nextIndex = (currentIndex + offset) % turnTrack.size();
            Player nextP = turnTrack.get(nextIndex).getOccupiedBy();

            // Se in questo slot c'è seduto qualcuno, e non ha ancora piazzato il totem
            if (nextP != null && !nextP.hasPlacedTotem()) {
                this.currentPlayer = nextP;
                notifyTurnChanged(currentPlayer);
                return; // Abbiamo trovato il prossimo, usciamo dal metodo
            } else {
                offset++; // posto successivo
            }
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
            JsonObject countsRoot = JsonParser.parseReader(new FileReader("data/EraCounts.json")).getAsJsonObject();
            JsonArray buildingCounts = countsRoot.getAsJsonObject("buildingCounts").getAsJsonArray(String.valueOf(players.size()));

            if (buildingCounts != null) {
                cardsToDraw = buildingCounts.get(currentEra - 1).getAsInt();
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
        for (IGameObserver obs : getSafeObservers()) obs.onPlayerJoined(this, player);
    }

    private void notifyTotemColorChosen(Player player, TotemColors color) {
        for (IGameObserver obs : getSafeObservers()) obs.onTotemColorChosen(this, player, color);
    }

    public void notifyTotemPlaced(Player player, int offerPosition) {
        for (IGameObserver obs : getSafeObservers()) obs.onTotemPlaced(this, player, offerPosition);
    }

    public void notifyCardPicked(Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {
        for (IGameObserver obs : getSafeObservers()) obs.onCardPicked(this, player, row, cardIndex, newFoodBalance, newMagicBalance);
    }

    private void notifyTurnChanged(Player player) {
        for (IGameObserver obs : getSafeObservers()) obs.onTurnChanged(this, player);
    }

    private void notifyStateChanged(GameState newState) {
        for (IGameObserver obs : getSafeObservers()) obs.onStateChanged(this, newState);
    }

    public void notifyTotemReturned(Player player, int position) {
        int finalFood = player.getFood();
        int finalAura = player.getAuraPoints();

        for (IGameObserver obs : getSafeObservers()) {
            obs.onTotemReturned(this, player, position, finalFood, finalAura);
        }
    }

    public void notifyEventResolved(EventCard eventCard) {
        for (IGameObserver obs : getSafeObservers()) {
            obs.onEventResolved(this, eventCard);
        }
    }

    @Override
    public void endTurn(String nickname) throws Exception {
        currentState.endTurn(this, nickname);
    }

    public synchronized void removePlayerEntirely(String nickname) {
        // 1. Rimuove fisicamente il giocatore dalla lista
        players.removeIf(p -> p.getNickname().equalsIgnoreCase(nickname));

        // 2. Se tutti abbandonano a fine partita, riporta il server allo stato di Setup iniziale
        if (players.isEmpty() && currentState.shouldResetToSetupOnEmpty()) {
            this.setState(new SetUpGameState());
        }
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
     * Risolve il problema dei "Cloni di Gson".
     * Deserializzando il JSON, Gson crea copie distinte per gli oggetti referenziati più volte.
     * Questo metodo riallaccia i puntatori (currentPlayer, slot della board)
     * alle UNICHE istanze ufficiali contenute nella lista 'players'.
     */
    public void restoreReferences() {
        // 1. Ripristina il currentPlayer
        if (this.currentPlayer != null) {
            try {
                this.currentPlayer = getPlayerByNickname(this.currentPlayer.getNickname());
            } catch (Exception e) {}
        }

        // 2. Ripristina gli occupanti degli slot di turno (Turn Order)
        if (this.board != null && this.board.getTurnOrderTile() != null) {
            for (it.polimi.ingsw.am54.model.TurnOrderSlot slot : this.board.getTurnOrderTile()) {
                if (slot.getOccupiedBy() != null) {
                    try {
                        slot.occupy(getPlayerByNickname(slot.getOccupiedBy().getNickname()));
                    } catch (Exception e) {}
                }
            }
        }

        // 3. Ripristina gli occupanti delle offerte e ricollega il riferimento inverso
        if (this.board != null && this.board.getOfferingTrack() != null) {
            for (it.polimi.ingsw.am54.model.OfferingTile tile : this.board.getOfferingTrack()) {
                if (tile.getOccupiedBy() != null) {
                    try {
                        Player p = getPlayerByNickname(tile.getOccupiedBy().getNickname());
                        tile.occupy(p);
                        p.setCurrentTile(tile);
                    } catch (Exception e) {}
                }
            }
        }
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

    public TurnOrderSlot moveToLastSlot(Player p) {
        List<TurnOrderSlot> slots = board.getTurnOrderTile();

        // libera il giocatore dallo slot corrente
        int freedIndex = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (p.equals(slots.get(i).getOccupiedBy())) {
                slots.get(i).freeSlot();
                freedIndex = i;
                break;
            }
        }

        // compatta spostando a sinistra gli occupanti dopo freedIndex
        if (freedIndex >= 0) {
            for (int i = freedIndex; i < slots.size() - 1; i++) {
                Player occupant = slots.get(i + 1).getOccupiedBy();
                if (occupant != null) {
                    slots.get(i + 1).freeSlot();
                    slots.get(i).occupy(occupant);
                }
            }
        }

        int lastIdx = slots.size() - 1;
        TurnOrderSlot lastSlot = slots.get(lastIdx);
        if (lastSlot.getOccupiedBy() == null) {
            lastSlot.occupy(p);
            return lastSlot;
        }

        return null;
    }

    public boolean isPickingComplete() {
        // Il conteggio basato su activePlayerCount fallisce quando un giocatore
        // disconnesso viene spostato nel turn order da moveToLastSlot (in
        // autoHandleDisconnection): viene conteggiato come "aver pescato",
        // saltando i giocatori ancora sul tracciato offerte.
        //
        // La condizione corretta: tutti i giocatori che hanno piazzato un totem
        // sul tracciato offerte hanno completato la pesca, cioè il loro occupante
        // è stato spostato nel turn order (o la tessera è stata liberata).
        for (OfferingTile tile : board.getOfferingTrack()) {
            Player occupant = tile.getOccupiedBy();
            if (occupant != null && !isInTurnOrder(occupant)) {
                return false;
            }
        }
        return true;
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

    public int getPlayerInventorsBonus(Player p) {
        long inventors = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR).count();
        long uniqueInventions = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR)
                .mapToInt(c -> c.getLevel())
                .distinct().count();
        return (int) (inventors * uniqueInventions);
    }

    public int getPlayerArtistsBonus(Player p) {
        long artists = p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.ARTIST).count();
        return (int) (artists / 2) * 10;
    }

    public int getPlayerConstructorsBonus(Player p) {
        return p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.CONSTRUCTOR)
                .mapToInt(CharacterCard::getAura).sum();
    }

    public int getPlayerBuildingsBaseAura(Player p) {
        return p.getBuildings().stream()
                .mapToInt(BuildingCard::getFinalAura).sum();
    }

    public int calculatePlayerScore(Player p) {
        int score = 0;
        // 1. Punti Aura comprensivi di modifiche EndGameState
        score += p.getAuraPoints();
        // 2. Bonus Inventori
        score += getPlayerInventorsBonus(p);
        // 3. Bonus Artisti
        score += getPlayerArtistsBonus(p);
        // 4. Bonus Costruttori
        score += getPlayerConstructorsBonus(p);
        // 5. Punti base stampati sulle carte Edificio
        score += getPlayerBuildingsBaseAura(p);

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
        for (IGameObserver obs : getSafeObservers()) {
            obs.onEndGame(this, winners);
        }
    }

    private List<IGameObserver> getSafeObservers() {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        return observers;
    }
}