package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderSlotTest {

    private TurnOrderSlot slot;
    private Player player;
    private Game game;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("TestPlayer");
        game.joinGame("Player2");
        game.chooseTotemColor("TestPlayer", "RED");
        game.chooseTotemColor("Player2", "YELLOW");
        game.setupGame();
        
        player = game.getPlayers().get(0);
        slot = new TurnOrderSlot(1, 2);
    }

    @Test
    void testTurnOrderSlot_GetPosition() {
        assertEquals(1, slot.getPosition());
    }

    @Test
    void testTurnOrderSlot_GetBonusFood() {
        assertEquals(2, slot.getBonusFood());
    }

    @Test
    void testTurnOrderSlot_GetOccupiedBy_InitiallyNull() {
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void testTurnOrderSlot_Occupy() {
        slot.occupy(player);
        assertEquals(player, slot.getOccupiedBy());
    }

    @Test
    void testTurnOrderSlot_FreeSlot() {
        slot.occupy(player);
        assertEquals(player, slot.getOccupiedBy());
        
        slot.freeSlot();
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void testTurnOrderSlot_OccupyTwice_Overwrites() throws Exception {
        Game game2 = new Game();
        game2.joinGame("Player2");
        game2.joinGame("Player3");
        game2.chooseTotemColor("Player2", "BLACK");
        game2.chooseTotemColor("Player3", "WHITE");
        game2.setupGame();
        
        Player player2 = game2.getPlayers().get(0);
        
        slot.occupy(player);
        slot.occupy(player2);
        
        assertEquals(player2, slot.getOccupiedBy());
    }

    @Test
    void testTurnOrderSlot_FreeSlot_WhenAlreadyEmpty() {
        // Should not throw
        assertDoesNotThrow(() -> slot.freeSlot());
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void testTurnOrderSlot_NegativeBonus() {
        TurnOrderSlot negativeSlot = new TurnOrderSlot(2, -1);
        assertEquals(-1, negativeSlot.getBonusFood());
    }

    @Test
    void testTurnOrderSlot_ZeroBonus() {
        TurnOrderSlot zeroSlot = new TurnOrderSlot(3, 0);
        assertEquals(0, zeroSlot.getBonusFood());
    }
}
