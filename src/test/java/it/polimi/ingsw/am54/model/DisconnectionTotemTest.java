package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Test che riproduce il bug: "un giocatore si disconnette, ma il totem
 * di un altro giocatore (connesso) scompare".
 *
 * Setup: 3 giocatori (Alice, Bob, Charlie). Tutti hanno piazzato il totem
 * su una tessera offerta e sono in PickingState. Uno si disconnette →
 * verifichiamo che gli altri mantengano i loro totem (getCurrentTile() != null).
 */
public class DisconnectionTotemTest {

    private Game game;
    private Player alice;
    private Player bob;
    private Player charlie;

    private Board board;
    private OfferingTile tile0;
    private OfferingTile tile1;
    private OfferingTile tile2;

    @BeforeEach
    public void setUp() {
        game = new Game();
        board = new Board();
        game.setBoard(board);

        alice = new Player(); alice.setNickname("Alice");
        bob   = new Player(); bob.setNickname("Bob");
        charlie = new Player(); charlie.setNickname("Charlie");

        game.getPlayers().add(alice);
        game.getPlayers().add(bob);
        game.getPlayers().add(charlie);

        // --- Turn order ---
        TurnOrderSlot s1 = new TurnOrderSlot(1, 0); s1.occupy(alice);
        TurnOrderSlot s2 = new TurnOrderSlot(2, 0); s2.occupy(bob);
        TurnOrderSlot s3 = new TurnOrderSlot(3, 0); s3.occupy(charlie);
        board.getTurnOrderTile().add(s1);
        board.getTurnOrderTile().add(s2);
        board.getTurnOrderTile().add(s3);

        // --- Offering tiles (3 tiles for 3 players) ---
        tile0 = new OfferingTile('A', 2, 1, 0);
        tile1 = new OfferingTile('B', 2, 1, 0);
        tile2 = new OfferingTile('C', 2, 1, 0);
        board.getOfferingTrack().add(tile0);
        board.getOfferingTrack().add(tile1);
        board.getOfferingTrack().add(tile2);

        // Tutti e 3 piazzano il totem su tessere diverse
        tile0.occupy(alice); alice.setCurrentTile(tile0); alice.setHasPlacedTotem(true);
        tile1.occupy(bob);   bob.setCurrentTile(tile1);   bob.setHasPlacedTotem(true);
        tile2.occupy(charlie); charlie.setCurrentTile(tile2); charlie.setHasPlacedTotem(true);

        // Li togliamo dal turn order (come fa StartTurnState.placeTotem)
        s1.freeSlot();
        s2.freeSlot();
        s3.freeSlot();

        // Stato: PickingState
        game.setState(new it.polimi.ingsw.am54.model.states.PickingState());
        game.setCurrentPlayer(alice);
    }

    // ================================================
    // 1. NON-CURRENT disconnesso in PickingState
    //    Charlie (non di turno) si disconnette
    // ================================================
    @Test
    public void testNonCurrentDisconnectInPicking_otherTotemsIntact() {
        // Pre-condizioni: tutti hanno un tile
        assertNotNull(alice.getCurrentTile(),  "Alice deve avere un tile");
        assertNotNull(bob.getCurrentTile(),    "Bob deve avere un tile");
        assertNotNull(charlie.getCurrentTile(), "Charlie deve avere un tile");
        assertEquals(alice, game.getCurrentPlayer(), "Alice è di turno");

        // Simula autoHandleDisconnection per Charlie (non-current in PickingState)
        charlie.getCurrentTile().freeSlot();
        charlie.setCurrentTile(null);
        game.moveToLastSlot(charlie);

        // ---- VERIFICHE ----
        // Charlie ha perso il suo tile (corretto — si è disconnesso)
        assertNull(charlie.getCurrentTile(), "Charlie ha perso il suo tile (disconnesso)");

        // Alice e Bob DEVONO ancora avere i loro tile
        assertNotNull(alice.getCurrentTile(), "Alice deve ancora avere il suo tile!");
        assertNotNull(bob.getCurrentTile(),   "Bob deve ancora avere il suo tile!");

        // Alice è ancora il current player
        assertEquals(alice, game.getCurrentPlayer(), "Alice resta il giocatore di turno");
    }

