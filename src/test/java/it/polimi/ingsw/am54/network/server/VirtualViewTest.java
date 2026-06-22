package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.network.messages.EventUpdate;
import it.polimi.ingsw.am54.network.messages.LeaderboardUpdate;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test della VirtualView: come observer del Game, traduce ogni evento del modello
 * in una notifica sulla ClientConnection costruendo i DTO di rete.
 */
public class VirtualViewTest {

    private Game game;
    private Board board;
    private Player p1;
    private RecordingConnection rec;
    private VirtualView vv;

    @BeforeEach
    public void setUp() {
        game = new Game();
        board = new Board();
        game.setBoard(board);

        p1 = new Player();
        p1.setNickname("uno");
        p1.setTotemColor(TotemColors.RED);
        p1.modifyFood(4);
        p1.modifyAuraPoints(6);
        p1.modifyMagicPoints(2);
        p1.getMyTribe().add(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1));
        p1.getBuildings().add(building());
        game.getPlayers().add(p1);

        // Board popolata per i DTO completi
        board.addCharacterCard(new CharacterCard(1, JobType.HUNTER, 0, null, new ArrayList<>(), 1), "upper");
        board.addCharacterCard(new CharacterCard(1, JobType.ARTIST, 0, null, new ArrayList<>(), 1), "lower");
        board.addEventCard(event(), "upper");
        board.addBuildingCard(building(), "upper");
        board.getOfferingTrack().add(new OfferingTile('A', 1, 1, 0));
        TurnOrderSlot slot = new TurnOrderSlot(1, 2);
        slot.occupy(p1);
        board.getTurnOrderTile().add(slot);

        rec = new RecordingConnection();
        vv = new VirtualView(rec, "uno");
    }

    private BuildingCard building() {
        BuildingCard b = new BuildingCard(JobType.HUNTER, 2, 3, null, new ArrayList<>(), 1);
        b.setName("Capanna");
        return b;
    }

    private EventCard event() {
        EventCard e = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0);
        e.setName("HUNTER");
        return e;
    }

    @Test
    public void testSimpleObserverNotifications() {
        assertEquals("uno", vv.getNickname());

        vv.onPlayerJoined(game, p1);
        assertEquals("uno", rec.joinedNick);

        vv.onTotemColorChosen(game, p1, TotemColors.RED);
        assertEquals(TotemColors.RED, rec.color);

        vv.onTotemPlaced(game, p1, 0);
        assertEquals(0, rec.totemPos);

        vv.onTotemReturned(game, p1, 1, 4, 6);
        assertEquals(1, rec.returnedPos);

        vv.onTurnChanged(game, p1);
        assertEquals("uno", rec.turnNick);
        vv.onTurnChanged(game, null);
        assertNull(rec.turnNick, "Tra una fase e l'altra il turno può essere null");
    }

    @Test
    public void testOnCardPickedBuildsOfferingSnapshot() {
        vv.onCardPicked(game, p1, "upper", 0, 5, 1);
        assertEquals("uno", rec.cardNick);
        assertNotNull(rec.cardOffering);
        assertFalse(rec.cardOffering.isEmpty(), "Lo snapshot delle offerte deve essere generato");
    }

    @Test
    public void testOnStateChangedBuildsFullSnapshot() {
        vv.onStateChanged(game, new PickingState());

        assertEquals("PickingState", rec.stateName);
        assertTrue(rec.stateStats.containsKey("uno"));
        assertEquals(1, rec.stateOffering.size());
        assertEquals(1, rec.stateTurnOrder.size());
        assertEquals(1, rec.charUp.size());
        assertEquals(1, rec.charLow.size());
        assertEquals(1, rec.eventUp.size());
        assertEquals(1, rec.buildUp.size());
    }

    @Test
    public void testOnStateChangedWithNullBoardIsSafe() {
        Game noBoard = new Game();
        noBoard.getPlayers().add(p1);
        VirtualView v = new VirtualView(rec, "uno");

        v.onStateChanged(noBoard, new PickingState());
        assertEquals("PickingState", rec.stateName);
        assertTrue(rec.stateTurnOrder.isEmpty(), "Senza board le strutture restano vuote");
    }

    @Test
    public void testOnEventResolvedBuildsSnapshot() {
        vv.onEventResolved(game, event());
        assertEquals("HUNTER - ERA: 1", rec.eventName);
        assertTrue(rec.eventStats.containsKey("uno"));
    }

    @Test
    public void testOnEndGameComputesScores() {
        vv.onEndGame(game, List.of(p1));
        assertEquals(List.of("uno"), rec.endWinners);
        assertTrue(rec.endScores.containsKey("uno"));
        assertEquals(1, rec.endNumPlayers);
    }

    @Test
    public void testDirectSignals() {
        vv.showError("errore");
        assertEquals("errore", rec.error);
        vv.showPlayerDisconnected("due");
        assertEquals("due", rec.disconnected);
        vv.showPlayerReconnected("due");
        assertEquals("due", rec.reconnected);
        vv.showGameSuspended();
        assertTrue(rec.suspended);
        vv.showGameResumed();
        assertTrue(rec.resumed);
    }

    /** ClientConnection che cattura l'ultimo valore ricevuto per ogni notifica. */
    private static class RecordingConnection implements ClientConnection {
        String joinedNick, turnNick, cardNick, stateName, eventName, error, disconnected, reconnected;
        TotemColors color;
        int totemPos, returnedPos, endNumPlayers;
        boolean suspended, resumed;
        Map<Integer, String> cardOffering, stateOffering;
        Map<String, StateChangedUpdate.PlayerSnapshot> stateStats;
        Map<String, EventUpdate.PlayerSnapshot> eventStats;
        List<String> stateTurnOrder, charUp, charLow, eventUp, buildUp, endWinners;
        Map<String, Integer> endScores;

        public void notifyPlayerJoined(String n) { joinedNick = n; }
        public void notifyTotemColor(String n, TotemColors c) { color = c; }
        public void notifyTotemPlaced(String n, int p) { totemPos = p; }
        public void notifyCardPicked(String n, String r, int i, int f, int m, Map<Integer, String> o) {
            cardNick = n; cardOffering = o;
        }
        public void notifyTotemReturned(String n, int p, int f, int a) { returnedPos = p; }
        public void notifyEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> s,
                                        List<String> a, List<String> b, List<String> c, List<String> d,
                                        List<String> e, List<String> f) {
            eventName = name; eventStats = s;
        }
        public void notifyTurnChanged(String n) { turnNick = n; }
        public void notifyStateChanged(String s, int era, int round,
                                       Map<String, StateChangedUpdate.PlayerSnapshot> stats,
                                       List<String> to, Map<Integer, String> off,
                                       List<String> a, List<String> b, List<String> c,
                                       List<String> d, List<String> e, List<String> f) {
            stateName = s; stateStats = stats; stateTurnOrder = to; stateOffering = off;
            charUp = a; charLow = b; eventUp = c; buildUp = e;
        }
        public void notifyEndGame(List<String> w, Map<String, Integer> s, int n) {
            endWinners = w; endScores = s; endNumPlayers = n;
        }
        public void notifyError(String m) { error = m; }
        public void sendPing() {}
        public void sendLeaderboardUpdate(LeaderboardUpdate u) {}
        public void notifyPlayerDisconnected(String n) { disconnected = n; }
        public void notifyPlayerReconnected(String n) { reconnected = n; }
        public void notifyGameSuspended() { suspended = true; }
        public void notifyGameResumed() { resumed = true; }
        public void disconnect() {}
    }
}
