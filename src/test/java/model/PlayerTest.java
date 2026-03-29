package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

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

    // ==================== Basic Tests ====================

    @Test
    void testPlayer_GetNickname() {
        assertEquals("TestPlayer", player.getNickname());
    }

    @Test
    void testPlayer_GetTotemColor() {
        assertEquals(TotemColors.RED, player.getTotemColors());
    }

    // ==================== Food Tests ====================

    @Test
    void testPlayer_ModifyFood_Positive() {
        int initialFood = player.getFood();
        player.modifyFood(5);
        assertEquals(initialFood + 5, player.getFood());
    }

    @Test
    void testPlayer_ModifyFood_Negative() {
        int initialFood = player.getFood();
        player.modifyFood(-1);
        assertEquals(initialFood - 1, player.getFood());
    }

    @Test
    void testPlayer_ModifyFood_CannotGoNegative() {
        int initialFood = player.getFood();
        player.modifyFood(-initialFood - 10);
        assertEquals(0, player.getFood());
    }

    @Test
    void testPlayer_ModifyFood_Zero() {
        int initialFood = player.getFood();
        player.modifyFood(0);
        assertEquals(initialFood, player.getFood());
    }

    // ==================== Aura Tests ====================

    @Test
    void testPlayer_ModifyAuraPoints() {
        int initialAura = player.getAuraPoints();
        player.modifyAuraPoints(10);
        assertEquals(initialAura + 10, player.getAuraPoints());
    }

    @Test
    void testPlayer_ModifyAuraPoints_Negative() {
        int initialAura = player.getAuraPoints();
        player.modifyAuraPoints(-5);
        assertEquals(initialAura - 5, player.getAuraPoints());
    }

    // ==================== Magic Points Tests ====================

    @Test
    void testPlayer_ModifyMagicPoints() {
        int initialMagic = player.getMagicPoints();
        player.modifyMagicPoints(3);
        assertEquals(initialMagic + 3, player.getMagicPoints());
    }

    // ==================== Tribe Tests ====================

    @Test
    void testPlayer_ModifyMyTribe() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        int initialSize = player.getMyTribe().size();
        
        player.modifyMyTribe(card);
        
        assertEquals(initialSize + 1, player.getMyTribe().size());
    }

    @Test
    void testPlayer_ModifyMyTribe_AllJobTypes() {
        int initialSize = player.getMyTribe().size();
        JobType[] validTypes = {JobType.HUNTER, JobType.SHAMAN, JobType.CONSTRUCTOR, 
                                JobType.FARMER, JobType.INVENTOR, JobType.ARTIST};
        for (JobType type : validTypes) {
            CharacterCard card = new CharacterCard(1, type, 0, Phases.EVENT, null, 1);
            player.modifyMyTribe(card);
        }
        assertEquals(initialSize + validTypes.length, player.getMyTribe().size());
    }

    // ==================== Buildings Tests ====================

    @Test
    void testPlayer_GetBuildings() {
        assertNotNull(player.getBuildings());
    }

    @Test
    void testPlayer_ModifyMyBuildingDiscount() {
        int initialDiscount = player.getMyBuildingDiscount();
        player.modifyMyBuildingDiscount(2);
        assertEquals(initialDiscount + 2, player.getMyBuildingDiscount());
    }

    // ==================== Event Cost Tests ====================

    @Test
    void testPlayer_SetEventCostFood() {
        player.setEventCostFood(5);
        assertEquals(5, player.getEventCostFood());
    }

    @Test
    void testPlayer_SetEventCostAura() {
        player.setEventCostAura(3);
        assertEquals(3, player.getEventCostAura());
    }

    @Test
    void testPlayer_ResetEventAttributes() {
        player.setEventCostFood(5);
        player.setEventCostAura(3);
        
        player.resetEventAttributes();
        
        assertEquals(0, player.getEventCostFood());
        assertEquals(0, player.getEventCostAura());
    }

    // ==================== Tile Tests ====================

    @Test
    void testPlayer_GetCurrentTile() {
        assertNull(player.getCurrentTile());
    }

    @Test
    void testPlayer_SetCurrentTile() {
        OfferingTile tile = game.getBoard().getOfferingTrack().get(0);
        player.setCurrentTile(tile);
        assertEquals(tile, player.getCurrentTile());
    }

    // ==================== Totem Tests ====================

    @Test
    void testPlayer_HasPlacedTotem_InitiallyFalse() {
        assertFalse(player.hasPlacedTotem());
    }

    @Test
    void testPlayer_SetHasPlacedTotem() {
        player.setHasPlacedTotem(true);
        assertTrue(player.hasPlacedTotem());
    }

    // ==================== Food Discount Tests ====================

    @Test
    void testPlayer_ModifyMyFoodDiscount() {
        int initial = player.getMyFoodDiscount();
        player.modifyMyFoodDiscount(1);
        assertEquals(initial + 1, player.getMyFoodDiscount());
    }

    // ==================== Initial Food Tests ====================

    @Test
    void testPlayer_InitialFoodAmounts() throws Exception {
        Game game2 = new Game();
        game2.joinGame("First");
        game2.joinGame("Second");
        game2.joinGame("Third");
        game2.chooseTotemColor("First", "YELLOW");
        game2.chooseTotemColor("Second", "BLACK");
        game2.chooseTotemColor("Third", "WHITE");
        game2.setupGame();
        
        Player first = game2.getPlayers().get(0);
        Player second = game2.getPlayers().get(1);
        Player third = game2.getPlayers().get(2);
        
        assertTrue(first.getFood() >= 2);
        assertTrue(second.getFood() >= 3);
        assertTrue(third.getFood() >= 3);
    }
}
