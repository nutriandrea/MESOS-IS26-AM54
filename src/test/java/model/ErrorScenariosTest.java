package model;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.TotemColors;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Error Scenarios Test: Comprehensive error handling tests
 */
class ErrorScenariosTest {

    @Test
    void testError_DuplicateNickname() {
        Game game = new Game();
        assertThrows(Exception.class, () -> {
            game.joinGame("Alice");
            game.joinGame("Alice");
        });
    }

    @Test
    void testError_DuplicateTotemColor() {
        assertThrows(Exception.class, () -> {
            Game game = new Game();
            game.joinGame("Player1");
            game.joinGame("Player2");
            game.joinGame("Player3");
            game.chooseTotemColor("Player1", "RED");
            game.chooseTotemColor("Player2", "RED");
        });
    }

    @Test
    void testError_InvalidTotemColor() {
        assertThrows(Exception.class, () -> {
            Game game = new Game();
            game.joinGame("Player1");
            game.joinGame("Player2");
            game.chooseTotemColor("Player1", "INVALID_COLOR");
        });
    }

    @Test
    void testError_InvalidTotemColor_Green() {
        assertThrows(Exception.class, () -> {
            Game game = new Game();
            game.joinGame("Player1");
            game.joinGame("Player2");
            game.chooseTotemColor("Player1", "GREEN");
        });
    }

    @Test
    void testError_InvalidTotemColor_Purple() {
        assertThrows(Exception.class, () -> {
            Game game = new Game();
            game.joinGame("Player1");
            game.joinGame("Player2");
            game.chooseTotemColor("Player1", "PURPLE");
        });
    }

    @Test
    void testError_PlayerNotFound() {
        assertThrows(Exception.class, () -> {
            Game game = new Game();
            game.getPlayerByNickname("Nobody");
        });
    }

    @Test
    void testError_TooManyPlayers() {
        assertThrows(Exception.class, () -> {
            Game game = new Game();
            game.joinGame("P1");
            game.joinGame("P2");
            game.joinGame("P3");
            game.joinGame("P4");
            game.joinGame("P5");
            game.joinGame("P6");
        });
    }

    @Test
    void testValidTotemColors() throws Exception {
        Game game = new Game();
        game.joinGame("P1");
        game.joinGame("P2");
        
        assertDoesNotThrow(() -> {
            game.chooseTotemColor("P1", "RED");
            game.chooseTotemColor("P2", "YELLOW");
        });
        
        assertEquals(TotemColors.RED, game.getPlayerByNickname("P1").getTotemColors());
        assertEquals(TotemColors.YELLOW, game.getPlayerByNickname("P2").getTotemColors());
    }

    @Test
    void testValidTotemColors_BlackWhiteBlue() throws Exception {
        Game game = new Game();
        game.joinGame("P1");
        game.joinGame("P2");
        
        assertDoesNotThrow(() -> {
            game.chooseTotemColor("P1", "BLACK");
            game.chooseTotemColor("P2", "WHITE");
        });
        
        assertEquals(TotemColors.BLACK, game.getPlayerByNickname("P1").getTotemColors());
        assertEquals(TotemColors.WHITE, game.getPlayerByNickname("P2").getTotemColors());
    }
}
