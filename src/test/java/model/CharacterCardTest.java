package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharacterCardTest {

    private Game game;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("TestPlayer");
        game.joinGame("Player2");
        game.chooseTotemColor("TestPlayer", "RED");
        game.chooseTotemColor("Player2", "YELLOW");
        game.setupGame();
        
        player = game.getPlayers().get(0);
    }

    @Test
    void testCharacterCard_GetLevel() {
        CharacterCard card = new CharacterCard(2, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        assertEquals(2, card.getLevel());
    }

    @Test
    void testCharacterCard_GetJobType() {
        CharacterCard hunter = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        CharacterCard CONSTRUCTOR = new CharacterCard(1, JobType.CONSTRUCTOR, 2, Phases.EVENT, null, 1);
        CharacterCard artist = new CharacterCard(1, JobType.ARTIST, 0, Phases.EVENT, null, 1);
        
        assertEquals(JobType.HUNTER, hunter.getJobType());
        assertEquals(JobType.CONSTRUCTOR, CONSTRUCTOR.getJobType());
        assertEquals(JobType.ARTIST, artist.getJobType());
    }

    @Test
    void testCharacterCard_GetAura() {
        CharacterCard card = new CharacterCard(1, JobType.CONSTRUCTOR, 5, Phases.EVENT, null, 1);
        assertEquals(5, card.getAura());
    }

    @Test
    void testCharacterCard_GetCost_ReturnsZero() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        assertEquals(0, card.getCost());
    }

    @Test
    void testCharacterCard_GetName() {
        CharacterCard hunter = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        CharacterCard CONSTRUCTOR = new CharacterCard(1, JobType.CONSTRUCTOR, 2, Phases.EVENT, null, 1);
        
        assertEquals("HUNTER", hunter.getName());
        assertEquals("CONSTRUCTOR", CONSTRUCTOR.getName());
    }

    @Test
    void testCharacterCard_Pick_AddsToTribe() throws Exception {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        int initialSize = player.getMyTribe().size();
        
        card.pick(player, game);
        
        assertEquals(initialSize + 1, player.getMyTribe().size());
    }

    @Test
    void testCharacterCard_Pick_DoesNotCostFood() throws Exception {
        int initialFood = player.getFood();
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        
        card.pick(player, game);
        
        assertEquals(initialFood, player.getFood());
    }

    @Test
    void testCharacterCard_Era() {
        CharacterCard card = new CharacterCard(2, JobType.ARTIST, 0, Phases.EVENT, null, 2);
        assertEquals(2, card.getEra());
    }

    @Test
    void testCharacterCard_AllJobTypes() {
        for (JobType type : JobType.values()) {
            CharacterCard card = new CharacterCard(1, type, 0, Phases.EVENT, null, 1);
            assertEquals(type, card.getJobType());
        }
    }

    @Test
    void testCharacterCard_Reveal_AddsToBoard() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        Board board = game.getBoard();
        int initialUpperSize = board.getCharacterRowSize("upper");
        
        card.reveal(board, "upper");
        
        assertEquals(initialUpperSize + 1, board.getCharacterRowSize("upper"));
    }

    @Test
    void testCharacterCard_Reveal_LowerRow() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        Board board = game.getBoard();
        int initialLowerSize = board.getCharacterRowSize("lower");
        
        card.reveal(board, "lower");
        
        assertEquals(initialLowerSize + 1, board.getCharacterRowSize("lower"));
    }
}
