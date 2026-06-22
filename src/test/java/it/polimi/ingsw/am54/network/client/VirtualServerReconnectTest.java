package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the reconnection routing bug: after an auto-reconnect swaps a new live
 * connection into the Client singleton, VirtualServer must send actions to the
 * NEW connection, not the (dead) one captured at construction time.
 */
public class VirtualServerReconnectTest {

    /** Minimal fake connection that just counts the picks it receives. */
    private static class CountingConnection implements ServerConnection {
        final AtomicInteger picks = new AtomicInteger();
        public void joinGame(String nickname) {}
        public void setupGame() {}
        public void chooseTotem(TotemColors color) {}
        public void placeTotem(int position) {}
        public void pickCard(String row, int index) { picks.incrementAndGet(); }
        public void endTurn() {}
        public void requestLeaderboard(int numPlayers) {}
        public void disconnect() {}
    }

    @Test
    public void pickGoesToLiveConnectionAfterReconnect() {
        Client.resetClient();
        Client client = Client.getInstance();

        CountingConnection oldConn = new CountingConnection();
        CountingConnection newConn = new CountingConnection();

        // Initial connection, as MainClient does.
        client.setConnection(oldConn);
        VirtualServer server = new VirtualServer(oldConn, "tester");

        server.pickCard("upper", 0);
        assertEquals(1, oldConn.picks.get(), "First pick must reach the original connection");

        // Simulate an auto-reconnect: the Client singleton now holds a NEW connection.
        client.setConnection(newConn);

        server.pickCard("upper", 0);
        assertEquals(1, oldConn.picks.get(), "Dead connection must NOT receive picks after reconnect");
        assertEquals(1, newConn.picks.get(), "Pick after reconnect must reach the NEW live connection");

        Client.resetClient();
    }
}
