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


}
