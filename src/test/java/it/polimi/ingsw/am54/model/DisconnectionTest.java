package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import it.polimi.ingsw.am54.model.states.EventState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Stack;

public class DisconnectionTest {

    private Game game;
    private Board board;
    private Player p1;
    private Player p2;
    private Player p3;

    @BeforeEach
    public void setUp() {
        game = new Game();
        board = new Board();
        game.setBoard(board);
        game.setActivePlayerCount(3);

        p1 = new Player(); p1.setNickname("Alice");
        p2 = new Player(); p2.setNickname("Bob");
        p3 = new Player(); p3.setNickname("Charlie");

        game.getPlayers().add(p1);
        game.getPlayers().add(p2);
        game.getPlayers().add(p3);

        game.setTribeDeck(new Deck<>(new Stack<>()));
    }

    @Test
    public void testPickingStateSkipTurn_FreeTileAndGoToLastSlot() throws Exception {
        game.setState(new PickingState());

        // 4 slots for 3 players -> last slot free for disconnected player
        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 2));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, -1));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, -2));
        board.getTurnOrderTile().get(0).occupy(p1);
        board.getTurnOrderTile().get(1).occupy(p3);

        OfferingTile tile = new OfferingTile('A', 1, 1, 0);
        tile.occupy(p2);
        p2.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        game.setCurrentPlayer(p2);
        p2.modifyFood(5);

        game.getCurrentState().skipTurn(game, "Bob");

        assertNull(p2.getCurrentTile(), "La tessera deve essere liberata");
        assertTrue(tile.isAvailable(), "La tessera deve essere disponibile");

        TurnOrderSlot lastSlot = board.getTurnOrderTile().get(board.getTurnOrderTile().size() - 1);
        assertEquals(p2, lastSlot.getOccupiedBy(), "Deve essere nell'ultimo slot");

        assertEquals(3, p2.getFood(), "Deve perdere 2 cibo (malus slot4, ultimo libero dal fondo)");
    }

    @Test
    public void testPickingStateSkipTurn_LastSlotMalusWithNegativeBonus() throws Exception {
        game.setState(new PickingState());

        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 2));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, -2));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, -1));
        board.getTurnOrderTile().get(0).occupy(p1);
        board.getTurnOrderTile().get(1).occupy(p3);

        OfferingTile tile = new OfferingTile('A', 1, 1, 0);
        tile.occupy(p2);
        p2.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        game.setCurrentPlayer(p2);
        p2.modifyFood(5);

        game.getCurrentState().skipTurn(game, "Bob");

        assertEquals(4, p2.getFood(),
                "Deve perdere 1 cibo (bonus -1 dell'ultimo slot libero)");
    }

    @Test
    public void testPickingStateSkipTurn_NoFoodAuraPenalty() throws Exception {
        game.setState(new PickingState());

        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 2));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, -1));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, -2));
        board.getTurnOrderTile().get(0).occupy(p1);
        board.getTurnOrderTile().get(1).occupy(p3);

        OfferingTile tile = new OfferingTile('A', 1, 1, 0);
        tile.occupy(p2);
        p2.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);
        game.setCurrentPlayer(p2);
        p2.modifyAuraPoints(10);

        game.getCurrentState().skipTurn(game, "Bob");

        // p2 has 0 food -> fallback: lose 2 aura
        assertEquals(8, p2.getAuraPoints(),
                "Senza cibo deve perdere 2 aura (fallback del malus -1)");
    }

    @Test
    public void testPickingStateSkipTurn_AdvancesToNextPicker() throws Exception {
        game.setState(new PickingState());

        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 2));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, -1));
        board.getTurnOrderTile().get(0).occupy(p1);

        OfferingTile t1 = new OfferingTile('A', 1, 1, 0);
        OfferingTile t2 = new OfferingTile('B', 1, 1, 0);
        t1.occupy(p1); p1.setCurrentTile(t1);
        t2.occupy(p2); p2.setCurrentTile(t2);
        board.getOfferingTrack().add(t1);
        board.getOfferingTrack().add(t2);

        game.setCurrentPlayer(p1);
        p1.modifyFood(5);

        game.getCurrentState().skipTurn(game, "Alice");

        assertEquals(p2, game.getCurrentPlayer(),
                "Il prossimo picker deve essere p2");
    }

    @Test
    public void testNonCurrentPickingState_FreeTileAndGoToLastSlot() {
        // 4 slots: p1 in slot1, p3 in slot2, last slots free
        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 2));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, -1));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, -2));
        board.getTurnOrderTile().get(0).occupy(p1);
        board.getTurnOrderTile().get(1).occupy(p3);

        OfferingTile tile = new OfferingTile('A', 1, 1, 0);
        tile.occupy(p2);
        p2.setCurrentTile(tile);
        board.getOfferingTrack().add(tile);

        // Simula Controller: freeTile + moveToLastSlot
        p2.getCurrentTile().freeSlot();
        p2.setCurrentTile(null);
        game.moveToLastSlot(p2);

        TurnOrderSlot lastSlot = board.getTurnOrderTile().get(board.getTurnOrderTile().size() - 1);
        assertEquals(p2, lastSlot.getOccupiedBy(),
                "Il giocatore non-current deve finire nell'ultimo slot");
        assertNull(p2.getCurrentTile(), "La tessera deve essere liberata");
    }

    @Test
    public void testStartTurnStateSkipTurn_FreeSlotAndMarkPlaced() throws Exception {
        game.setState(new StartTurnState());

        // 4 slots: p2 starts in slot1, p1 in slot2, p3 in slot3
        // slot4 free at end
        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, 0));
        board.getTurnOrderTile().get(0).occupy(p2);
        board.getTurnOrderTile().get(1).occupy(p1);
        board.getTurnOrderTile().get(2).occupy(p3);

        game.setCurrentPlayer(p2);

        game.getCurrentState().skipTurn(game, "Bob");

        assertTrue(p2.hasPlacedTotem(),
                "hasPlacedTotem deve essere true dopo skipTurn");
        TurnOrderSlot lastSlot = board.getTurnOrderTile()
                .get(board.getTurnOrderTile().size() - 1);
        assertEquals(p2, lastSlot.getOccupiedBy(),
                "Il giocatore deve essere nell'ultimo slot del turn order");
    }

    @Test
    public void testStartTurnStateSkipTurn_NonCurrentPlayerIsSkipped() throws Exception {
        game.setState(new StartTurnState());

        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, 0));
        board.getTurnOrderTile().get(0).occupy(p1);
        board.getTurnOrderTile().get(1).occupy(p2);
        board.getTurnOrderTile().get(2).occupy(p3);

        game.setCurrentPlayer(p1);

        // p2 (non-current) viene saltato: moveToLastSlot + hasPlacedTotem
        // moveToLastSlot ora gestisce internamente la free e la compattazione
        game.moveToLastSlot(p2);
        p2.setHasPlacedTotem(true);

        assertTrue(p2.hasPlacedTotem(), "Non-current deve essere marcato come completato");
        TurnOrderSlot lastSlot = board.getTurnOrderTile()
                .get(board.getTurnOrderTile().size() - 1);
        assertEquals(p2, lastSlot.getOccupiedBy(),
                "Non-current deve essere nell'ultimo slot");
    }

    @Test
    public void testPickingComplete_DisconnectedPlayerInTurnOrder_DoesNotSkipRemaining() throws Exception {
        // Scenario: B disconnesso in StartTurnState (moveToLastSlot).
        // A e C piazzano totem. Transizione a PickingState.
        // A pesca → isPickingComplete() deve essere FALSE (C deve ancora pescare).
        game.setState(new PickingState());

        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 2));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, -1));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, -2));

        // A e C hanno totem sul tracciato offerte (piazzati in StartTurnState)
        OfferingTile tileA = new OfferingTile('A', 1, 1, 0);
        OfferingTile tileC = new OfferingTile('C', 1, 0, 0);
        tileA.occupy(p1); p1.setCurrentTile(tileA);
        tileC.occupy(p3); p3.setCurrentTile(tileC);
        board.getOfferingTrack().clear();
        board.getOfferingTrack().add(tileA);
        board.getOfferingTrack().add(tileC);

        // B è disconnesso: già spostato in turn order da moveToLastSlot
        board.getTurnOrderTile().get(1).occupy(p2);
        p2.setHasPlacedTotem(true);

        // A ha appena pescato → assegnato al turn order, tileA non ancora liberato
        // (executeEndTurnLogic: assignPickerToTurnOrder prima di freeSlot)
        board.getTurnOrderTile().get(0).occupy(p1);

        game.setCurrentPlayer(p3); // C è il prossimo picker

        // isPickingComplete DEVE essere false: C è sul tracciato offerte
        // ma NON è nel turn order (non ha ancora pescato).
        assertFalse(game.isPickingComplete(),
                "isPickingComplete deve essere false: C è ancora sul tracciato offerte");

        // C pesca → assegnato al turn order
        board.getTurnOrderTile().get(2).occupy(p3);
        tileC.freeSlot(); p3.setCurrentTile(null);

        // Ora tutti gli occupanti del tracciato offerte sono nel turn order
        assertTrue(game.isPickingComplete(),
                "isPickingComplete deve essere true: tutti hanno pescato");
    }

    @Test
    public void testPickingComplete_NormalFlow_NoDisconnections() throws Exception {
        // 3 giocatori, nessuna disconnessione: isPickingComplete segue il normale flusso
        game.setState(new PickingState());

        board.getTurnOrderTile().clear();
        for (int i = 0; i < 4; i++)
            board.getTurnOrderTile().add(new TurnOrderSlot(i+1, 0));

        OfferingTile tileA = new OfferingTile('A', 1, 1, 0);
        OfferingTile tileB = new OfferingTile('B', 1, 1, 0);
        OfferingTile tileC = new OfferingTile('C', 1, 0, 0);
        tileA.occupy(p1); p1.setCurrentTile(tileA);
        tileB.occupy(p2); p2.setCurrentTile(tileB);
        tileC.occupy(p3); p3.setCurrentTile(tileC);
        board.getOfferingTrack().clear();
        board.getOfferingTrack().add(tileA);
        board.getOfferingTrack().add(tileB);
        board.getOfferingTrack().add(tileC);

        game.setCurrentPlayer(p1);

        // Nessuno ha ancora pescato
        assertFalse(game.isPickingComplete(),
                "Nessuno ha pescato → isPickingComplete = false");

        // A pesca
        board.getTurnOrderTile().get(0).occupy(p1);
        assertFalse(game.isPickingComplete(),
                "Solo A ha pescato → false");

        // B pesca
        board.getTurnOrderTile().get(1).occupy(p2);
        assertFalse(game.isPickingComplete(),
                "A e B hanno pescato, C ancora no → false");

        // C pesca
        board.getTurnOrderTile().get(2).occupy(p3);
        tileC.freeSlot(); p3.setCurrentTile(null);
        assertTrue(game.isPickingComplete(),
                "Tutti hanno pescato → true");
    }

    @Test
    public void testStartTurnStateNonCurrent_WithPlacedTotem_FreesOfferingTile() throws Exception {
        game.setState(new StartTurnState());

        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, 0));
        board.getTurnOrderTile().get(0).occupy(p1);
        board.getTurnOrderTile().get(1).occupy(p2);
        board.getTurnOrderTile().get(2).occupy(p3);

        game.setCurrentPlayer(p3);

        OfferingTile tileP2 = new OfferingTile('B', 1, 1, 0);
        tileP2.occupy(p2);
        p2.setCurrentTile(tileP2);
        p2.setHasPlacedTotem(true);
        board.getOfferingTrack().add(tileP2);

        // Simula disconnessione non-current con totem piazzato
        if (p2.getCurrentTile() != null) {
            p2.getCurrentTile().freeSlot();
            p2.setCurrentTile(null);
        }
        // moveToLastSlot ora gestisce internamente la free e la compattazione
        game.moveToLastSlot(p2);

        assertNull(p2.getCurrentTile(), "currentTile deve essere null dopo disconnessione");
        assertTrue(tileP2.isAvailable(), "Offering tile deve essere libero");
        assertTrue(p2.hasPlacedTotem(), "hasPlacedTotem deve rimanere true");

        TurnOrderSlot lastSlot = board.getTurnOrderTile()
                .get(board.getTurnOrderTile().size() - 1);
        assertEquals(p2, lastSlot.getOccupiedBy(),
                "Disconnesso deve essere nell'ultimo slot del turn order");
    }

    @Test
    public void testMoveToLastSlot_CompactionWithMultipleDisconnections() throws Exception {
        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 2));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, -1));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, -2));
        board.getTurnOrderTile().get(0).occupy(p1);
        board.getTurnOrderTile().get(1).occupy(p2);
        board.getTurnOrderTile().get(2).occupy(p3);

        // Primo disconnesso: p2 (non-current, senza totem)
        game.moveToLastSlot(p2);
        p2.setHasPlacedTotem(true);

        // Dopo compattazione: p1->slot0, p3->slot1, EMPTY->slot2, p2->slot3
        assertEquals(p3, board.getTurnOrderTile().get(1).getOccupiedBy(),
                "p3 deve essere stato compattato in slot1");
        assertEquals(p2, board.getTurnOrderTile().get(3).getOccupiedBy(),
                "p2 deve essere nell'ultimo slot (slot3)");

        // Secondo disconnesso: p1 (current)
        game.moveToLastSlot(p1);
        p1.setHasPlacedTotem(true);

        // Dopo compattazione: p3->slot0, EMPTY->slot1, p2->slot2, p1->slot3
        assertEquals(p3, board.getTurnOrderTile().get(0).getOccupiedBy(),
                "p3 deve essere in slot0 dopo seconda compattazione");
        assertEquals(p2, board.getTurnOrderTile().get(2).getOccupiedBy(),
                "p2 deve essere stato compattato in slot2");
        assertEquals(p1, board.getTurnOrderTile().get(3).getOccupiedBy(),
                "p1 deve essere nell'ultimo slot (slot3)");
    }

    @Test
    public void testEventStateTransition_SetsFirstOccupiedSlotAsCurrent() throws Exception {
        board.getTurnOrderTile().clear();
        board.getTurnOrderTile().add(new TurnOrderSlot(1, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(2, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(3, 0));
        board.getTurnOrderTile().add(new TurnOrderSlot(4, 0));
        board.getTurnOrderTile().get(1).occupy(p1);
        board.getTurnOrderTile().get(3).occupy(p2);

        game.setState(new EventState());
        game.setTribeDeck(new Deck<>(new Stack<>()));
        game.setCurrentPlayer(p1);

        game.getCurrentState().resolvePhase(game);

        assertTrue(game.getCurrentState() instanceof StartTurnState,
                "Dopo EventState deve essere StartTurnState");
        assertEquals(p1, game.getCurrentPlayer(),
                "Il primo slot occupato deve essere il nuovo currentPlayer");
    }
}
