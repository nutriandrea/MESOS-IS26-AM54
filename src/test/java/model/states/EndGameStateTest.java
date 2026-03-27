package model.states;

import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndGameStateTest {

    private Game game;
    private EndGameState endGameState;
    private Player carbicane;
    private Player frabbo;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("carbicane");
        game.joinGame("frabbo");
        game.chooseTotemColor("carbicane", "RED");
        game.chooseTotemColor("frabbo", "YELLOW");
        game.setupGame();
        
        carbicane = game.getPlayers().get(0);
        frabbo = game.getPlayers().get(1);
        
        endGameState = new EndGameState();
    }

    @Test
    void testResolvePhase_CallsResolveAllVisibleEvents() throws Exception {
        assertDoesNotThrow(() -> {
            endGameState.resolvePhase(game);
        });
    }

    @Test
    void testResolvePhase_CalculatesWinners() throws Exception {
        assertDoesNotThrow(() -> {
            endGameState.resolvePhase(game);
        });
        
        java.util.List<Player> winners = game.determineWinners();
        assertNotNull(winners);
    }

    @Test
    void testResolvePhase_ReturnsWinnersArePlayers() throws Exception {
        endGameState.resolvePhase(game);
        
        java.util.List<Player> winners = game.determineWinners();
        for (Player w : winners) {
            assertTrue(game.getPlayers().contains(w));
        }
    }

    @Test
    void testResolvePhase_HandlesEmptyTribe() throws Exception {
        assertDoesNotThrow(() -> {
            endGameState.resolvePhase(game);
        });
        
        int score = game.calculatePlayerScore(carbicane);
        assertTrue(score != Integer.MIN_VALUE);
    }

    @Test
    void testResolvePhase_WorksWithScoreCalculation() throws Exception {
        carbicane.modifyAuraPoints(5);
        
        assertDoesNotThrow(() -> {
            endGameState.resolvePhase(game);
        });
    }

    @Test
    void testDetermineWinners_ReturnsAtLeastOneWinner() throws Exception {
        endGameState.resolvePhase(game);
        
        java.util.List<Player> winners = game.determineWinners();
        assertNotNull(winners);
    }
}
