package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Game game;
    private Board board;

    @BeforeEach
    void setUp() {
        game = new Game();
        
        try {
            game.joinGame("carbicane");
            game.joinGame("frabbo");
            game.chooseTotemColor("carbicane", "RED");
            game.chooseTotemColor("frabbo", "YELLOW");
            game.setupGame();
            board = game.getBoard();
        } catch (Exception e) {
            fail("Setup should not throw: " + e.getMessage());
        }
    }

    @Test
    void testOfferingTrackNotEmpty() {
        assertNotNull(board.getOfferingTrack());
        assertFalse(board.getOfferingTrack().isEmpty());
    }

    @Test
    void testTurnOrderTileNotEmpty() {
        assertNotNull(board.getTurnOrderTile());
        assertEquals(2, board.getTurnOrderTile().size());
    }

    @Test
    void testUpperRowHasCards() {
        int upperSize = board.getCharacterRowSize("upper");
        assertTrue(upperSize > 0);
    }

    @Test
    void testLowerRowHasCards() {
        int lowerSize = board.getCharacterRowSize("lower");
        assertTrue(lowerSize > 0);
    }

    @Test
    void testTakeCharacterCard() {
        int initialSize = board.getCharacterRowSize("upper");
        CharacterCard card = board.takeCharacterCard("upper", 0);
        
        assertNotNull(card);
        assertEquals(initialSize - 1, board.getCharacterRowSize("upper"));
    }

    @Test
    void testTakeCharacterCard_InvalidIndex() {
        CharacterCard card = board.takeCharacterCard("upper", 999);
        assertNull(card);
    }

    @Test
    void testMoveUpperToLower() {
        int upperInitialSize = board.getCharacterRowSize("upper");
        int lowerInitialSize = board.getCharacterRowSize("lower");
        
        board.moveUpperToLower();
        
        assertEquals(0, board.getCharacterRowSize("upper"));
        assertEquals(lowerInitialSize + upperInitialSize, board.getCharacterRowSize("lower"));
    }

    @Test
    void testClearLowerRow() {
        int initialSize = board.getCharacterRowSize("lower");
        assertTrue(initialSize > 0);
        
        board.clearLowerRow();
        
        assertEquals(0, board.getCharacterRowSize("lower"));
    }

    @Test
    void testResolveAllEventsInOrder() {
        // Add an event to lower row for testing
        // This test verifies the method exists and can be called
        assertDoesNotThrow(() -> board.resolveAllEventsInOrder(game));
    }

    @Test
    void testGetBuildingCardsUpperRow() {
        List<BuildingCard> upperBuildings = board.getBuildingCardsUpperRow();
        assertNotNull(upperBuildings);
    }

    @Test
    void testOfferingTileOccupation() {
        try {
            Player carbicane = game.getPlayerByNickname("carbicane");
            assertNotNull(carbicane);
            
            OfferingTile tile = board.getOfferingTrack().get(0);
            tile.occupy(carbicane);
            
            assertEquals(carbicane, tile.getOccupiedBy());
            
            tile.freeSlot();
            assertNull(tile.getOccupiedBy());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    void testTurnOrderSlotOccupation() {
        try {
            Player frabbo = game.getPlayerByNickname("frabbo");
            assertNotNull(frabbo);
            
            TurnOrderSlot slot = board.getTurnOrderTile().get(0);
            slot.occupy(frabbo);
            
            assertEquals(frabbo, slot.getOccupiedBy());
            
            slot.freeSlot();
            assertNull(slot.getOccupiedBy());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }
}
