package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Player class.
 */
public class PlayerTest {

    private Player player;

    @BeforeEach
    public void setUp() {
        player = new Player();
        player.setNickname("TestPlayer");
    }

    @Test
    public void testSetNickname() {
        assertEquals("TestPlayer", player.getNickname());
    }

    @Test
    public void testSetTotemColor() {
        player.setTotemColor(TotemColors.RED);
        assertEquals(TotemColors.RED, player.getTotemColors());
    }

    @Test
    public void testModifyFood() {
        player.modifyFood(5);
        assertEquals(5, player.getFood());
        player.modifyFood(-2);
        assertEquals(3, player.getFood());
        player.modifyFood(-10);
        assertEquals(0, player.getFood());
    }

    @Test
    public void testModifyAuraPoints() {
        player.modifyAuraPoints(10);
        assertEquals(10, player.getAuraPoints());
    }

    @Test
    public void testModifyMagicPoints() {
        player.modifyMagicPoints(5);
        assertEquals(5, player.getMagicPoints());
    }

    @Test
    public void testHasPlacedTotem() {
        assertFalse(player.hasPlacedTotem());
        player.setHasPlacedTotem(true);
        assertTrue(player.hasPlacedTotem());
    }
}