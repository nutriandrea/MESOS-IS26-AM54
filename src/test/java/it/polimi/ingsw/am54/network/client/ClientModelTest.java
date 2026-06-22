package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.EventUpdate;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del modello locale lato client (ClientModel): applicazione degli update,
 * gestione disconnessioni/riconnessioni, messaggi di sistema e fine partita.
 */
public class ClientModelTest {

    private ClientModel model;

    @BeforeEach
    public void setUp() {
        model = new ClientModel();
    }

    private StateChangedUpdate stateWith(List<String> charUpper, List<String> buildUpper,
                                         List<String> charLower, Map<Integer, String> offering) {
        var snap = new StateChangedUpdate.PlayerSnapshot(5, 3, 1, TotemColors.RED, List.of(), List.of());
        return new StateChangedUpdate("PickingState", 1, 1,
                Map.of("uno", snap),
                List.of("uno"),
                offering,
                charUpper, charLower, List.of(), List.of(), buildUpper, List.of());
    }

    @Test
    public void testBasicUpdates() {
        model.updatePlayerJoined("uno");
        assertTrue(model.getPlayers().contains("uno"));

        model.updateTotemColor("uno", TotemColors.YELLOW);
        assertEquals(TotemColors.YELLOW, model.getPlayerColors().get("uno"));

        model.updateTurn("uno");
        assertEquals("uno", model.getCurrentPlayer());
    }

    @Test
    public void testUpdateStatePopulatesEverything() {
        model.updateState(stateWith(List.of("c0", "c1"), List.of("b0"), List.of(), Map.of(0, "A -> uno")));
        assertEquals("PickingState", model.getCurrentState());
        assertEquals(1, model.getCurrentEra());
        assertTrue(model.getPlayers().contains("uno"));
        assertEquals(2, model.getCharacterUpperRow().size());
        assertEquals(1, model.getUpperBuildingRow().size());
        assertEquals(5, model.getPlayerFood().get("uno"));
        assertEquals(3, model.getPlayerAura().get("uno"));
    }

    @Test
    public void testUpdateCardPickedCharacterAndBuilding() {
        model.updateState(stateWith(
                new ArrayList<>(List.of("char0", "char1")),
                new ArrayList<>(List.of("build0")),
                new ArrayList<>(List.of("low0")),
                Map.of(0, "A -> uno")));

        // Pesca personaggio upper index 0
        model.updateCardPicked("uno", "upper", 0, 4, 1, Map.of(0, "A -> VUOTO"));
        assertTrue(model.getPlayerTribe().get("uno").contains("char0"));
        assertEquals(4, model.getPlayerFood().get("uno"));
        assertEquals(1, model.getPlayerMagic().get("uno"));

        // Ora upper ha solo "char1"; index 1 punta all'edificio "build0"
        model.updateCardPicked("uno", "upper", 1, 3, 1, Map.of(0, "A -> VUOTO"));
        assertTrue(model.getPlayerBuildings().get("uno").contains("build0"));

        // Pesca personaggio lower index 0
        model.updateCardPicked("uno", "lower", 0, 3, 1, Map.of(0, "A -> VUOTO"));
        assertTrue(model.getPlayerTribe().get("uno").contains("low0"));
    }

    @Test
    public void testUpdateTotemPlacementAndReturned() {
        model.updateState(stateWith(List.of(), List.of(), List.of(),
                new java.util.HashMap<>(Map.of(0, "A | Cibo: +1 -> VUOTO"))));
        // popola turnOrderTrack con "uno"
        model.getTurnOrderTrack().add("uno");

        model.updateTotemPlacement("uno", 0);
        assertTrue(model.getOfferingTrack().get(0).contains("uno"));

        model.updateTotemReturned("uno", 7, 9);
        assertEquals(7, model.getPlayerFood().get("uno"));
        assertEquals(9, model.getPlayerAura().get("uno"));
    }

    @Test
    public void testDisconnectionLifecycle() {
        model.updatePlayerDisconnected("bob");
        assertTrue(model.isPlayerDisconnected("bob"));
        assertTrue(model.getDisconnectedPlayers().contains("bob"));

        model.updatePlayerReconnected("bob");
        assertFalse(model.isPlayerDisconnected("bob"));
        assertNotNull(model.getLastSystemMessage());
    }

    @Test
    public void testSuspendResumeAndSystemMessages() {
        model.updateGameSuspended();
        assertTrue(model.getLastSystemMessage().contains("SOSPESO"));

        model.updateGameResumed();
        assertTrue(model.getLastSystemMessage().contains("RIPRESO"));

        model.triggerSystemMessage("ciao");
        assertEquals("ciao", model.getLastSystemMessage());
        model.clearSystemMessage();
        assertNull(model.getLastSystemMessage());
    }

    @Test
    public void testErrorMessageLifecycle() {
        model.updateError("mossa illegale");
        assertEquals("mossa illegale", model.getLastErrorMessage());
        model.clearError();
        assertNull(model.getLastErrorMessage());
    }

    @Test
    public void testConnectionLostAndReconnectingFlag() {
        final boolean[] lost = {false};
        model.setOnConnectionLostCallback(() -> lost[0] = true);
        model.triggerConnectionLost();
        assertTrue(lost[0], "Il callback di connessione persa deve scattare");
        assertTrue(model.getLastSystemMessage().contains("CONNESSIONE"));

        final boolean[] reconnChanged = {false};
        model.setOnReconnectingChangedCallback(() -> reconnChanged[0] = true);
        assertFalse(model.isReconnecting());
        model.setReconnectingStatus(true);
        assertTrue(model.isReconnecting());
        assertTrue(reconnChanged[0]);
    }

    @Test
    public void testEndGameImmediate() {
        model.updateEndGame(List.of("uno"), Map.of("uno", 30, "due", 20), 2);
        assertEquals(List.of("uno"), model.getWinners());
        assertEquals(30, model.getLastMatchScores().get("uno"));
        assertEquals(2, model.getLastMatchNumPlayers());
    }

    @Test
    public void testEndGameDeferredBehindPendingEvents() {
        // Registra un callback eventi e produce un report pendente
        model.setOnEventResolvedCallback(reports -> { /* consumato dalla UI */ });

        var snap = new EventUpdate.PlayerSnapshot(5, 3, 1, List.of(), List.of());
        EventUpdate ev = new EventUpdate("HUNTER", Map.of("uno", snap),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        model.updateEventSnapshot(ev);

        // Con eventi pendenti, l'endgame viene comunque registrato nei vincitori
        model.updateEndGame(List.of("uno"), Map.of("uno", 27), 2);
        assertEquals(List.of("uno"), model.getWinners());
        assertEquals(27, model.getLastMatchScores().get("uno"));
    }

    @Test
    public void testEventSnapshotUpdatesResources() {
        var snap = new EventUpdate.PlayerSnapshot(8, 6, 2, List.of("t0"), List.of("b0"));
        EventUpdate ev = new EventUpdate("FARMER", Map.of("uno", snap),
                List.of("c0"), List.of(), List.of(), List.of(), List.of(), List.of());
        model.updateEventSnapshot(ev);

        assertEquals(8, model.getPlayerFood().get("uno"));
        assertEquals(6, model.getPlayerAura().get("uno"));
        assertEquals(2, model.getPlayerMagic().get("uno"));
        assertEquals(List.of("c0"), model.getCharacterUpperRow());
    }
}
