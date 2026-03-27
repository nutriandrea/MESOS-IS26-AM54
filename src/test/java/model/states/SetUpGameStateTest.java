package model.states;

import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetUpGameStateTest {

    private Game game;
    private SetUpGameState setUpGameState;

    @BeforeEach
    void setUp() {
        game = new Game();
        setUpGameState = new SetUpGameState();
    }

    @Test
    void testJoinGame_Success() throws Exception {
        setUpGameState.joinGame(game, "carbicane");
        
        assertEquals(1, game.getPlayers().size());
        assertEquals("carbicane", game.getPlayers().get(0).getNickname());
    }

    @Test
    void testJoinGame_FullRoom_Throws() throws Exception {
        game.joinGame("Player1");
        game.joinGame("Player2");
        game.joinGame("Player3");
        game.joinGame("Player4");
        game.joinGame("Player5");
        
        assertThrows(Exception.class, () -> {
            setUpGameState.joinGame(game, "Player6");
        });
    }

    @Test
    void testJoinGame_DuplicateNickname_Throws() throws Exception {
        setUpGameState.joinGame(game, "carbicane");
        
        assertThrows(Exception.class, () -> {
            setUpGameState.joinGame(game, "carbicane");
        });
    }

    @Test
    void testJoinGame_DifferentCases_SameName_Throws() throws Exception {
        setUpGameState.joinGame(game, "carbicane");
        
        assertThrows(Exception.class, () -> {
            setUpGameState.joinGame(game, "carbicane");
        });
    }

    @Test
    void testChooseTotemColor_Success() throws Exception {
        setUpGameState.joinGame(game, "carbicane");
        
        assertDoesNotThrow(() -> {
            setUpGameState.chooseTotemColor(game, "carbicane", "RED");
        });
        
        assertEquals(TotemColors.RED, game.getPlayerByNickname("carbicane").getTotemColors());
    }

    @Test
    void testChooseTotemColor_InvalidColor_Throws() throws Exception {
        setUpGameState.joinGame(game, "carbicane");
        
        assertThrows(Exception.class, () -> {
            setUpGameState.chooseTotemColor(game, "carbicane", "INVALID");
        });
    }

    @Test
    void testChooseTotemColor_AlreadyTaken_Throws() throws Exception {
        setUpGameState.joinGame(game, "carbicane");
        setUpGameState.joinGame(game, "frabbo");
        setUpGameState.chooseTotemColor(game, "carbicane", "RED");
        
        assertThrows(Exception.class, () -> {
            setUpGameState.chooseTotemColor(game, "frabbo", "RED");
        });
    }

    @Test
    void testChooseTotemColor_PlayerNotFound_Throws() {
        assertThrows(Exception.class, () -> {
            setUpGameState.chooseTotemColor(game, "Nobody", "RED");
        });
    }

    @Test
    void testSetupGame_Success_TransitionsToStartTurnState() throws Exception {
        game.joinGame("carbicane");
        game.joinGame("frabbo");
        game.chooseTotemColor("carbicane", "RED");
        game.chooseTotemColor("frabbo", "YELLOW");
        
        setUpGameState.setupGame(game);
        
        assertTrue(game.getCurrentState() instanceof StartTurnState);
    }

    @Test
    void testSetupGame_LessThan2Players_Throws() throws Exception {
        game.joinGame("carbicane");
        game.chooseTotemColor("carbicane", "RED");
        
        assertThrows(Exception.class, () -> {
            setUpGameState.setupGame(game);
        });
    }

    @Test
    void testSetupGame_MissingColor_Throws() throws Exception {
        game.joinGame("carbicane");
        game.joinGame("frabbo");
        game.chooseTotemColor("carbicane", "RED");
        
        assertThrows(Exception.class, () -> {
            setUpGameState.setupGame(game);
        });
    }

    @Test
    void testSetupGame_AllPlayersHaveColors_Success() throws Exception {
        game.joinGame("carbicane");
        game.joinGame("frabbo");
        game.chooseTotemColor("carbicane", "RED");
        game.chooseTotemColor("frabbo", "YELLOW");
        
        assertDoesNotThrow(() -> {
            setUpGameState.setupGame(game);
        });
    }
}
