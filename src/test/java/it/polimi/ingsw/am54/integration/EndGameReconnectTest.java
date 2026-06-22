package it.polimi.ingsw.am54.integration;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.states.EndGameState;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A player who is disconnected when the game ends must, upon reconnecting,
 * receive the final result (winner board) — it is captured at game-over time
 * and replayed on reconnect.
 */
public class EndGameReconnectTest {

    @Test
    public void reconnectingAfterGameOverReceivesWinner() throws Exception {
        Game game = new Game();
        game.setBoard(new Board());

        Player bubba = new Player(); bubba.setNickname("bubba");
        Player frabbo = new Player(); frabbo.setNickname("frabbo");
        game.getPlayers().add(bubba);
        game.getPlayers().add(frabbo);
        bubba.modifyAuraPoints(10); // bubba is the winner

        game.setState(new EndGameState());

        Controller controller = new Controller(game);

        // Simulate "game already ended": capture the final result as the real
        // end-of-game paths do (default win -> bubba).
        java.lang.reflect.Method capture =
                Controller.class.getDeclaredMethod("captureFinalResult", List.class);
        capture.setAccessible(true);
        capture.invoke(controller, Collections.singletonList(bubba));

        // frabbo was disconnected at game-over; now reconnects.
        TestClientConnection conn = new TestClientConnection();
        controller.handleReconnect("frabbo", conn);

        List<EndGameUpdate> endGames = conn.received.stream()
                .filter(m -> m instanceof EndGameUpdate)
                .map(m -> (EndGameUpdate) m)
                .toList();

        assertEquals(1, endGames.size(), "Reconnecting player must receive the final result exactly once");
        assertTrue(endGames.get(0).getWinners().contains("bubba"),
                "The replayed result must name bubba as winner");
        assertTrue(endGames.get(0).getAllScores().containsKey("bubba"),
                "Scores must include the winner even after they later leave");
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
