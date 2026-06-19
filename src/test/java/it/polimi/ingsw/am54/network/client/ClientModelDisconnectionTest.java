package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Test che simula la sequenza di messaggi che il client riceve
 * durante una disconnessione, e verifica che lo stato del ClientModel
 * sia corretto: il totem del giocatore disconnesso viene rimosso
 * dall'offeringTrack, quelli degli altri rimangono.
 */
public class ClientModelDisconnectionTest {

    private ClientModel model;

    @BeforeEach
    public void setUp() {
        model = new ClientModel();
        model.setOnModelUpdatedCallback(() -> { /* no-op */ });

        // Simula l'inizializzazione: tutti e 3 in Lobby
        model.updatePlayerJoined("Alice");
        model.updatePlayerJoined("Bob");
        model.updatePlayerJoined("Charlie");

        // Colori
        model.updateTotemColor("Alice", TotemColors.RED);
        model.updateTotemColor("Bob", TotemColors.YELLOW);
        model.updateTotemColor("Charlie", TotemColors.BLUE);
    }

    /**
     * Test: Ha ricevuto uno StateChangedUpdate che inizializza l'offeringTrack.
     * Poi: Charlie disconnesso → arriva TotemReturnedUpdate.
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
        // Turn order: Alice, Bob, Charlie (tutti inizialmente in turno)
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

        // 2. Simula i placeTotem: ogni giocatore piazza il totem su una tessera
        model.updateTotemPlacement("Alice", 0);
        model.updateTotemPlacement("Bob", 1);
        model.updateTotemPlacement("Charlie", 2);

        // Dopo i placeTotem, offeringTrack dovrebbe avere tutti e 3
        Map<Integer, String> offeringBefore = model.getOfferingTrack();
        assertTrue(offeringBefore.get(0).contains("Alice"),
                "Alice deve essere sull'offeringTrack dopo placeTotem");
        assertTrue(offeringBefore.get(1).contains("Bob"),
                "Bob deve essere sull'offeringTrack dopo placeTotem");
        assertTrue(offeringBefore.get(2).contains("Charlie"),
                "Charlie deve essere sull'offeringTrack dopo placeTotem");

        // 3. Simula la disconnessione di Charlie: arriva TotemReturnedUpdate
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 4. VERIFICA: Alice e Bob devono ANCORA essere sull'offeringTrack
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(0).contains("Alice"),
                "Alice deve ANCORA essere sull'offeringTrack DOPO la disconnessione di Charlie!");
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve ANCORA essere sull'offeringTrack DOPO la disconnessione di Charlie!");
        // Charlie potrebbe non esserci più (rimosso)
        if (offeringAfter.containsKey(2)) {
            assertFalse(offeringAfter.get(2).contains("Charlie"),
                    "Charlie NON deve essere sull'offeringTrack (disconnesso)");
        }

        // 5. Verifica il turnOrderTrack: Charlie deve esserci (è tornato in turno)
        List<String> turnOrderAfter = model.getTurnOrderTrack();
        boolean charlieInTurnOrder = turnOrderAfter.stream()
                .anyMatch(s -> s.contains("Charlie"));
        assertTrue(charlieInTurnOrder,
                "Charlie deve essere nel turnOrderTrack dopo TotemReturnedUpdate");
    }

    /**
     * Test REALISTICO: StateChangedUpdate con tutti i giocatori già
     * sull'offeringTrack (come nel passaggio StartTurn → PickingState),
     * poi arriva TotemReturnedUpdate per un giocatore disconnesso.
     * Verifica che gli altri NON vengano rimossi.
     */
    @Test
    public void testRealisticStateChangeThenTotemReturned() {
        // 1. StateChangedUpdate: siamo in PickingState, tutti e 3 hanno piazzato
        Map<String, StateChangedUpdate.PlayerSnapshot> stats = new LinkedHashMap<>();
        stats.put("Alice",   new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.RED, List.of(), List.of()));
        stats.put("Bob",     new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.YELLOW, List.of(), List.of()));
        stats.put("Charlie", new StateChangedUpdate.PlayerSnapshot(5, 3, 0, TotemColors.BLUE, List.of(), List.of()));

        // Turn order: TUTTI VUOTO (sono tutti sull'offeringTrack)
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

        // 2. Simula disconnessione di Charlie: arriva TotemReturnedUpdate
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 3. VERIFICA: Alice e Bob devono ancora essere sull'offeringTrack
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(0).contains("Alice"),
                "Alice deve ANCORA essere sull'offeringTrack DOPO disconnessione di Charlie!");
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve ANCORA essere sull'offeringTrack DOPO disconnessione di Charlie!");
        if (offeringAfter.containsKey(2)) {
            assertFalse(offeringAfter.get(2).contains("Charlie"),
                    "Charlie NON deve essere sull'offeringTrack (disconnesso)");
        }

        // 4. Verifica turnOrderTrack: Charlie deve esserci (è tornato in coda)
        List<String> turnOrderAfter = model.getTurnOrderTrack();
        boolean charlieInTurnOrder = turnOrderAfter.stream()
                .anyMatch(s -> s.contains("Charlie"));
        assertTrue(charlieInTurnOrder,
                "Charlie deve essere nel turnOrderTrack dopo TotemReturnedUpdate");

        // Alice e Bob NON devono essere nel turnOrderTrack (hanno ancora totem sull'offering)
        boolean aliceInTurnOrder = turnOrderAfter.stream()
                .anyMatch(s -> s.contains("Alice"));
        boolean bobInTurnOrder = turnOrderAfter.stream()
                .anyMatch(s -> s.contains("Bob"));
        assertFalse(aliceInTurnOrder, "Alice NON deve essere nel turnOrderTrack (ha ancora totem)");
        assertFalse(bobInTurnOrder, "Bob NON deve essere nel turnOrderTrack (ha ancora totem)");
    }

    /**
     * Test SEQUENZA COMPLETA: PlayerDisconnectedUpdate + TotemReturnedUpdate +
     * StateChangedUpdate (dovuto a transizione di stato dopo skip turn).
     * Simula: non-current player con tile si disconnette →
     * la partita NON cambia stato → arriva SOLO TotemReturnedUpdate.
     * Verifica: altri totem preservati.
     */
    @Test
    public void testDisconnectNonCurrentNoStateChange_othersPreserved() {
        // 1. StateChangedUpdate: PickingState, tutti con totem
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

        // 2. PlayerDisconnectedUpdate + TotemReturnedUpdate per Charlie (non-current con tile)
        model.updatePlayerDisconnected("Charlie");
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 3. VERIFICA: Alice e Bob ancora lì
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(0).contains("Alice"),
                "Alice deve ANCORA essere sull'offeringTrack!");
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve ANCORA essere sull'offeringTrack!");
    }

    /**
     * Test SEQUENZA: PlayerDisconnectedUpdate + TotemReturnedUpdate +
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

        // 2. Simula disconnessione di Charlie
        model.updatePlayerDisconnected("Charlie");
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 3. Il server ha skippato Charlie → picking completo → transizione a StartTurnState
        // Il server invia un NUOVO StateChangedUpdate con lo stato aggiornato
        // (tutti in turnOrder, offering VUOTO)
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

        // 5. VERIFICA: turnOrderTrack DEVE contenere tutti e 3
        List<String> turnOrderAfter = model.getTurnOrderTrack();
        assertEquals(3, turnOrderAfter.size(),
                "turnOrderTrack deve avere 3 giocatori dopo nuovo StateChangedUpdate");
        assertTrue(turnOrderAfter.get(0).contains("Alice"),
                "Primo in turn order deve essere Alice");
    }

    /**
     * Test: TotemReturnedUpdate per un giocatore che ha GIA' PESCATO
     * (non è più nell'offeringTrack, è già in turnOrderTrack).
     * Verifica: nessun altro viene rimosso dall'offeringTrack.
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

        // 2. Alice pesca: arriva CardPickedUpdate con offering aggiornato (Alice rimossa)
        Map<Integer, String> afterPick = new TreeMap<>();
        afterPick.put(1, "[Tessera B | ...] -> Bob");
        afterPick.put(2, "[Tessera C | ...] -> Charlie");

        model.updateCardPicked("Alice", "upper", 0, 3, 0, afterPick);

        // Alice è stata rimossa dall'offering da CardPickedUpdate
        // Ora arriva TotemReturnedUpdate per Alice (dal server dopo executeEndTurnLogic)
        model.updateTotemReturned("Alice", 0, 3, 3);

        // 3. VERIFICA: Alice ora è in turnOrderTrack
        List<String> turnOrderAfter = model.getTurnOrderTrack();
        boolean aliceInTurnOrder = turnOrderAfter.stream().anyMatch(s -> s.contains("Alice"));
        assertTrue(aliceInTurnOrder, "Alice deve essere in turnOrderTrack dopo TotemReturnedUpdate");

        // 4. Simula disconnessione di Charlie (che è ancora sull'offeringTrack)
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 5. VERIFICA: Bob deve ancora essere sull'offeringTrack (Charlie rimosso, Alice già rimossa)
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve ANCORA essere sull'offeringTrack!");
        assertTrue(offeringAfter.get(1) != null && !offeringAfter.get(1).contains("Charlie"),
                "Charlie NON deve essere sull'offeringTrack");
        // Alice non c'è più (già rimossa da CardPickedUpdate), posizione 0 potrebbe non esistere
    }

    /**
     * Test: TotemReturnedUpdate per Alice che GIA' nel turnOrderTrack.
     * Simula: arriva updateTotemReturned per un giocatore che non è nell'offeringTrack.
     * Non deve rimuovere altri giocatori dall'offeringTrack.
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

        // Alice piazza il totem
        model.updateTotemPlacement("Alice", 0);
        // Bob piazza il totem
        model.updateTotemPlacement("Bob", 1);

        // Simula TotemReturned per Charlie (che NON è nell'offeringTrack)
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // Alice e Bob devono restare nell'offeringTrack
        Map<Integer, String> track = model.getOfferingTrack();
        assertTrue(track.get(0).contains("Alice"),
                "Alice deve restare nell'offeringTrack");
        assertTrue(track.get(1).contains("Bob"),
                "Bob deve restare nell'offeringTrack");
    }

    /**
     * Test: Sequenza reale — StateChangedUpdate poi TotemReturnedUpdate.
     * Simula il caso in cui un StateChangedUpdate arriva DOPO il
     * TotemReturnedUpdate (sovrascrive tutto).
     */
    @Test
    public void testStateChangedAfterTotemReturned_preservesCorrectState() {
        // 1. Place totem per tutti
        model.updateTotemPlacement("Alice", 0);
        model.updateTotemPlacement("Bob", 1);
        model.updateTotemPlacement("Charlie", 2);

        // 2. TotemReturned per Charlie (disconnessione)
        model.updateTotemReturned("Charlie", 2, 5, 3);

        // 3. StateChangedUpdate che arriva dopo (tipo giro successivo)
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

        // 4. Place totem di nuovo per Alice e Bob (nuovo round)
        model.updateTotemPlacement("Alice", 0);
        model.updateTotemPlacement("Bob", 1);

        // Charlie è disconnesso quindi non placeTotem.
        // Dopo i placeTotem: Alice e Bob devono essere sull'offeringTrack
        Map<Integer, String> offeringAfter = model.getOfferingTrack();
        assertTrue(offeringAfter.get(0).contains("Alice"),
                "Alice deve essere sull'offeringTrack nel nuovo round");
        assertTrue(offeringAfter.get(1).contains("Bob"),
                "Bob deve essere sull'offeringTrack nel nuovo round");

        // Charlie NON deve essere sull'offeringTrack (non ha piazzato)
        if (offeringAfter.containsKey(2)) {
            assertFalse(offeringAfter.get(2).contains("Charlie"),
                    "Charlie NON deve essere sull'offeringTrack nel nuovo round");
        }
    }
}
