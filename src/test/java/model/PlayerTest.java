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

    @Test
    void testPlayer_GetNickname() {
        assertEquals("TestPlayer", player.getNickname());
    }

    @Test
    void testPlayer_ModifyFood() {
        int initialFood = player.getFood();
        
        player.modifyFood(5);
        assertEquals(initialFood + 5, player.getFood());
        
        player.modifyFood(-3);
        assertEquals(initialFood + 2, player.getFood());
    }

    @Test
    void testPlayer_ModifyFood_CannotGoNegative() {
        int initialFood = player.getFood();
        player.modifyFood(-initialFood - 10);
        assertEquals(0, player.getFood());
    }

    @Test
    void testPlayer_ModifyAuraPoints() {
        int initialAura = player.getAuraPoints();
        
        player.modifyAuraPoints(10);
        assertEquals(initialAura + 10, player.getAuraPoints());
    }

    @Test
    void testPlayer_ModifyMagicPoints() {
        int initialMagic = player.getMagicPoints();
        
        player.modifyMagicPoints(3);
        assertEquals(initialMagic + 3, player.getMagicPoints());
    }

    @Test
    void testPlayer_GetMyTribe() {
        assertNotNull(player.getMyTribe());
    }

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

    @Test
    void testPlayer_ModifyMyTribe() {
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        int initialSize = player.getMyTribe().size();
        
        player.modifyMyTribe(card);
        
        assertEquals(initialSize + 1, player.getMyTribe().size());
    }

    @Test
    void testPlayer_SetEventCostFood() {
        player.setEventCostFood(5);
        assertEquals(5, player.getEventCostFood());
    }

    @Test
    void testPlayer_GetEventCostFood() {
        assertEquals(0, player.getEventCostFood());
    }

    @Test
    void testPlayer_SetEventCostAura() {
        player.setEventCostAura(3);
        assertEquals(3, player.getEventCostAura());
    }

    @Test
    void testPlayer_GetEventCostAura() {
        assertEquals(0, player.getEventCostAura());
    }

    @Test
    void testPlayer_ResetEventAttributes() {
        player.setEventCostFood(5);
        player.setEventCostAura(3);
        
        player.resetEventAttributes();
        
        assertEquals(0, player.getEventCostFood());
        assertEquals(0, player.getEventCostAura());
    }

    @Test
    void testPlayer_GetCurrentTile() {
        // Initially null
        assertNull(player.getCurrentTile());
    }

    @Test
    void testPlayer_SetCurrentTile() {
        OfferingTile tile = game.getBoard().getOfferingTrack().get(0);
        
        player.setCurrentTile(tile);
        
        assertEquals(tile, player.getCurrentTile());
    }

    @Test
    void testPlayer_GetTotemColor() {
        assertEquals(TotemColors.RED, player.getTotemColors());
    }

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
