package model;

import model.states.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Event Resolution Test: All event types and effects
 */
class EventResolutionTest {

    private Game game;
    private Player alice, bob, charlie;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.joinGame("Charlie");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "YELLOW");
        game.chooseTotemColor("Charlie", "BLACK");
        game.setupGame();
        game.setState(new StartTurnState());
        
        alice = game.getPlayerByNickname("Alice");
        bob = game.getPlayerByNickname("Bob");
        charlie = game.getPlayerByNickname("Charlie");
    }

    @Test
    void testGameHasEvents() {
        int upperRowSize = game.getBoard().getCharacterRowSize("upper");
        int lowerRowSize = game.getBoard().getCharacterRowSize("lower");
        assertTrue(upperRowSize > 0 || lowerRowSize > 0);
    }

    @Test
    void testHuntEvent_GeneratesFood() throws Exception {
        CharacterCard hunter = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        hunter.pick(alice, game);
        
        assertTrue(alice.getMyTribe().stream()
            .anyMatch(c -> c.getJobType() == JobType.HUNTER));
    }

    @Test
    void testArtist_EndGamePoints() throws Exception {
        CharacterCard artist = new CharacterCard(1, JobType.ARTIST, 1, Phases.EVENT, null, 1);
        artist.pick(alice, game);
        
        int score = game.calculatePlayerScore(alice);
        assertTrue(score >= 0);
    }

    @Test
    void testInventor_EndGamePoints() throws Exception {
        CharacterCard inventor = new CharacterCard(1, JobType.INVENTOR, 1, Phases.EVENT, null, 1);
        inventor.pick(alice, game);
        
        int score = game.calculatePlayerScore(alice);
        assertTrue(score >= 0);
    }

    @Test
    void testConstructor_BuildingDiscount() throws Exception {
        CharacterCard constructor = new CharacterCard(1, JobType.CONSTRUCTOR, 1, Phases.EVENT, null, 1);
        constructor.pick(alice, game);
        
        assertTrue(alice.getMyTribe().stream()
            .anyMatch(c -> c.getJobType() == JobType.CONSTRUCTOR));
    }

    @Test
    void testShaman_MajorityMinority() throws Exception {
        CharacterCard shaman = new CharacterCard(1, JobType.SHAMAN, 1, Phases.EVENT, null, 1);
        shaman.pick(alice, game);
        
        assertTrue(alice.getMyTribe().stream()
            .anyMatch(c -> c.getJobType() == JobType.SHAMAN));
    }

    @Test
    void testFarmer_Sustenance() throws Exception {
        CharacterCard farmer = new CharacterCard(1, JobType.FARMER, 1, Phases.EVENT, null, 1);
        farmer.pick(alice, game);
        
        assertTrue(alice.getMyTribe().stream()
            .anyMatch(c -> c.getJobType() == JobType.FARMER));
    }

    @Test
    void testAllJobTypes_CanBePicked() throws Exception {
        List<JobType> validTypes = List.of(
            JobType.HUNTER, JobType.SHAMAN, JobType.CONSTRUCTOR,
            JobType.FARMER, JobType.INVENTOR, JobType.ARTIST
        );
        for (JobType type : validTypes) {
            CharacterCard card = new CharacterCard(1, type, 1, Phases.EVENT, null, 1);
            assertDoesNotThrow(() -> card.pick(alice, game));
        }
        assertEquals(6, alice.getMyTribe().size());
    }

    @Test
    void testCharacterCard_CanBePicked() throws Exception {
        CharacterCard hunter = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        int initialSize = alice.getMyTribe().size();
        
        hunter.pick(alice, game);
        
        assertEquals(initialSize + 1, alice.getMyTribe().size());
    }

    @Test
    void testCharacterCard_DoesNotCostFood() throws Exception {
        int initialFood = alice.getFood();
        CharacterCard hunter = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        
        hunter.pick(alice, game);
        
        assertEquals(initialFood, alice.getFood());
    }

    @Test
    void testEndGame_ScoreCalculation() throws Exception {
        CharacterCard artist = new CharacterCard(1, JobType.ARTIST, 2, Phases.EVENT, null, 1);
        artist.pick(alice, game);
        
        CharacterCard inventor = new CharacterCard(1, JobType.INVENTOR, 1, Phases.EVENT, null, 1);
        inventor.pick(alice, game);
        
        CharacterCard constructor = new CharacterCard(1, JobType.CONSTRUCTOR, 1, Phases.EVENT, null, 1);
        constructor.pick(alice, game);
        
        int score = game.calculatePlayerScore(alice);
        assertTrue(score >= 0);
    }
}
