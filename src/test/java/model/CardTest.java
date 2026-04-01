package model;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    private Game game;
    private Player alice;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "YELLOW");
        game.setupGame();
        alice = game.getPlayers().get(0);
    }

    // ==================== Card Interface Tests ====================

    @Test
    void testCard_Pick_CharacterCard() throws Exception {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        int initialSize = alice.getMyTribe().size();
        
        card.pick(alice, game);
        
        assertEquals(initialSize + 1, alice.getMyTribe().size());
    }

    @Test
    void testCard_Pick_BuildingCard() throws Exception {
        BuildingCard card = new BuildingCard(JobType.CONSTRUCTOR, 1, 2, Phases.ENDGAME, null, 1);
        int initialSize = alice.getBuildings().size();
        
        card.pick(alice, game);
        
        assertEquals(initialSize + 1, alice.getBuildings().size());
    }

    @Test
    void testCard_Pick_DoesNotModifyFood_Character() throws Exception {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        int initialFood = alice.getFood();
        
        card.pick(alice, game);
        
        assertEquals(initialFood, alice.getFood());
    }

    @Test
    void testCharacterCard_Reveal_UpperRow() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        Board board = game.getBoard();
        int initialSize = board.getCharacterRowSize("upper");
        
        card.reveal(board, "upper");
        
        assertEquals(initialSize + 1, board.getCharacterRowSize("upper"));
    }

    @Test
    void testCharacterCard_Reveal_LowerRow() {
        CharacterCard card = new CharacterCard(1, JobType.ARTIST, 0, Phases.EVENT, null, 1);
        Board board = game.getBoard();
        int initialSize = board.getCharacterRowSize("lower");
        
        card.reveal(board, "lower");
        
        assertEquals(initialSize + 1, board.getCharacterRowSize("lower"));
    }

    @Test
    void testEventCard_Reveal() {
        EventCard card = new EventCard(1, false, null, 2, 1, 0, 0, 0, 0, 0);
        Board board = game.getBoard();
        
        assertDoesNotThrow(() -> card.reveal(board, "lower"));
    }

    @Test
    void testBuildingCard_CanBePurchased() throws Exception {
        BuildingCard card = new BuildingCard(JobType.CONSTRUCTOR, 1, 2, Phases.ENDGAME, null, 1);
        
        assertTrue(card.canBePurchased(alice));
    }

    @Test
    void testBuildingCard_CannotBePurchased_InsufficientFood() throws Exception {
        alice.modifyFood(-alice.getFood());
        BuildingCard card = new BuildingCard(JobType.CONSTRUCTOR, 100, 5, Phases.ENDGAME, null, 1);
        
        assertFalse(card.canBePurchased(alice));
    }

    @Test
    void testCharacterCard_GetEra() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 2);
        assertEquals(2, card.getEra());
    }

    @Test
    void testCard_Pick_BaseCard_ThrowsUnsupportedOperationException() {
        Card baseCard = new Card(1);
        
        Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
            baseCard.pick(alice, game);
        });
        
        assertEquals("Cannot pick this card type", exception.getMessage());
    }
}
