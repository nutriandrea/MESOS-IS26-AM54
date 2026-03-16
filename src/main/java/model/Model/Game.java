package model.Model;

import javafx.beans.binding.ListBinding;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class Game {

    /**
     * Default constructor
     */
    private Game() {
    }

    /**
     * ©©
     */
    private Board board;

    /**
     * 
     */
    private Deck<TribeCard> deckTribu;

    /**
     * 
     */
    private Deck<BuildingCard> deckBuildings;

    /**
     * 
     */
    private List<Player> players;

    /**
     * 
     */
    private int chosenPlayerNumber;

    /**
     * 
     */
    private int currentEra;

    /**
     * 
     */
    private int roundCount;

    /**
     * 
     */
    private GameStatus status;

    /**
     * 
     */
    private Player currentPlayer;







    /**
     * @return
     */
    public int getPlayerNumber() {
        // TODO implement here
        return 0;
    }

    /**
     * @return
     */
    public Player getCurrentPlayer() {
        // TODO implement here
        return null;
    }

    /**
     * @return
     */
    public void setupGame() {
        // TODO implement here
        System.out.println("Inizializzazione della partita...");

        // 1. STATO INIZIALE DEL GIOCO
        this.status = GameStatus.START;
        this.currentEra = 1;
        this.roundCount = 0;

        // 2. CREAZIONE DEI MAZZI TRAMITE FACTORY
        // L'UML dice che DeckFactory restituisce uno Stack. Lo passiamo al costruttore del nostro Deck.
        Stack<BuildingCard> stackEdifici = DeckFactory.createBuildingDeck("buildings_data.csv");
        this.deckBuildings = new Deck<>(stackEdifici);

        Stack<TribeCard> stackTribu = DeckFactory.createTribeDeck("tribes_data.csv");
        this.deckTribu = new Deck<>(stackTribu);

        // Mescoliamo subito i mazzi
        this.deckBuildings.shuffle();
        this.deckTribu.shuffle();

        // 3. SETUP DEI GIOCATORI (Player [cite: 2])
        // Assumiamo che la lista 'players' sia già stata popolata dal Controller (UI)
        // con nomi e colori scelti. Qui assegniamo le risorse di base.
        for (Player p : this.players) {
            p.modifyFood(5); // Es: 5 cibo iniziale a tutti
            // I punti aura e magia partono da 0 di default
        }

        // 4. ORDINE DI TURNO INIZIALE
        // Copiamo la lista dei giocatori e la mescoliamo per decidere l'ordine del primissimo round
        this.turnOrder = new ArrayList<>(this.players);
        Collections.shuffle(this.turnOrder);

        // Impostiamo il primo giocatore del round
        this.currentPlayer = this.turnOrder.get(0);

        // 5. CREAZIONE DEL TABELLONE (Board)
        this.board = new Board();

        // Generiamo l'infrastruttura del tabellone in base al numero di giocatori
        // (Es: il numero di TurnOrderSlot o OfferingTile cambia se si gioca in 2 o in 5)
        this.chosenPlayerNumber = this.players.size();
        this.board.generateBoard(this.chosenPlayerNumber);

        // 6. POPOLAMENTO DEL MERCATO DELLE CARTE
        // Dato che nel tuo UML i metodi refill non prendono il mazzo in input,
        // presumibilmente il Game pesca e passa le carte al Board, oppure dovrai
        // passare il mazzo come parametro a refillUpperRow() nel codice vero.
        // Logica semplificata:
        this.board.refillUpperRow(this.chosenPlayerNumber /*, this.deckTribu */);

        // 7. FINE SETUP
        this.status = GameStatus.INGAME;
        System.out.println("Setup completato! Inizia il giocatore: " + this.currentPlayer.getNickname());

    }

    /**
     * @param Player p 
     * @param TurnOrderSlot selectedSlot 
     * @return
     */
    public void placeTotemOrder(Player p, TurnOrderSlot selectedSlot) {
        // TODO implement here
    }

    /**
     * @return
     */
    public void startRound() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void pickingPhase() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void resolveEvents() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void nextTurn() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void handleEraTransition() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void calculateFinalScore() {
        // TODO implement here
    }

    /**
     * @return
     */
    public void endGame() {
        // TODO implement here
    }

}