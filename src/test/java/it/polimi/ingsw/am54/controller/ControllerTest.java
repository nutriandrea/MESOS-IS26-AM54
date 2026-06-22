package it.polimi.ingsw.am54.controller;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.model.states.SetUpGameState;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del Controller: ciclo lobby → partita, gestione disconnessione/sospensione/
 * ripresa, classifica, abbandono in lobby e svuotamento della stanza.
 */
public class ControllerTest {

    private Game game;
    private Controller controller;
    private RecordingConnection c1;
    private RecordingConnection c2;

    @BeforeEach
    public void setUp() {
        game = new Game();
        controller = new Controller(game);
        c1 = new RecordingConnection();
        c2 = new RecordingConnection();
    }

    private void startTwoPlayerGame() {
        controller.handleJoinGame("uno", c1);
        controller.handleJoinGame("due", c2);
        controller.handleChooseTotemColor("uno", "RED");
        controller.handleChooseTotemColor("due", "BLUE");
        controller.handleSetupGame("uno");
    }

    @Test
    public void testGameStartsAndPlayerCount() {
        startTwoPlayerGame();
        assertEquals(2, controller.getPlayerCount());
        assertFalse(game.getCurrentState() instanceof SetUpGameState,
                "Dopo il setup non si è più in lobby");
    }

    @Test
    public void testDisconnectionSuspendsAndReconnectionResumes() {
        startTwoPlayerGame();

        // Un giocatore cade: resta 1 attivo -> sospensione
        controller.removeClient("due");
        assertTrue(c1.suspended.get(), "Il client rimasto deve ricevere la sospensione");

        // Il giocatore rientra: 2 attivi -> ripresa
        RecordingConnection c2b = new RecordingConnection();
        controller.handleJoinGame("due", c2b);
        assertTrue(c1.resumed.get() || c2b.resumed.get(), "La partita deve riprendere");
        assertEquals(2, controller.getPlayerCount());
    }

    @Test
    public void testEmptyRoomPurgesToLobby() {
        startTwoPlayerGame();
        controller.removeClient("due"); // sospende
        controller.removeClient("uno"); // stanza vuota -> purge

        assertTrue(game.getCurrentState() instanceof SetUpGameState,
                "Con la stanza vuota il gioco torna in lobby");
        assertEquals(0, controller.getPlayerCount());
    }

    @Test
    public void testLeaderboardRequestSendsUpdate() {
        startTwoPlayerGame();
        controller.handleGetLeaderboard(c1, "uno", 2);
        assertNotNull(c1.lastLeaderboard, "Il client deve ricevere un LeaderboardUpdate");
        assertEquals(2, c1.lastLeaderboard.getNumPlayers());
    }

    @Test
    public void testLeaveDuringLobbyFreesSlot() {
        controller.handleJoinGame("uno", c1);
        controller.handleJoinGame("due", c2);
        assertEquals(2, controller.getPlayerCount());

        // In lobby (SetUpGameState) la rimozione è permanente e libera lo slot
        controller.removeClient("due");
        assertEquals(1, controller.getPlayerCount());
    }

    @Test
    public void testRecoveryValidationAndDbManager() {
        startTwoPlayerGame();
        assertNotNull(controller.getDatabaseManager(), "Il DatabaseManager deve essere accessibile");
        // Ripristina il giocatore attivo in base alla fase corrente: non deve lanciare
        assertDoesNotThrow(() -> controller.validateGameStateAfterRecovery());
        assertNotNull(game.getCurrentPlayer());
    }

    @Test
    public void testDuplicateNicknameRejected() {
        controller.handleJoinGame("uno", c1);
        RecordingConnection dup = new RecordingConnection();
        controller.handleJoinGame("uno", dup);
        // Il duplicato riceve un errore e non viene aggiunto
        assertEquals(1, controller.getPlayerCount());
        assertNotNull(dup.lastError);
    }

    /** Connessione che registra le notifiche rilevanti per le asserzioni. */
    private static class RecordingConnection implements ClientConnection {
        final AtomicBoolean suspended = new AtomicBoolean(false);
        final AtomicBoolean resumed = new AtomicBoolean(false);
        final AtomicInteger heartbeats = new AtomicInteger(0);
        volatile LeaderboardUpdate lastLeaderboard;
        volatile String lastError;

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
        public void notifyError(String m) { this.lastError = m; }
        public void sendPing() {}
        public void sendLeaderboardUpdate(LeaderboardUpdate u) { this.lastLeaderboard = u; }
        public void notifyPlayerDisconnected(String n) {}
        public void notifyPlayerReconnected(String n) {}
        public void notifyGameSuspended() { suspended.set(true); }
        public void notifyGameResumed() { resumed.set(true); }
        public void startHeartbeat() { heartbeats.incrementAndGet(); }
        public void disconnect() {}
    }
}