    // ================================================
    // 2. CURRENT PLAYER disconnesso in PickingState
    //    Alice (di turno) si disconnette
    // ================================================
    @Test
    public void testCurrentPlayerDisconnectInPicking_otherTotemsIntact() {
        // Pre-condizioni
        assertNotNull(alice.getCurrentTile());
        assertNotNull(bob.getCurrentTile());
        assertNotNull(charlie.getCurrentTile());

        // Simula skipTurn per Alice (current) in PickingState
        // (logica da PickingState.skipTurn)
        TurnOrderSlot slot = game.moveToLastSlot(alice);
        assertNotNull(slot, "Alice deve finire in uno slot del turn order");
        alice.getCurrentTile().freeSlot();
        alice.setCurrentTile(null);

        // Il prossimo picker dovrebbe essere Bob o Charlie
        Player next = game.getCurrentPicker();

        // ---- VERIFICHE ----
        assertNull(alice.getCurrentTile(), "Alice ha perso il suo tile (disconnessa)");
        assertNotNull(bob.getCurrentTile(),   "Bob deve ancora avere il suo tile!");
        assertNotNull(charlie.getCurrentTile(), "Charlie deve ancora avere il suo tile!");

        // Il gioco va avanti con un altro picker
        assertNotNull(next, "Ci deve essere un prossimo picker");
        assertNotEquals(alice, next, "Il prossimo picker non può essere Alice");
    }

    // ================================================
    // 3. NOTIFICHE — Verifica che il notifyTotemReturned
    //    usi il nickname corretto (quello del disconnesso)
    // ================================================
    @Test
    public void testNotifyTotemReturned_sendsCorrectPlayer() {
        final List<String> notifiedNicknames = new ArrayList<>();

        // Aggiungiamo un observer di test
        IGameObserver observer = new IGameObserver() {
            @Override public void onTotemReturned(Game g, Player p, int pos, int food, int aura) {
                notifiedNicknames.add(p.getNickname());
            }
            // Altri metodi vuoti
            @Override public void onPlayerJoined(Game g, Player p) {}
            @Override public void onTotemColorChosen(Game g, Player p, TotemColors c) {}
            @Override public void onTotemPlaced(Game g, Player p, int pos) {}
            @Override public void onCardPicked(Game g, Player p, String r, int i, int f, int m) {}
            @Override public void onTurnChanged(Game g, Player p) {}
            @Override public void onStateChanged(Game g, GameState s) {}
            @Override public void onEventResolved(Game g, EventCard e) {}
            @Override public void onEndGame(Game g, List<Player> w) {}
        };
        game.addObserver(observer);

        // Simula la disconnessione di Charlie
        charlie.getCurrentTile().freeSlot();
        charlie.setCurrentTile(null);
        game.moveToLastSlot(charlie);
        game.notifyTotemReturned(charlie, -1);

        // ---- VERIFICHE ----
        assertEquals(1, notifiedNicknames.size(),
                "Deve essere stata inviata ESATTAMENTE una notifica");
        assertEquals("Charlie", notifiedNicknames.get(0),
                "La notifica deve riguardare Charlie, NON Alice o Bob!");
    }

    // ================================================
    // 4. StartTurnState — non-current senza totem piazzato
    // ================================================
    @Test
    public void testNonCurrentDisconnectInStartTurn_othersIntact() {
        // Resetta: nessuno ha ancora piazzato il totem, siamo in StartTurnState
        tile0.freeSlot(); alice.setCurrentTile(null); alice.setHasPlacedTotem(false);
        tile1.freeSlot(); bob.setCurrentTile(null);   bob.setHasPlacedTotem(false);
        tile2.freeSlot(); charlie.setCurrentTile(null); charlie.setHasPlacedTotem(false);

        // Rimettiamo tutti nel turn order
        board.getTurnOrderTile().get(0).occupy(alice);
        board.getTurnOrderTile().get(1).occupy(bob);
        board.getTurnOrderTile().get(2).occupy(charlie);

        game.setState(new it.polimi.ingsw.am54.model.states.StartTurnState());
        game.setCurrentPlayer(alice);

        // Pre-condizioni
        assertFalse(alice.hasPlacedTotem());
        assertFalse(bob.hasPlacedTotem());
        assertFalse(charlie.hasPlacedTotem());

        // Simula autoHandleDisconnection per Charlie (non-current, StartTurnState)
        board.getTurnOrderTile().stream()
                .filter(s -> charlie.equals(s.getOccupiedBy()))
                .findFirst()
                .ifPresent(s -> s.freeSlot());
        game.moveToLastSlot(charlie);
        charlie.setHasPlacedTotem(true);

        // ---- VERIFICHE ----
        assertTrue(charlie.hasPlacedTotem(), "Charlie marcato come piazzato (skip)");
        assertFalse(alice.hasPlacedTotem(),  "Alice NON deve essere marcata come piazzata!");
        assertFalse(bob.hasPlacedTotem(),    "Bob NON deve essere marcato come piazzato!");

        // Alice è ancora di turno
        assertEquals(alice, game.getCurrentPlayer(), "Alice resta di turno");
    }
}
