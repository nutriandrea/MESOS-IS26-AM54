package model.Model;

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
     * 
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
        board.generateBoard(chosenPlayerNumber);



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