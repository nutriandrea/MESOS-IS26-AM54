package it.polimi.ingsw.am54.integration;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.states.EndGameState;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import it.polimi.ingsw.am54.network.server.VirtualView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the missing end-of-game notification.
 *
 * EndGameState is BOTH auto-resolvable and game-over. The automatic-phase
 * resolver used to skip it (its guard was {@code isAutoResolvable && !isGameOver}),
 * so {@link EndGameState#resolvePhase} — the only place that calls
 * {@code game.notifyEndGame(...)} — never ran on a normal deck-exhaustion end.
 * Connected clients therefore never received an EndGameUpdate with the real
 * final scores and could only sort the raw round-10 aura, showing the wrong
 * winner and the wrong points on the final ranking.
 *
 * This test drives the controller's private automatic-phase resolution while
 * the game sits in EndGameState and asserts that a connected client receives a
 * single EndGameUpdate carrying every player's real final score and the correct
 * winner.
 */
public class EndGameNotifyScoresTest {

    @Test
    public void gameOverPushesFinalScoresAndWinnerToConnectedClients() throws Exception {
        Game game = new Game();
        game.setBoard(new Board());

        Player bubba = new Player();  bubba.setNickname("bubba");
        Player frabbo = new Player(); frabbo.setNickname("frabbo");
        game.getPlayers().add(bubba);
        game.getPlayers().add(frabbo);

        // bubba should win on aura even though, before the fix, the client would
        // only ever see the raw aura and could not be trusted to pick the winner.
        bubba.modifyAuraPoints(27);
        frabbo.modifyAuraPoints(22);

        game.setState(new EndGameState());

        Controller controller = new Controller(game);

        // Register a connected client exactly as a normal join would, but without
        // going through joinGame() (which restarts the match on EndGameState).
        TestClientConnection conn = new TestClientConnection();
        game.addObserver(new VirtualView(conn, "bubba"));

        // Trigger the server-side automatic resolution that fires at game-over.
        Method resolve = Controller.class.getDeclaredMethod("checkAndResolveAutomaticPhases");
        resolve.setAccessible(true);
        resolve.invoke(controller);

        List<EndGameUpdate> endGames = conn.received.stream()
                .filter(m -> m instanceof EndGameUpdate)
                .map(m -> (EndGameUpdate) m)
                .toList();

        assertEquals(1, endGames.size(),
                "A connected client must receive exactly one EndGameUpdate at game-over");

        EndGameUpdate update = endGames.get(0);
        assertEquals(List.of("bubba"), update.getWinners(),
                "The winner pushed to clients must be the real highest-score player");

        Map<String, Integer> scores = update.getAllScores();
        assertEquals(27, scores.get("bubba"),
                "Scores must be the real final scores, not the raw round-10 aura");
        assertEquals(22, scores.get("frabbo"),
                "Every player's real final score must be present");
    }

    private static class TestClientConnection implements ClientConnection {
        final List<Object> received = new CopyOnWriteArrayList<>();
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
        public void notifyEndGame(List<String> winners, Map<String, Integer> scores, int n) {
            received.add(new EndGameUpdate(winners, scores, n));
        }
        public void notifyError(String m) {}
        public void sendPing() {}
        public void sendLeaderboardUpdate(LeaderboardUpdate u) {}
        public void notifyPlayerDisconnected(String n) {}
        public void notifyPlayerReconnected(String n) {}
        public void disconnect() {}
    }
}
