package model;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculationTest {

    private Game game;
    private Player carbicane;
    private Player frabbo;

    @BeforeEach
    void setUp() {
        game = new Game();
        
        try {
            game.joinGame("carbicane");
            game.joinGame("frabbo");
            game.chooseTotemColor("carbicane", "RED");
            game.chooseTotemColor("frabbo", "YELLOW");
            game.setupGame();
            
            carbicane = game.getPlayerByNickname("carbicane");
            frabbo = game.getPlayerByNickname("frabbo");
        } catch (Exception e) {
            fail("Setup should not throw: " + e.getMessage());
        }
    }

    @Test
    void testCalculatePlayerScore_EmptyPlayer() {
        int score = game.calculatePlayerScore(carbicane);
        assertEquals(0, score);
    }

    @Test
    void testDetermineWinners_ReturnsNonEmptyList() {
        List<Player> winners = game.determineWinners();
        assertNotNull(winners);
        assertFalse(winners.isEmpty());
    }

    @Test
    void testEraTransition() {
        assertFalse(game.isEraTransition());
        assertEquals(1, game.getCurrentEra());
    }

    @Test
    void testAdvanceRound() {
        int initialRound = game.getRoundCount();
        game.advanceRound();
        assertEquals(initialRound + 1, game.getRoundCount());
    }

    @Test
    void testApplyEndGameBuildingEffects_NoErrors() {
        assertDoesNotThrow(() -> game.applyEndGameBuildingEffects());
    }

    @Test
    void testGetCurrentPicker_MethodExists() {
        assertDoesNotThrow(() -> {
            game.getCurrentPicker();
        });
    }
}
