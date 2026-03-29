package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Game game;
    private Board board;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "YELLOW");
        game.setupGame();
        board = game.getBoard();
        alice = game.getPlayers().get(0);
        bob = game.getPlayers().get(1);
    }

    // ==================== Basic Tests ====================

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
    void testGetBuildingCardsUpperRow() {
        List<BuildingCard> upperBuildings = board.getBuildingCardsUpperRow();
        assertNotNull(upperBuildings);
    }

    @Test
    void testGetBuildingCardsLowerRow() {
        List<BuildingCard> lowerBuildings = board.getBuildingCardsLowerRow();
        assertNotNull(lowerBuildings);
    }

    // ==================== Character Card Tests ====================

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
    void testTakeCharacterCard_LowerRow() {
        int initialSize = board.getCharacterRowSize("lower");
        CharacterCard card = board.takeCharacterCard("lower", 0);
        
        assertNotNull(card);
    }

    @Test
    void testAddCharacterCard_UpperRow() {
        int initialSize = board.getCharacterRowSize("upper");
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        
        board.addCharacterCard(card, "upper");
        
        assertEquals(initialSize + 1, board.getCharacterRowSize("upper"));
    }

    @Test
    void testAddCharacterCard_LowerRow() {
        int initialSize = board.getCharacterRowSize("lower");
        CharacterCard card = new CharacterCard(1, JobType.ARTIST, 0, Phases.EVENT, null, 1);
        
        board.addCharacterCard(card, "lower");
        
        assertEquals(initialSize + 1, board.getCharacterRowSize("lower"));
    }

    // ==================== Building Card Tests ====================

    @Test
    void testTakeBuildingCard_Success() {
        int initialUpperSize = board.getBuildingRowSize("upper");
        BuildingCard card = board.takeBuildingCard("upper", 0);
        
        if (initialUpperSize > 0) {
            assertNotNull(card);
        }
    }

    @Test
    void testTakeBuildingCard_InvalidIndex() {
        BuildingCard card = board.takeBuildingCard("upper", 999);
        assertNull(card);
    }

    @Test
    void testGetBuildingRowSize_Upper() {
        int size = board.getBuildingRowSize("upper");
        assertTrue(size >= 0);
    }

    @Test
    void testGetBuildingRowSize_Lower() {
        int size = board.getBuildingRowSize("lower");
        assertTrue(size >= 0);
    }

    // ==================== Event Card Tests ====================

    @Test
    void testAddEventCard() {
        EventCard eventCard = new EventCard(1, false, null, 2, 1, 0, 0, 0, 0, 0);
        
        assertDoesNotThrow(() -> board.addEventCard(eventCard, "lower"));
    }

    // ==================== Row Management Tests ====================

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
    void testRefillUpperRow() {
        board.clearLowerRow();
        board.moveUpperToLower();
        
        int sizeBefore = board.getCharacterRowSize("upper");
        board.refillUpperRow(game);
        
        assertTrue(board.getCharacterRowSize("upper") >= sizeBefore);
    }

    // ==================== Event Resolution Tests ====================

    @Test
    void testResolveAllEventsInOrder() {
        assertDoesNotThrow(() -> board.resolveAllEventsInOrder(game));
    }

    @Test
    void testResolveAllVisibleEvents() {
        assertDoesNotThrow(() -> board.resolveAllVisibleEvents(game));
    }

    // ==================== Offering Tile Tests ====================

    @Test
    void testOfferingTileOccupation() {
        OfferingTile tile = board.getOfferingTrack().get(0);
        tile.occupy(alice);
        
        assertEquals(alice, tile.getOccupiedBy());
        
        tile.freeSlot();
        assertNull(tile.getOccupiedBy());
    }

    @Test
    void testAddTotemToPosition() {
        OfferingTile tile = board.getOfferingTrack().get(0);
        
        assertDoesNotThrow(() -> board.addTotemToPosition(alice, tile));
    }

    // ==================== Turn Order Tests ====================

    @Test
    void testTurnOrderSlotOccupation() {
        TurnOrderSlot slot = board.getTurnOrderTile().get(0);
        slot.occupy(alice);
        
        assertEquals(alice, slot.getOccupiedBy());
        
        slot.freeSlot();
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void testTurnOrderSlotBonusFood() {
        TurnOrderSlot slot = board.getTurnOrderTile().get(0);
        assertTrue(slot.getBonusFood() >= 0);
    }

    // ==================== Row Access Tests ====================

    @Test
    void testGetRow_Upper() {
        List<?> row = board.getRow("upper");
        assertNotNull(row);
    }

    @Test
    void testGetRow_Lower() {
        List<?> row = board.getRow("lower");
        assertNotNull(row);
    }

    @Test
    void testGetCharacterRowSize() {
        assertTrue(board.getCharacterRowSize("upper") > 0);
        assertTrue(board.getCharacterRowSize("lower") >= 0);
    }

    // ==================== Move Buildings Tests ====================

    @Test
    void testMoveBuildings() {
        assertDoesNotThrow(() -> board.moveBuildings());
    }

    @Test
    void testPopulateUpperRowBuildings() {
        assertDoesNotThrow(() -> board.populateUpperRowBuildings(game));
    }
}
