package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.EndGameState;
import it.polimi.ingsw.am54.model.states.EventState;
import it.polimi.ingsw.am54.model.states.PickingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test deterministici dei rami di PickingState: pesca personaggio/edificio,
 * extra pick, validazioni, fine turno obbligatoria e transizione a EndGameState.
 */
public class PickingStateCoverageTest {

    private Game game;
    private Board board;
    private Player p1;

    @BeforeEach
    public void setUp() {
        game = new Game();
        board = new Board();
        game.setBoard(board);
        game.setActivePlayerCount(1);
        game.setCurrentEra(1);
        game.setTribeDeck(new Deck<>(new Stack<>())); // mazzo vuoto di default

        p1 = new Player(); p1.setNickname("Alice");
        game.getPlayers().add(p1);

        // Un turn order con uno slot libero per il rientro dal pick
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 0));
        game.setState(new PickingState());
        game.setCurrentPlayer(p1);
    }

    private CharacterCard ch() {
        return new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1);
    }

    private BuildingCard bld(int cost) {
        return new BuildingCard(JobType.HUNTER, cost, 0, null, new ArrayList<>(), 1);
    }

    @Test
    public void testPickCharacterThenTransitionsToEvent() throws Exception {
        OfferingTile tile = new OfferingTile('A', 1, 0, 0);
        tile.occupy(p1); p1.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        board.addCharacterCard(ch(), "upper");

        game.pickCard("Alice", "upper", 0);

        assertEquals(1, p1.getMyTribe().size(), "Il personaggio deve entrare in tribù");
        // Pick esaurito -> auto fine turno -> mazzo non in era 3 -> EventState
        assertTrue(game.getCurrentState() instanceof EventState,
                "A pick completato (era 1) si passa a EventState");
    }

    @Test
    public void testPickBuildingConsumesFood() throws Exception {
        p1.modifyFood(5);
        OfferingTile tile = new OfferingTile('A', 1, 0, 0);
        tile.occupy(p1); p1.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        board.addBuildingCard(bld(2), "upper"); // index 0 = edificio (0 personaggi)

        game.pickCard("Alice", "upper", 0);

        assertEquals(3, p1.getFood(), "Deve scalare il costo dell'edificio (5 - 2)");
        assertEquals(1, p1.getBuildings().size());
    }

    @Test
    public void testExtraPickFromUpperWithoutTile() throws Exception {
        p1.setExtraPick(true);
        p1.setCurrentTile(null); // nessuna tessera attiva
        board.addCharacterCard(ch(), "upper");

        game.pickCard("Alice", "upper", 0);

        assertEquals(1, p1.getMyTribe().size());
        assertFalse(p1.hasExtraPick(), "L'extra pick deve essere consumato");
    }

    @Test
    public void testExtraPickOnLowerIsRejected() {
        p1.setExtraPick(true);
        p1.setCurrentTile(null);
        board.addCharacterCard(ch(), "lower");
        Exception e = assertThrows(Exception.class, () -> game.pickCard("Alice", "lower", 0));
        assertTrue(e.getMessage().toLowerCase().contains("extra"));
    }

    @Test
    public void testInvalidRowAndIndexAndTurn() {
        OfferingTile tile = new OfferingTile('A', 1, 1, 0);
        tile.occupy(p1); p1.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        board.addCharacterCard(ch(), "upper");

        assertThrows(Exception.class, () -> game.pickCard("Alice", "middle", 0), "Riga non valida");
        assertThrows(Exception.class, () -> game.pickCard("Alice", "upper", 99), "Indice non valido");
        assertThrows(Exception.class, () -> game.pickCard("Bob", "upper", 0), "Non è il turno di Bob");
    }

    @Test
    public void testCannotAffordBuilding() {
        // food 0, edificio costoso
        OfferingTile tile = new OfferingTile('A', 1, 0, 0);
        tile.occupy(p1); p1.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        board.addBuildingCard(bld(5), "upper");

        Exception e = assertThrows(Exception.class, () -> game.pickCard("Alice", "upper", 0));
        assertTrue(e.getMessage().toLowerCase().contains("cibo"));
    }

    @Test
    public void testEndTurnBlockedWhenMandatoryCharactersLeft() {
        OfferingTile tile = new OfferingTile('A', 2, 0, 0); // 2 pick upper
        tile.occupy(p1); p1.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        board.addCharacterCard(ch(), "upper");
        board.addCharacterCard(ch(), "upper");

        Exception e = assertThrows(Exception.class, () -> game.endTurn("Alice"));
        assertTrue(e.getMessage().toLowerCase().contains("personaggi"),
                "Non si può chiudere il turno con pick obbligatori rimasti");
    }

    @Test
    public void testEndTurnAllowedForTesterBypass() throws Exception {
        Player tester = new Player(); tester.setNickname("tester");
        game.getPlayers().add(tester);
        OfferingTile tile = new OfferingTile('A', 2, 0, 0);
        tile.occupy(tester); tester.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        board.addCharacterCard(ch(), "upper");
        game.setCurrentPlayer(tester);

        // "tester" può forzare la chiusura anche con pick obbligatori
        assertDoesNotThrow(() -> game.endTurn("tester"));
    }

    @Test
    public void testReachesEndGameStateWhenDeckEmptyInEra3() throws Exception {
        game.setCurrentEra(3);
        game.setTribeDeck(new Deck<>(new Stack<>())); // mazzo vuoto

        OfferingTile tile = new OfferingTile('A', 1, 0, 0);
        tile.occupy(p1); p1.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        board.addCharacterCard(ch(), "upper");

        game.pickCard("Alice", "upper", 0);

        assertTrue(game.getCurrentState() instanceof EndGameState,
                "Mazzo vuoto + Era 3 + pick completato -> EndGameState");
    }
}
