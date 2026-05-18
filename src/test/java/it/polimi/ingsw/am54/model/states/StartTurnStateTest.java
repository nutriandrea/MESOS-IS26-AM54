package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StartTurnStateTest {

    private Game game;
    private Player p1;
    private Player p2;
    private StartTurnState startState;
    private Board board;

    @BeforeEach
    public void setUp() {
        game = new Game();
        board = new Board();
        game.setBoard(board);

        p1 = new Player(); p1.setNickname("Alice");
        p2 = new Player(); p2.setNickname("Bob");

        game.getPlayers().add(p1);
        game.getPlayers().add(p2);
        game.setActivePlayerCount(2);

        // Inizializziamo una board minima per 2 giocatori
        board.initializeLayout(2);

        // Mettiamo i giocatori nell'ordine di turno
        TurnOrderSlot slot1 = board.getTurnOrderTile().get(0);
        TurnOrderSlot slot2 = board.getTurnOrderTile().get(1);
        slot1.occupy(p1);
        slot2.occupy(p2);

        startState = new StartTurnState();
        game.setState(startState);

        // Impostiamo Alice come primo giocatore di turno
        game.setCurrentPlayer(p1);
    }

    // ==========================================
    // 1. TEST PLACE TOTEM: TUTTE LE ECCEZIONI
    // ==========================================
    @Test
    public void testPlaceTotem_Exceptions() throws Exception {
        // A. Non è il tuo turno
        Exception wrongTurnEx = assertThrows(Exception.class, () -> startState.placeTotem(game, "Bob", 0));
        assertTrue(wrongTurnEx.getMessage().contains("Non è il tuo turno"));

        // B. Posizione negativa
        Exception negativePosEx = assertThrows(Exception.class, () -> startState.placeTotem(game, "Alice", -1));
        assertTrue(negativePosEx.getMessage().contains("Posizione offerta non valida"));

        // C. Posizione oltre il limite
        int trackSize = board.getOfferingTrack().size();
        Exception outOfBoundsEx = assertThrows(Exception.class, () -> startState.placeTotem(game, "Alice", trackSize));
        assertTrue(outOfBoundsEx.getMessage().contains("Posizione offerta non valida"));

        // D. Tessera già occupata
        startState.placeTotem(game, "Alice", 0); // Alice prende la 0. Ora tocca a Bob.
        Exception occupiedEx = assertThrows(Exception.class, () -> startState.placeTotem(game, "Bob", 0));
        assertTrue(occupiedEx.getMessage().contains("già occupata"));

        // E. Ha già piazzato il totem (Caso forzato per coprire la riga 17)
        game.setCurrentPlayer(p2); // Tocca a Bob
        p2.setHasPlacedTotem(true); // Simuliamo che Bob lo abbia già piazzato
        Exception alreadyPlacedEx = assertThrows(Exception.class, () -> startState.placeTotem(game, "Bob", 1));
        assertTrue(alreadyPlacedEx.getMessage().contains("Hai già piazzato il tuo totem"));
    }

    // ==========================================
    // 2. TEST PLACE TOTEM: FLUSSO NORMALE E TRANSIZIONE
    // ==========================================
    @Test
    public void testPlaceTotem_SuccessAndStateTransition() throws Exception {
        // Alice piazza il totem
        startState.placeTotem(game, "Alice", 0);

        // Verifiche per Alice
        assertTrue(p1.hasPlacedTotem(), "Il flag deve essere true");
        assertNull(board.getTurnOrderTile().get(0).getOccupiedBy(), "Lo slot nell'ordine di turno deve essere stato liberato");
        assertEquals(p2, game.getCurrentPlayer(), "Il turno deve essere passato a Bob (tramite nextPlayer)");
        assertTrue(game.getCurrentState() instanceof StartTurnState, "Lo stato non deve ancora cambiare");

        // Bob piazza il totem (Ultimo giocatore attivo)
        startState.placeTotem(game, "Bob", 1);

        // Verifiche Transizione
        assertTrue(game.getCurrentState() instanceof PickingState, "Tutti hanno piazzato: lo stato deve passare a PickingState");
        assertNotNull(game.getCurrentPlayer(), "Il primo giocatore a pescare deve essere stato calcolato");
    }

    // ==========================================
    // 3. TEST SKIP TURN (Gestione Disconnessioni)
    // ==========================================
    @Test
    public void testSkipTurn_Exceptions() {
        // Non è il turno di Bob
        Exception wrongTurnEx = assertThrows(Exception.class, () -> startState.skipTurn(game, "Bob"));
        assertTrue(wrongTurnEx.getMessage().contains("Non e' il turno di"));

        // Alice ha già piazzato
        p1.setHasPlacedTotem(true);
        Exception alreadyPlacedEx = assertThrows(Exception.class, () -> startState.skipTurn(game, "Alice"));
        assertTrue(alreadyPlacedEx.getMessage().contains("ha gia' piazzato il totem"));
    }

    @Test
    public void testSkipTurn_Success() throws Exception {
        // Assicuriamoci che Alice sia nello slot 0
        TurnOrderSlot aliceSlot = board.getTurnOrderTile().get(0);
        assertEquals(p1, aliceSlot.getOccupiedBy());

        // Eseguiamo lo skipTurn per Alice
        startState.skipTurn(game, "Alice");

        // Verifiche
        assertTrue(p1.hasPlacedTotem(), "Alice deve essere marcata come se avesse piazzato il totem");
        assertNull(aliceSlot.getOccupiedBy(), "Lo slot di Alice deve essere stato liberato");
        assertEquals(p2, game.getCurrentPlayer(), "Il turno deve essere passato a Bob tramite nextPlayer()");
    }
}