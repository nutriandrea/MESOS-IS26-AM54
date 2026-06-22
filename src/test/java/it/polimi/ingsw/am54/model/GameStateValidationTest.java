package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.EventState;
import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test delle validazioni dello stato di setup e dei vincoli di transizione degli stati.
 */
public class GameStateValidationTest {

    private Game game;

    @BeforeEach
    public void setUp() {
        game = new Game(); // parte in SetUpGameState
    }

    // ==========================================
    // SETUP: JOIN
    // ==========================================
    @Test
    public void testJoinAddsPlayer() throws Exception {
        game.joinGame("Alice");
        assertEquals(1, game.getPlayersNumber());
    }

    @Test
    public void testJoinDuplicateNicknameFails() throws Exception {
        game.joinGame("Alice");
        Exception e = assertThrows(Exception.class, () -> game.joinGame("alice"));
        assertTrue(e.getMessage().toLowerCase().contains("uso") || e.getMessage().toLowerCase().contains("già"));
    }

    @Test
    public void testJoinFullRoomFails() throws Exception {
        game.joinGame("A"); game.joinGame("B"); game.joinGame("C");
        game.joinGame("D"); game.joinGame("E");
        assertThrows(Exception.class, () -> game.joinGame("F"), "La stanza è piena a 5 giocatori");
    }

    // ==========================================
    // SETUP: COLOR
    // ==========================================
    @Test
    public void testChooseTotemColorSuccess() throws Exception {
        game.joinGame("Alice");
        game.chooseTotemColor("Alice", "red");
        assertEquals(TotemColors.RED, game.getPlayers().get(0).getTotemColors());
    }

    @Test
    public void testChooseInvalidColorFails() throws Exception {
        game.joinGame("Alice");
        assertThrows(Exception.class, () -> game.chooseTotemColor("Alice", "ORANGE"));
    }

    @Test
    public void testChooseDuplicateColorFails() throws Exception {
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.chooseTotemColor("Alice", "BLUE");
        assertThrows(Exception.class, () -> game.chooseTotemColor("Bob", "blue"));
    }

    // ==========================================
    // SETUP: START
    // ==========================================
    @Test
    public void testSetupFailsWithLessThanTwoPlayers() throws Exception {
        game.joinGame("Alice");
        game.chooseTotemColor("Alice", "RED");
        assertThrows(Exception.class, () -> game.getCurrentState().setupGame(game));
    }

    @Test
    public void testSetupFailsIfPlayerHasNoColor() throws Exception {
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.chooseTotemColor("Alice", "RED");
        // Bob non ha colore
        assertThrows(Exception.class, () -> game.getCurrentState().setupGame(game));
    }

    @Test
    public void testSetupSucceedsAndTransitions() throws Exception {
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "BLUE");

        game.getCurrentState().setupGame(game);
        assertTrue(game.getCurrentState() instanceof StartTurnState);
    }

    // ==========================================
    // FLAGS DEGLI STATI
    // ==========================================
    @Test
    public void testStateFlags() {
        assertTrue(game.getCurrentState().isRemovePermanent(), "SetUp: rimozione permanente");
        assertTrue(game.getCurrentState().isDisconnectBlocked(), "SetUp: disconnessione bloccata");

        assertFalse(new StartTurnState().isGameOver());
        assertFalse(new PickingState().isGameOver());
        assertTrue(new EventState().isAutoResolvable(), "EventState è auto-risolvibile");
        assertTrue(new EventState().isDisconnectBlocked());

        EndGameStateFlags();
    }

    private void EndGameStateFlags() {
        var end = new it.polimi.ingsw.am54.model.states.EndGameState();
        assertTrue(end.isGameOver());
        assertTrue(end.isAutoResolvable());
        assertTrue(end.isRemovePermanent());
        assertTrue(end.isDisconnectBlocked());
        assertTrue(end.shouldResetToSetupOnEmpty());
    }

    // ==========================================
    // AZIONI NON PERMESSE NELLO STATO SBAGLIATO
    // ==========================================
    @Test
    public void testWrongPhaseActionsThrow() {
        // In SetUpGameState non si può piazzare totem, pescare o terminare il turno
        assertThrows(Exception.class, () -> game.placeTotem("X", 0));
        assertThrows(Exception.class, () -> game.pickCard("X", "upper", 0));
        assertThrows(Exception.class, () -> game.endTurn("X"));

        // StartTurnState non permette pickCard/endTurn
        StartTurnState st = new StartTurnState();
        assertThrows(Exception.class, () -> st.pickCard(game, "X", "upper", 0));
        assertThrows(Exception.class, () -> st.endTurn(game, "X"));

        // PickingState non permette placeTotem
        PickingState ps = new PickingState();
        assertThrows(Exception.class, () -> ps.placeTotem(game, "X", 0));
    }

    @Test
    public void testRecoverActivePlayerDefaultsNull() {
        // GameState base e SetUpGameState non hanno un giocatore attivo da recuperare
        assertNull(game.getCurrentState().recoverActivePlayer(game));
    }
}
