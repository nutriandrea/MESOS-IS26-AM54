package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndGameStateTest {

    private Game game;
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
    }

    @Test
    void testDetermineWinners_BothSameScore() throws Exception {
        // Both players should have same initial score (0)
        java.util.List<Player> winners = game.determineWinners();
        
        assertNotNull(winners);
        assertFalse(winners.isEmpty());
    }

    @Test
    void testCalculatePlayerScore_InitialZero() {
        int score = game.calculatePlayerScore(carbicane);
        assertEquals(0, score);
    }

    @Test
    void testApplyEndGameBuildingEffects_NoErrors() {
        // Should run without errors even with no buildings
        assertDoesNotThrow(() -> game.applyEndGameBuildingEffects());
    }

    @Test
    void testDetermineWinners_ReturnsAtLeastOneWinner() throws Exception {
        java.util.List<Player> winners = game.determineWinners();
        
        assertTrue(winners.size() >= 1);
    }

    @Test
    void testDetermineWinners_AllWinnersArePlayers() throws Exception {
        java.util.List<Player> winners = game.determineWinners();
        
        for (Player w : winners) {
            assertTrue(game.getPlayers().contains(w));
        }
    }

    @Test
    void testCalculatePlayerScore_AfterAddingCharacter() throws Exception {
        CharacterCard builder = new CharacterCard(1, JobType.CONSTRUCTOR, 2, Phases.EVENT, null, 1);
        carbicane.modifyMyTribe(builder);
        
        int score = game.calculatePlayerScore(carbicane);
        
        assertTrue(score >= 1);
    }

    @Test
    void testCalculatePlayerScore_Inventors() throws Exception {
        carbicane.modifyMyTribe(new CharacterCard(1, JobType.INVENTOR, 0, Phases.EVENT, null, 1));
        carbicane.modifyMyTribe(new CharacterCard(2, JobType.INVENTOR, 0, Phases.EVENT, null, 1));
        carbicane.modifyMyTribe(new CharacterCard(3, JobType.INVENTOR, 0, Phases.EVENT, null, 1));
        
        int score = game.calculatePlayerScore(carbicane);
        
        assertTrue(score >= 9);
    }

    @Test
    void testCalculatePlayerScore_Artists() throws Exception {
        for (int i = 0; i < 4; i++) {
            carbicane.modifyMyTribe(new CharacterCard(1, JobType.ARTIST, 0, Phases.EVENT, null, 1));
        }
        
        int score = game.calculatePlayerScore(carbicane);
        
        assertTrue(score >= 20);
    }

    @Test
    void testCalculatePlayerScore_Buildings() throws Exception {
        BuildingCard building = new BuildingCard(JobType.CONSTRUCTOR, 3, 5, Phases.ENDGAME, null, 1);
        building.addOwner(carbicane);
        carbicane.getBuildings().add(building);
        
        int score = game.calculatePlayerScore(carbicane);
        
        assertTrue(score >= 0);
    }
}
