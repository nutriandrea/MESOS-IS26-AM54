package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import model.states.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game();
        
        try {
            game.joinGame("carbicane");
            game.joinGame("frabbo");
            game.chooseTotemColor("carbicane", "RED");
            game.chooseTotemColor("frabbo", "YELLOW");
            game.setupGame();
        } catch (Exception e) {
            fail("Setup should not throw: " + e.getMessage());
        }
    }

    @Test
    void testGameInitialization() {
        assertEquals(2, game.getPlayersNumber());
        assertEquals(1, game.getCurrentEra());
        assertEquals(0, game.getRoundCount());
    }

    @Test
    void testPlayersExist() {
        List<Player> players = game.getPlayers();
        assertEquals(2, players.size());
        assertEquals("carbicane", players.get(0).getNickname());
        assertEquals("frabbo", players.get(1).getNickname());
    }

    @Test
    void testBoardExists() {
        assertNotNull(game.getBoard());
    }

    @Test
    void testOfferingTilesExist() {
        List<OfferingTile> tiles = game.getBoard().getOfferingTrack();
        assertNotNull(tiles);
        assertFalse(tiles.isEmpty());
    }

    @Test
    void testTurnOrderSlotsExist() {
        List<TurnOrderSlot> slots = game.getBoard().getTurnOrderTile();
        assertNotNull(slots);
        assertEquals(2, slots.size());
    }

    @Test
    void testTribeDeckExists() {
        assertNotNull(game.getTribeDeck());
    }

    @Test
    void testBuildingDeckExists() {
        assertNotNull(game.getBuildingDeck());
    }

    @Test
    void testUpperAndLowerRowsExist() {
        Board board = game.getBoard();
        assertNotNull(board.getCharacterRowSize("upper"));
        assertNotNull(board.getCharacterRowSize("lower"));
    }

    @Test
    void testIsEraTransition_InitiallyFalse() {
        assertFalse(game.isEraTransition());
    }

    @Test
    void testAdvanceRound_IncrementsCount() {
        int initial = game.getRoundCount();
        game.advanceRound();
        assertEquals(initial + 1, game.getRoundCount());
    }

    @Test
    void testGetPlayersNumber() {
        assertEquals(2, game.getPlayersNumber());
    }
}
