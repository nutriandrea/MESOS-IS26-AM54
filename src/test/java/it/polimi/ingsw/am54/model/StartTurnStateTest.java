package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test deterministici di StartTurnState.placeTotem: flusso felice, transizione a
 * PickingState quando tutti hanno piazzato, e validazioni di errore.
 */
public class StartTurnStateTest {

    private Game game;
    private Board board;
    private Player p1, p2;

    @BeforeEach
    public void setUp() {
        game = new Game();
        board = new Board();
        game.setBoard(board);
        game.setActivePlayerCount(2);

        p1 = new Player(); p1.setNickname("Alice");
        p2 = new Player(); p2.setNickname("Bob");
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        board.getOfferingTrack().add(new OfferingTile('A', 1, 1, 0));
        board.getOfferingTrack().add(new OfferingTile('B', 1, 1, 0));
        TurnOrderSlot s1 = new TurnOrderSlot(1, 0); s1.occupy(p1);
        TurnOrderSlot s2 = new TurnOrderSlot(2, 0); s2.occupy(p2);
        board.getTurnOrderTile().add(s1);
        board.getTurnOrderTile().add(s2);

        game.setState(new StartTurnState());
        game.setCurrentPlayer(p1);
    }

    @Test
    public void testPlaceTotemAdvancesThenTransitionsToPicking() throws Exception {
        game.placeTotem("Alice", 0);
        assertTrue(p1.hasPlacedTotem());
        assertFalse(board.getOfferingTrack().get(0).isAvailable());
        assertEquals(p2, game.getCurrentPlayer(), "Il turno passa a Bob");
        assertTrue(game.getCurrentState() instanceof StartTurnState, "Non ancora tutti piazzati");

        game.placeTotem("Bob", 1);
        assertTrue(game.getCurrentState() instanceof PickingState,
                "Con tutti i totem piazzati si entra in PickingState");
        assertNotNull(game.getCurrentPlayer(), "Deve esserci un primo picker");
    }

    @Test
    public void testPlaceTotemErrors() throws Exception {
        // Non è il turno di Bob
        assertThrows(Exception.class, () -> game.placeTotem("Bob", 1));

        // Posizione non valida
        assertThrows(Exception.class, () -> game.placeTotem("Alice", 99));
        assertThrows(Exception.class, () -> game.placeTotem("Alice", -1));

        // Piazza correttamente, poi riprova: ha già piazzato
        game.placeTotem("Alice", 0);
        game.setCurrentPlayer(p1); // forziamo di nuovo Alice per testare il blocco
        assertThrows(Exception.class, () -> game.placeTotem("Alice", 1),
                "Non può piazzare due volte nello stesso round");
    }

    @Test
    public void testPlaceTotemOnOccupiedTileFails() throws Exception {
        game.placeTotem("Alice", 0); // Alice occupa la tessera 0
        // Bob prova a occupare la stessa tessera
        assertThrows(Exception.class, () -> game.placeTotem("Bob", 0));
    }

    @Test
    public void testHandleDisconnectionWithPlacedTotemFreesTile() throws Exception {
        game.placeTotem("Alice", 0);
        // Alice si disconnette dopo aver piazzato: la tessera deve liberarsi
        StartTurnState st = (StartTurnState) (game.getCurrentState() instanceof StartTurnState
                ? game.getCurrentState() : new StartTurnState());
        st.handleDisconnection(game, p1, false);
        assertNull(p1.getCurrentTile(), "La tessera del disconnesso deve essere liberata");
    }
}
