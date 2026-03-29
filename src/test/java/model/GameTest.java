package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import model.states.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Game game;
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
        alice = game.getPlayers().get(0);
        bob = game.getPlayers().get(1);
    }

    // ==================== Basic Tests ====================

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
        assertEquals("Alice", players.get(0).getNickname());
        assertEquals("Bob", players.get(1).getNickname());
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

    // ==================== Era/Round Tests ====================

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
    void testGetCurrentEra() {
        assertEquals(1, game.getCurrentEra());
    }

    @Test
    void testGetRoundCount() {
        assertEquals(0, game.getRoundCount());
    }

    // ==================== Player Tests ====================

    @Test
    void testGetPlayersNumber() {
        assertEquals(2, game.getPlayersNumber());
    }

    @Test
    void testGetPlayerByNickname() throws Exception {
        Player found = game.getPlayerByNickname("Alice");
        assertEquals(alice, found);
    }

    // ==================== Current Player Tests ====================

    @Test
    void testSetCurrentPlayer() {
        game.setCurrentPlayer(bob);
        assertEquals(bob, game.getCurrentPlayer());
    }

    @Test
    void testNextPlayer() {
        game.setCurrentPlayer(alice);
        Player first = game.getCurrentPlayer();
        game.nextPlayer();
        Player second = game.getCurrentPlayer();
        assertNotNull(second);
    }

    // ==================== State Tests ====================

    @Test
    void testGetCurrentState() {
        assertNotNull(game.getCurrentState());
    }

    @Test
    void testSetState() {
        game.setState(new PickingState());
        assertTrue(game.getCurrentState() instanceof PickingState);
    }

    // ==================== Deck Tests ====================

    @Test
    void testIsTribeDeckEmpty() {
        boolean empty = game.isTribeDeckEmpty();
        assertFalse(empty);
    }

    // ==================== Turn Order Tests ====================

    @Test
    void testIsPickingComplete() {
        boolean complete = game.isPickingComplete();
        assertTrue(complete);
    }

    // ==================== Score Tests ====================

    @Test
    void testCalculatePlayerScore() {
        int score = game.calculatePlayerScore(alice);
        assertTrue(score >= 0);
    }

    @Test
    void testDetermineWinners() {
        List<Player> winners = game.determineWinners();
        assertNotNull(winners);
        assertFalse(winners.isEmpty());
    }
}
