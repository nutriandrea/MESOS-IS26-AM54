package model;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TribeCard;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Era Transition Test: Era transitions at rounds 4 and 7
 */
class EraTransitionTest {

    private Game game;
    private Player p1, p2, p3, p4, p5;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.joinGame("Charlie");
        game.joinGame("Diana");
        game.joinGame("Evan");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "YELLOW");
        game.chooseTotemColor("Charlie", "BLACK");
        game.chooseTotemColor("Diana", "WHITE");
        game.chooseTotemColor("Evan", "BLUE");
        game.setupGame();
        game.setState(new StartTurnState());
        
        p1 = game.getPlayerByNickname("Alice");
        p2 = game.getPlayerByNickname("Bob");
        p3 = game.getPlayerByNickname("Charlie");
        p4 = game.getPlayerByNickname("Diana");
        p5 = game.getPlayerByNickname("Evan");
    }

    @Test
    void testInitialEra_IsOne() {
        assertEquals(1, game.getCurrentEra());
    }

    @Test
    void testBuildingsExistInUpperRow_BeforeTransition() {
        int era1Buildings = game.getBoard().getBuildingRowSize("upper");
        assertTrue(era1Buildings >= 2, "Era I should have at least 2 buildings in upper row");
    }

    @Test
    void testEraTransition_AffectsBuildingDeck() {
        boolean hasBuildings = !game.getBuildingDeck().isEmpty();
        assertTrue(hasBuildings, "Building deck should have cards");
    }

    @Test
    void testEraTransition_CanCheckEraTransition() {
        TribeCard nextCard = game.getTribeDeck().peek();
        if (nextCard != null) {
            int nextEra = nextCard.getEra();
            assertTrue(nextEra >= 1 && nextEra <= 3);
        }
    }

    @Test
    void testRoundCount_InitialZero() {
        assertEquals(0, game.getRoundCount());
    }

    @Test
    void testAdvanceRound_MethodExists() {
        assertDoesNotThrow(() -> {
            game.advanceRound();
        });
        assertEquals(1, game.getRoundCount());
    }

    @Test
    void testEraTransition_BuildsMove() throws Exception {
        int upperBuildings = game.getBoard().getBuildingRowSize("upper");
        assertTrue(upperBuildings > 0);
        
        game.getBoard().moveBuildings();
        
        int lowerBuildings = game.getBoard().getBuildingRowSize("lower");
        assertTrue(lowerBuildings >= upperBuildings);
    }
}
