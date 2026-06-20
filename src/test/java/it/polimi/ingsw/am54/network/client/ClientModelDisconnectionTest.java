package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Test that simulates the sequence of messages the client receives
 * durante una disconnessione, e verifica che lo stato del ClientModel
 * is correct: the disconnected player's totem is removed
 * dall'offeringTrack, quelli degli altri rimangono.
 */
public class ClientModelDisconnectionTest {

    private ClientModel model;

    @BeforeEach
    public void setUp() {
        model = new ClientModel();
        model.setOnModelUpdatedCallback(() -> { /* no-op */ });

        // Simulates initialization: all 3 in Lobby
        model.updatePlayerJoined("Alice");
        model.updatePlayerJoined("Bob");
        model.updatePlayerJoined("Charlie");

        // Colori
        model.updateTotemColor("Alice", TotemColors.RED);
        model.updateTotemColor("Bob", TotemColors.YELLOW);
        model.updateTotemColor("Charlie", TotemColors.BLUE);
    }

    /**
     * Test: Received a StateChangedUpdate that initializes the offeringTrack.
     * Then: Charlie disconnected → TotemReturnedUpdate arrives.
     * Verifica: Alice e Bob restano nell'offeringTrack.
     */
    @Test
    public void testTotemReturnedAfterDisconnect_otherTotemsIntact() {
        // 1. Simula StateChangedUpdate: siamo in PickingState, tutti hanno piazzato
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        stats.put("Alice",   new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.RED, List.of(), List.of()));
        stats.put("Bob",     new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.YELLOW, List.of(), List.of()));
        stats.put("Charlie", new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.BLUE, List.of(), List.of()));

        List<String> turnOrder = new ArrayList<>();
        // Turn order: Alice, Bob, Charlie (all initially in turn)
        turnOrder.add("[Slot 1 | Cibo: +0] -> Alice");
        turnOrder.add("[Slot 2 | Cibo: +0] -> Bob");
        turnOrder.add("[Slot 3 | Cibo: +0] -> Charlie");

        Map<Integer, String> offering = new TreeMap<>();
        offering.put(0, "[Tessera A | Pick Su:2 Giu:1 | Bonus:0] -> VUOTO");
        offering.put(1, "[Tessera B | Pick Su:2 Giu:1 | Bonus:0] -> VUOTO");
        offering.put(2, "[Tessera C | Pick Su:2 Giu:1 | Bonus:0] -> VUOTO");

        StateChangedUpdate stateUpdate = new StateChangedUpdate(
                "PickingState", 1, 1, stats, turnOrder, offering,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        model.updateState(stateUpdate);

        // 2. Simulate placeTotem: each player places the totem on a tile
        model.updateTotemPlacement("Alice", 0);
        model.updateTotemPlacement("Bob", 1);
        model.updateTotemPlacement("Charlie", 2);

        // After placeTotem, offeringTrack should have all 3
        Map<Integer, String> offeringBefore = model.getOfferingTrack();
        assertTrue(offeringBefore.get(0).contains("Alice"),
                "Alice deve essere sull'offeringTrack dopo placeTotem");
        assertTrue(offeringBefore.get(1).contains("Bob"),
                "Bob deve essere sull'offeringTrack dopo placeTotem");
        assertTrue(offeringBefore.get(2).contains("Charlie"),
                "Charlie deve essere sull'offeringTrack dopo placeTotem");

        // 3. Simulate Charlie's disconnection: TotemReturnedUpdate arrives
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 4. VERIFY: Alice and Bob must STILL be on the offeringTrack
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(0).contains("Alice"),
                "Alice deve ANCORA essere sull'offeringTrack DOPO la disconnessione di Charlie!");
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve ANCORA essere sull'offeringTrack DOPO la disconnessione di Charlie!");
        // Charlie may no longer be there (removed)
        if (offeringAfter.containsKey(2)) {
            assertFalse(offeringAfter.get(2).contains("Charlie"),
                    "Charlie NON deve essere sull'offeringTrack (disconnesso)");
        }

        // 5. Verify turnOrderTrack: Charlie must be there (back in turn)
        List<String> turnOrderAfter = model.getTurnOrderTrack();
        boolean charlieInTurnOrder = turnOrderAfter.stream()
                .anyMatch(s -> s.contains("Charlie"));
        assertTrue(charlieInTurnOrder,
                "Charlie deve essere nel turnOrderTrack dopo TotemReturnedUpdate");
    }

    /**
     * Realistic Test: StateChangedUpdate with all players already
     * on the offeringTrack (as in the StartTurn → PickingState transition),
     * then TotemReturnedUpdate arrives for a disconnected player.
     * Verifica che gli altri NON vengano rimossi.
     */
    @Test
    public void testRealisticStateChangeThenTotemReturned() {
        // 1. StateChangedUpdate: siamo in PickingState, tutti e 3 hanno piazzato
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        stats.put("Alice",   new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.RED, List.of(), List.of()));
        stats.put("Bob",     new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.YELLOW, List.of(), List.of()));
        stats.put("Charlie", new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.BLUE, List.of(), List.of()));

        // Turn order: ALL EMPTY (they are all on the offeringTrack)
        List<String> turnOrder = new ArrayList<>();
        turnOrder.add("[Slot 1 | Cibo: +0] -> VUOTO");
        turnOrder.add("[Slot 2 | Cibo: +0] -> VUOTO");
        turnOrder.add("[Slot 3 | Cibo: +0] -> VUOTO");

        // Offering: TUTTI OCCUPATI (Alice, Bob, Charlie)
        Map<Integer, String> offering = new TreeMap<>();
        offering.put(0, "[Tessera A | Pick Su:2 Giù:1 | Bonus 🍖:+0] -> Alice");
        offering.put(1, "[Tessera B | Pick Su:2 Giù:1 | Bonus 🍖:+0] -> Bob");
        offering.put(2, "[Tessera C | Pick Su:2 Giù:1 | Bonus 🍖:+0] -> Charlie");

        StateChangedUpdate stateUpdate = new StateChangedUpdate(
                "PickingState", 1, 1, stats, turnOrder, offering,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        model.updateState(stateUpdate);

        // 2. Simulate Charlie's disconnection: TotemReturnedUpdate arrives
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 3. VERIFY: Alice and Bob must still be on the offeringTrack
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(0).contains("Alice"),
                "Alice deve ANCORA essere sull'offeringTrack DOPO disconnessione di Charlie!");
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve ANCORA essere sull'offeringTrack DOPO disconnessione di Charlie!");
        if (offeringAfter.containsKey(2)) {
            assertFalse(offeringAfter.get(2).contains("Charlie"),
                    "Charlie NON deve essere sull'offeringTrack (disconnesso)");
        }

        // 4. Verify turnOrderTrack: Charlie must be there (back in queue)
        List<String> turnOrderAfter = model.getTurnOrderTrack();
        boolean charlieInTurnOrder = turnOrderAfter.stream()
                .anyMatch(s -> s.contains("Charlie"));
        assertTrue(charlieInTurnOrder,
                "Charlie deve essere nel turnOrderTrack dopo TotemReturnedUpdate");

        // Alice and Bob must NOT be in turnOrderTrack (they still have totems on offering)
        boolean aliceInTurnOrder = turnOrderAfter.stream()
                .anyMatch(s -> s.contains("Alice"));
        boolean bobInTurnOrder = turnOrderAfter.stream()
                .anyMatch(s -> s.contains("Bob"));
        assertFalse(aliceInTurnOrder, "Alice NON deve essere nel turnOrderTrack (ha ancora totem)");
        assertFalse(bobInTurnOrder, "Bob NON deve essere nel turnOrderTrack (ha ancora totem)");
    }

    /**
     * Test FULL SEQUENCE: PlayerDisconnectedUpdate + TotemReturnedUpdate +
     * StateChangedUpdate (dovuto a transizione di stato dopo skip turn).
     * Simula: non-current player con tile si disconnette →
     * the match does NOT change state → only TotemReturnedUpdate arrives.
     * Verifica: altri totem preservati.
     */
    @Test
    public void testDisconnectNonCurrentNoStateChange_othersPreserved() {
        // 1. StateChangedUpdate: PickingState, all with totems
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        stats.put("Alice",   new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.RED, List.of(), List.of()));
        stats.put("Bob",     new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.YELLOW, List.of(), List.of()));
        stats.put("Charlie", new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.BLUE, List.of(), List.of()));

        List<String> turnOrder = List.of("[Slot 1 | Cibo: +0] -> VUOTO", "[Slot 2 | Cibo: +0] -> VUOTO", "[Slot 3 | Cibo: +0] -> VUOTO");
        Map<Integer, String> offering = new TreeMap<>();
        offering.put(0, "[Tessera A | ...] -> Alice");
        offering.put(1, "[Tessera B | ...] -> Bob");
        offering.put(2, "[Tessera C | ...] -> Charlie");

        model.updateState(new StateChangedUpdate(
                "PickingState", 1, 1, stats, turnOrder, offering,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));

        // 2. PlayerDisconnectedUpdate + TotemReturnedUpdate for Charlie (non-current with tile)
        model.updatePlayerDisconnected("Charlie");
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 3. VERIFY: Alice and Bob still there
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(0).contains("Alice"),
                "Alice deve ANCORA essere sull'offeringTrack!");
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve ANCORA essere sull'offeringTrack!");
    }

    /**
     * Test SEQUENCE: PlayerDisconnectedUpdate + TotemReturnedUpdate +
     * StateChangedUpdate (transizione a StartTurnState).
     * Simula: ultimo picker si disconnette → picking completo →
     * game.setState(new StartTurnState()) → arriva StateChangedUpdate.
     * VERIFICA: il nuovo StateChangedUpdate sovrascrive correttamente
     * lo stato (tutti i totem sono tornati, tutti in turn order).
     */
    @Test
    public void testDisconnectCausingStateTransition() {
        // 1. Stato iniziale: PickingState, tutti in offering
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        stats.put("Alice",   new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.RED, List.of(), List.of()));
        stats.put("Bob",     new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.YELLOW, List.of(), List.of()));
        stats.put("Charlie", new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.BLUE, List.of(), List.of()));

        List<String> initialTurnOrder = List.of("[Slot 1 | Cibo: +0] -> VUOTO", "[Slot 2 | Cibo: +0] -> VUOTO", "[Slot 3 | Cibo: +0] -> VUOTO");
        Map<Integer, String> initialOffering = new TreeMap<>();
        initialOffering.put(0, "[Tessera A | ...] -> Alice");
        initialOffering.put(1, "[Tessera B | ...] -> Bob");
        initialOffering.put(2, "[Tessera C | ...] -> Charlie");

        model.updateState(new StateChangedUpdate(
                "PickingState", 1, 1, stats, initialTurnOrder, initialOffering,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));

        // 2. Simulate Charlie's disconnection
        model.updatePlayerDisconnected("Charlie");
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 3. The server skipped Charlie → picking complete → transition to StartTurnState
        // The server sends a NEW StateChangedUpdate with the updated state
        // (all in turnOrder, offering EMPTY)
        List<String> newTurnOrder = new ArrayList<>();
        newTurnOrder.add("[Slot 1 | Cibo: +3] -> Alice");
        newTurnOrder.add("[Slot 2 | Cibo: +2] -> Bob");
        newTurnOrder.add("[Slot 3 | Cibo: +1] -> Charlie");

        Map<Integer, String> newOffering = new TreeMap<>();
        newOffering.put(0, "[Tessera A | Pick Su:2 Giù:1 | Bonus 🍖:+0] -> VUOTO");
        newOffering.put(1, "[Tessera B | Pick Su:2 Giù:1 | Bonus 🍖:+0] -> VUOTO");
        newOffering.put(2, "[Tessera C | Pick Su:2 Giù:1 | Bonus 🍖:+0] -> VUOTO");

        model.updateState(new StateChangedUpdate(
                "StartTurnState", 1, 2, stats, newTurnOrder, newOffering,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));

        // 4. VERIFICA: dopo il nuovo StateChangedUpdate, l'offeringTrack DEVE essere VUOTO
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        for (int i = 0; i < 3; i++) {
            String val = offeringAfter.get(i);
            assertTrue(val != null && (val.contains("VUOTO") || val.contains("vuoto")),
                    "Tessera " + i + " deve essere VUOTA dopo nuovo StateChangedUpdate, invece: " + val);
        }

        // 5. VERIFY: turnOrderTrack MUST contain all 3
        List<String> turnOrderAfter = model.getTurnOrderTrack();
        assertEquals(3, turnOrderAfter.size(),
                "turnOrderTrack deve avere 3 giocatori dopo nuovo StateChangedUpdate");
        assertTrue(turnOrderAfter.get(0).contains("Alice"),
                "Primo in turn order deve essere Alice");
    }

    /**
     * Test: TotemReturnedUpdate for a player who has ALREADY PICKED
     * (no longer in offeringTrack, already in turnOrderTrack).
     * Verify: no one else is removed from the offeringTrack.
     */
    @Test
    public void testTotemReturnedForAlreadyPickedPlayer() {
        // 1. StateChangedUpdate: PickingState
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        stats.put("Alice",   new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.RED, List.of(), List.of()));
        stats.put("Bob",     new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.YELLOW, List.of(), List.of()));
        stats.put("Charlie", new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.BLUE, List.of(), List.of()));

        List<String> initialTurnOrder = List.of("[Slot 1 | Cibo: +0] -> VUOTO", "[Slot 2 | Cibo: +0] -> VUOTO", "[Slot 3 | Cibo: +0] -> VUOTO");
        Map<Integer, String> initialOffering = new TreeMap<>();
        initialOffering.put(0, "[Tessera A | ...] -> Alice");
        initialOffering.put(1, "[Tessera B | ...] -> Bob");
        initialOffering.put(2, "[Tessera C | ...] -> Charlie");

        model.updateState(new StateChangedUpdate(
                "PickingState", 1, 1, stats, initialTurnOrder, initialOffering,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));

        // 2. Alice draws: CardPickedUpdate arrives with updated offering (Alice removed)
        Map<Integer, String> afterPick = new TreeMap<>();
        afterPick.put(1, "[Tessera B | ...] -> Bob");
        afterPick.put(2, "[Tessera C | ...] -> Charlie");

        model.updateCardPicked("Alice", "upper", 0, 3, 0, afterPick);

        // Alice was removed from offering by CardPickedUpdate
        // Now TotemReturnedUpdate arrives for Alice (from server after executeEndTurnLogic)
        model.updateTotemReturned("Alice", 0, 3, 3);

        // 3. VERIFY: Alice is now in turnOrderTrack
        List<String> turnOrderAfter = model.getTurnOrderTrack();
        boolean aliceInTurnOrder = turnOrderAfter.stream().anyMatch(s -> s.contains("Alice"));
        assertTrue(aliceInTurnOrder, "Alice deve essere in turnOrderTrack dopo TotemReturnedUpdate");

        // 4. Simulate Charlie's disconnection (who is still on offeringTrack)
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 5. VERIFY: Bob must still be on offeringTrack (Charlie removed, Alice already removed)
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve ANCORA essere sull'offeringTrack!");
        assertTrue(offeringAfter.get(1) != null && !offeringAfter.get(1).contains("Charlie"),
                "Charlie NON deve essere sull'offeringTrack");
        // Alice is no longer there (already removed by CardPickedUpdate), position 0 may not exist
    }

    /**
     * Test: TotemReturnedUpdate per Alice che GIA' nel turnOrderTrack.
     * Simulates: updateTotemReturned arrives for a player not in the offeringTrack.
     * Must not remove other players from the offeringTrack.
     */
    @Test
    public void testTotemReturnedForNonexistentPlayer_noSideEffects() {
        model.updateTotemReturned("Alice", 0, 5, 3); // la aggiunge al turnOrderTrack
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        stats.put("Alice",   new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.RED, List.of(), List.of()));
        stats.put("Bob",     new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.YELLOW, List.of(), List.of()));
        stats.put("Charlie", new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.BLUE, List.of(), List.of()));

        List<String> turnOrder = List.of("[Slot 1 | Cibo: +0] -> Alice", "[Slot 2 | Cibo: +0] -> Bob", "[Slot 3 | Cibo: +0] -> Charlie");
        Map<Integer, String> offering = new TreeMap<>();
        offering.put(0, "[Tessera A | ...] -> VUOTO");
        offering.put(1, "[Tessera B | ...] -> VUOTO");
        offering.put(2, "[Tessera C | ...] -> VUOTO");

        model.updateState(new StateChangedUpdate("PickingState", 1, 1, stats, turnOrder, offering,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));

        // Alice places the totem
        model.updateTotemPlacement("Alice", 0);
        // Bob places the totem
        model.updateTotemPlacement("Bob", 1);

        // Simulates TotemReturned for Charlie (NOT in the offeringTrack)
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // Alice and Bob must remain in the offeringTrack
        Map<Integer, String> track = model.getOfferingTrack();
        assertTrue(track.get(0).contains("Alice"),
                "Alice deve restare nell'offeringTrack");
        assertTrue(track.get(1).contains("Bob"),
                "Bob deve restare nell'offeringTrack");
    }

    /**
     * Test: Real sequence — StateChangedUpdate then TotemReturnedUpdate.
     * Simula il caso in cui un StateChangedUpdate arriva DOPO il
     * TotemReturnedUpdate (sovrascrive tutto).
     */
    @Test
    public void testStateChangedAfterTotemReturned_preservesCorrectState() {
        // 1. Place totem for all
        model.updateTotemPlacement("Alice", 0);
        model.updateTotemPlacement("Bob", 1);
        model.updateTotemPlacement("Charlie", 2);

        // 2. TotemReturned for Charlie (disconnection)
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 3. StateChangedUpdate that arrives later (like next round)
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        stats.put("Alice",   new StateChangedUpdate.PlayerSnapshot(6, 3, 1, TotemColors.RED, List.of(), List.of()));
        stats.put("Bob",     new StateChangedUpdate.PlayerSnapshot(5, 4, 0, TotemColors.YELLOW, List.of(), List.of()));
        stats.put("Charlie", new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.BLUE, List.of(), List.of()));

        List<String> turnOrder = new ArrayList<>();
        turnOrder.add("[Slot 1 | Cibo: +0] -> Alice");
        turnOrder.add("[Slot 2 | Cibo: +0] -> Bob");
        turnOrder.add("[Slot 3 | Cibo: +0] -> Charlie");

        Map<Integer, String> offering = new TreeMap<>();
        offering.put(0, "[Tessera A | Pick Su:2 Giu:1 | Bonus:0] -> VUOTO");
        offering.put(1, "[Tessera B | Pick Su:2 Giu:1 | Bonus:0] -> VUOTO");
        offering.put(2, "[Tessera C | Pick Su:2 Giu:1 | Bonus:0] -> VUOTO");

        StateChangedUpdate stateUpdate = new StateChangedUpdate(
                "PickingState", 1, 1, stats, turnOrder, offering,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        model.updateState(stateUpdate);

        // 4. Place totem again for Alice and Bob (new round)
        model.updateTotemPlacement("Alice", 0);
        model.updateTotemPlacement("Bob", 1);

        // Charlie is disconnected so no placeTotem.
        // After placeTotem: Alice and Bob must be on offeringTrack
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(0).contains("Alice"),
                "Alice deve essere sull'offeringTrack nel nuovo round");
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve essere sull'offeringTrack nel nuovo round");

        // Charlie must NOT be on offeringTrack (he didn't place)
        if (offeringAfter.containsKey(2)) {
            assertFalse(offeringAfter.get(2).contains("Charlie"),
                    "Charlie NON deve essere sull'offeringTrack nel nuovo round");
        }
    }
}
