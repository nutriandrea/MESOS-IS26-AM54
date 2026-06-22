package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test dei messaggi di rete: costruttori, getter e dispatch polimorfico
 * ({@code applyTo} per gli Update, {@code execute} per le Request).
 * Gli Update vengono applicati a un ClientModel reale; le Request vengono
 * eseguite su un Controller reale (i suoi handler gestiscono internamente
 * eventuali errori, quindi le chiamate sono sicure).
 */
public class MessagesTest {

    // ------------------------------------------------------------------
    // UPDATE (SERVER -> CLIENT)
    // ------------------------------------------------------------------

    @Test
    public void testSimpleUpdatesGettersAndApplyTo() {
        ClientModel model = new ClientModel();

        PlayerJoinedUpdate joined = new PlayerJoinedUpdate("Alice");
        assertEquals("Alice", joined.getNickname());
        joined.applyTo(model);

        TotemColorUpdate color = new TotemColorUpdate("Alice", TotemColors.RED);
        assertEquals("Alice", color.getNickname());
        assertEquals(TotemColors.RED, color.getColor());
        color.applyTo(model);

        TotemPlacedUpdate placed = new TotemPlacedUpdate("Alice", 2);
        assertEquals(2, placed.getOfferPosition());
        assertEquals("Alice", placed.getNickname());
        placed.applyTo(model);

        TotemReturnedUpdate returned = new TotemReturnedUpdate("Alice", 1, 5, 7);
        assertEquals(1, returned.getPosition());
        assertEquals(5, returned.getNewFoodBalance());
        assertEquals(7, returned.getNewAuraBalance());
        assertEquals("Alice", returned.getNickname());
        returned.applyTo(model);

        TurnChangedUpdate turn = new TurnChangedUpdate("Bob");
        assertEquals("Bob", turn.getNickname());
        turn.applyTo(model);

        ErrorMessageUpdate err = new ErrorMessageUpdate("boom");
        assertEquals("boom", err.getMessage());
        err.applyTo(model);

        PlayerDisconnectedUpdate disc = new PlayerDisconnectedUpdate("Bob");
        assertEquals("Bob", disc.getNickname());
        disc.applyTo(model);

        PlayerReconnectedUpdate rec = new PlayerReconnectedUpdate("Bob");
        assertEquals("Bob", rec.getNickname());
        rec.applyTo(model);

        new GameSuspendedUpdate().applyTo(model);
        new GameResumedUpdate().applyTo(model);
    }

    @Test
    public void testCardPickedUpdate() {
        ClientModel model = new ClientModel();
        Map<Integer, String> offering = Map.of(0, "A|free");
        CardPickedUpdate cp = new CardPickedUpdate("Alice", "upper", 1, 4, 2, offering);
        assertEquals("Alice", cp.getNickname());
        assertEquals("upper", cp.getRow());
        assertEquals(1, cp.getCardIndex());
        assertEquals(4, cp.getNewFoodBalance());
        assertEquals(2, cp.getNewMagicBalance());
        assertEquals(offering, cp.getUpdatedOfferingTrack());
        cp.applyTo(model);
    }

    @Test
    public void testEndGameUpdate() {
        ClientModel model = new ClientModel();
        EndGameUpdate eg = new EndGameUpdate(List.of("Alice"), Map.of("Alice", 22, "Bob", 18), 2);
        assertEquals(List.of("Alice"), eg.getWinners());
        assertEquals(22, eg.getAllScores().get("Alice"));
        assertEquals(2, eg.getNumPlayers());
        eg.applyTo(model);
        assertEquals(Map.of("Alice", 22, "Bob", 18), model.getLastMatchScores());
    }

