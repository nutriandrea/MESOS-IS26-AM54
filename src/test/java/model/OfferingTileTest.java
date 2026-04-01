package model;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.OfferingTile;
import it.polimi.ingsw.am54.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OfferingTileTest {

    private OfferingTile tile;
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
        tile = new OfferingTile('A', 2, 1, 3);
    }

    @Test
    void testOfferingTile_GetId() {
        assertEquals('A', tile.getId());
    }

    @Test
    void testOfferingTile_GetFoodBonus() {
        assertEquals(3, tile.getFoodBonus());
    }

    @Test
    void testOfferingTile_GetOccupiedBy_InitiallyNull() {
        assertNull(tile.getOccupiedBy());
    }

    @Test
    void testOfferingTile_IsAvailable_Initially() {
        assertTrue(tile.isAvailable());
    }

    @Test
    void testOfferingTile_Occupy() {
        tile.occupy(player);
        
        assertEquals(player, tile.getOccupiedBy());
        assertFalse(tile.isAvailable());
    }

    @Test
    void testOfferingTile_Occupy_SetsRemainingPicks() {
        tile.occupy(player);
        
        assertEquals(2, tile.getRemainingUpperPicks());
        assertEquals(1, tile.getRemainingLowerPicks());
    }

    @Test
    void testOfferingTile_FreeSlot() {
        tile.occupy(player);
        assertFalse(tile.isAvailable());
        
        tile.freeSlot();
        assertNull(tile.getOccupiedBy());
        assertTrue(tile.isAvailable());
    }

    @Test
    void testOfferingTile_UseUpperPick() {
        tile.occupy(player);
        int initialPicks = tile.getRemainingUpperPicks();
        
        boolean result = tile.useUpperPick();
        
        assertTrue(result);
        assertEquals(initialPicks - 1, tile.getRemainingUpperPicks());
    }

    @Test
    void testOfferingTile_UseUpperPick_NoPicksLeft() {
        tile.occupy(player);
        // Use all picks
        while (tile.getRemainingUpperPicks() > 0) {
            tile.useUpperPick();
        }
        
        boolean result = tile.useUpperPick();
        
        assertFalse(result);
    }

    @Test
    void testOfferingTile_UseLowerPick() {
        tile.occupy(player);
        int initialPicks = tile.getRemainingLowerPicks();
        
        boolean result = tile.useLowerPick();
        
        assertTrue(result);
        assertEquals(initialPicks - 1, tile.getRemainingLowerPicks());
    }

    @Test
    void testOfferingTile_UseLowerPick_NoPicksLeft() {
        tile.occupy(player);
        // Use all picks
        while (tile.getRemainingLowerPicks() > 0) {
            tile.useLowerPick();
        }
        
        boolean result = tile.useLowerPick();
        
        assertFalse(result);
    }

    @Test
    void testOfferingTile_HasPicksLeft_Both() {
        tile.occupy(player);
        
        assertTrue(tile.hasPicksLeft());
    }

    @Test
    void testOfferingTile_HasPicksLeft_Neither() {
        tile.occupy(player);
        while (tile.hasPicksLeft()) {
            if (tile.getRemainingUpperPicks() > 0) tile.useUpperPick();
            if (tile.getRemainingLowerPicks() > 0) tile.useLowerPick();
        }
        
        assertFalse(tile.hasPicksLeft());
    }

    @Test
    void testOfferingTile_UseBeforeOccupy_ReturnsFalse() {
        assertFalse(tile.useUpperPick());
        assertFalse(tile.useLowerPick());
    }

    @Test
    void testOfferingTile_DifferentIds() {
        OfferingTile tileB = new OfferingTile('B', 1, 1, 0);
        
        assertEquals('A', tile.getId());
        assertEquals('B', tileB.getId());
    }
}