    @Test
    public void testStateChangedUpdate() {
        ClientModel model = new ClientModel();
        var snap = new StateChangedUpdate.PlayerSnapshot(3, 5, 1, TotemColors.BLUE,
                List.of("0|1|2|FARMER|1|0"), List.of());
        StateChangedUpdate sc = new StateChangedUpdate(
                "PickingState", 1, 2,
                Map.of("Alice", snap),
                List.of("Alice"),
                Map.of(0, "A|free"),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertEquals("PickingState", sc.getStateName());
        assertEquals(1, sc.getCurrentEra());
        assertEquals(2, sc.getCurrentRound());
        assertEquals(1, sc.getPlayersStats().size());
        assertEquals(List.of("Alice"), sc.getTurnOrderTrack());
        assertNotNull(sc.getOfferingTrack());
        assertNotNull(sc.getCharUpper());
        assertNotNull(sc.getCharLower());
        assertNotNull(sc.getEventUpper());
        assertNotNull(sc.getEventLower());
        assertNotNull(sc.getBuildUpper());
        assertNotNull(sc.getBuildLower());

        sc.applyTo(model);
        assertEquals("PickingState", model.getCurrentState());
        assertTrue(model.getPlayers().contains("Alice"));
    }

    @Test
    public void testEventUpdate() {
        ClientModel model = new ClientModel();
        var snap = new EventUpdate.PlayerSnapshot(3, 5, 1, List.of(), List.of());
        EventUpdate eu = new EventUpdate("HUNTER",
                Map.of("Alice", snap),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertEquals("HUNTER", eu.getEventName());
        assertEquals(1, eu.getPlayersStats().size());
        assertNotNull(eu.getCharUpper());
        assertNotNull(eu.getCharLower());
        assertNotNull(eu.getEventUpper());
        assertNotNull(eu.getEventLower());
        assertNotNull(eu.getBuildUpper());
        assertNotNull(eu.getBuildLower());
        eu.applyTo(model);
    }

    @Test
    public void testLeaderboardMessages() {
        ClientModel model = new ClientModel();
        LeaderboardEntry entry = new LeaderboardEntry("Alice", 50, 2, LocalDateTime.of(2026, 5, 1, 12, 0), 1);
        assertEquals("Alice", entry.getNickname());
        assertEquals(50, entry.getScore());
        assertEquals(2, entry.getNumPlayers());
        assertEquals(1, entry.getPosition());
        assertNotNull(entry.getMatchDate());

        LeaderboardEntry nullDate = new LeaderboardEntry("Bob", 10, 2, null, 2);
        assertNull(nullDate.getMatchDate());

        LeaderboardUpdate lu = new LeaderboardUpdate(1, List.of(entry), 2);
        assertEquals(1, lu.getPlayerPosition());
        assertEquals(1, lu.getEntries().size());
        assertEquals(2, lu.getNumPlayers());
        lu.applyTo(model);
        assertEquals(1, model.getLeaderboardEntries().size());
    }

    @Test
    public void testPingPongCarryTimestamp() {
        PingRequest ping = new PingRequest(123L);
        assertEquals(123L, ping.getTimestamp());
        ping.applyTo(new ClientModel()); // no-op, gestita a livello socket

        PongResponse pong = new PongResponse(456L);
        assertEquals(456L, pong.getTimestamp());
    }

    // ------------------------------------------------------------------
    // REQUEST (CLIENT -> SERVER) — execute() delega al Controller
    // ------------------------------------------------------------------

    @Test
    public void testRequestGettersAndExecute() {
        Game game = new Game();
        Controller controller = new Controller(game);
        ClientConnection conn = new NoopConnection();

        JoinGameRequest join = new JoinGameRequest("Alice");
        assertEquals("Alice", join.getNickname());
        assertDoesNotThrow(() -> join.execute(controller, "Alice", conn));

        ChooseTotemRequest choose = new ChooseTotemRequest(TotemColors.RED);
        assertEquals(TotemColors.RED, choose.getColor());
        assertDoesNotThrow(() -> choose.execute(controller, "Alice", conn));

        PlaceTotemRequest place = new PlaceTotemRequest(2);
        assertEquals(2, place.getOfferPosition());
        assertDoesNotThrow(() -> place.execute(controller, "Alice", conn));

        PickCardRequest pick = new PickCardRequest("upper", 1);
        assertEquals("upper", pick.getRow());
        assertEquals(1, pick.getCardIndex());
        assertDoesNotThrow(() -> pick.execute(controller, "Alice", conn));

        GetLeaderboardRequest lb = new GetLeaderboardRequest(3);
        assertEquals(3, lb.getNumPlayers());
        assertDoesNotThrow(() -> lb.execute(controller, "Alice", conn));

        assertDoesNotThrow(() -> new SetupGameRequest().execute(controller, "Alice", conn));
        assertDoesNotThrow(() -> new EndTurnRequest().execute(controller, "Alice", conn));
        assertDoesNotThrow(() -> new PongResponse(1L).execute(controller, "Alice", conn)); // no-op
    }

    /** Connessione di test che ignora ogni notifica. */
    private static class NoopConnection implements ClientConnection {
        public void notifyPlayerJoined(String n) {}
        public void notifyTotemColor(String n, TotemColors c) {}
        public void notifyTotemPlaced(String n, int p) {}
        public void notifyCardPicked(String n, String r, int i, int f, int m, Map<Integer, String> o) {}
        public void notifyTotemReturned(String n, int p, int f, int a) {}
        public void notifyEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> s,
                                        List<String> a, List<String> b, List<String> c, List<String> d,
                                        List<String> e, List<String> f) {}
        public void notifyTurnChanged(String n) {}
        public void notifyStateChanged(String s, int era, int round,
                                       Map<String, StateChangedUpdate.PlayerSnapshot> stats,
                                       List<String> to, Map<Integer, String> off,
                                       List<String> a, List<String> b, List<String> c,
                                       List<String> d, List<String> e, List<String> f) {}
        public void notifyEndGame(List<String> w, Map<String, Integer> s, int n) {}
        public void notifyError(String m) {}
        public void sendPing() {}
        public void sendLeaderboardUpdate(LeaderboardUpdate u) {}
        public void notifyPlayerDisconnected(String n) {}
        public void notifyPlayerReconnected(String n) {}
        public void disconnect() {}
    }
}
