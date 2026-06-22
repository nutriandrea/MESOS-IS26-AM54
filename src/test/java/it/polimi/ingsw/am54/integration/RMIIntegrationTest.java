package it.polimi.ingsw.am54.integration;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.RMIServerConnection;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;
import it.polimi.ingsw.am54.network.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione end-to-end sul trasporto RMI reale.
 *
 * Avvia un vero {@link Server} (registry RMI su porta dedicata) e collega due
 * client RMI reali ({@link RMIServerConnection}). Esercita la catena RMI:
 * RMIServerConnection → RMIClientHandler → Controller → Game → VirtualView →
 * callback RMI → ClientModel.
 */
public class RMIIntegrationTest {

    private static final int TCP_PORT = 25557; // non usato dai client RMI, ma Server avvia anche il TCP
    private static final int RMI_PORT = 25558;

    @AfterAll
    public static void cleanup() {
        new File("backup.json").delete();
        new File("backup.json.tmp").delete();
    }

    @Test
    public void testTwoClientsPlayOverRmi() throws Exception {
        new File("backup.json").delete();

        Server server = new Server();
        Thread serverThread = new Thread(() -> server.start(TCP_PORT, RMI_PORT));
        serverThread.setDaemon(true);
        serverThread.start();

        RMIGameServer serverStub = lookupWithRetry(5000);
        Assumptions.assumeTrue(serverStub != null, "Il registry RMI non si è avviato sulla porta " + RMI_PORT);

        ClientModel m1 = new ClientModel();
        ClientModel m2 = new ClientModel();
        RMIServerConnection c1 = new RMIServerConnection(serverStub, m1);
        RMIServerConnection c2 = new RMIServerConnection(serverStub, m2);

        try {
            // JOIN
            c1.joinGame("uno");
            assertTrue(waitUntil(() -> m1.getPlayers().contains("uno"), 3000));
            c2.joinGame("due");
            assertTrue(waitUntil(() -> m1.getPlayers().size() >= 2 && m2.getPlayers().size() >= 2, 3000),
                    "Entrambi i client RMI devono vedere 2 giocatori");

            // COLORE
            c1.chooseTotem(TotemColors.RED);
            c2.chooseTotem(TotemColors.BLUE);
            assertTrue(waitUntil(() -> m1.getPlayerColors().size() >= 2, 3000));

            // AVVIO
            c1.setupGame();
            assertTrue(waitUntil(() -> isInGame(m1.getCurrentState()) && isInGame(m2.getCurrentState()), 4000),
                    "Dopo il setup i client RMI devono entrare in partita");

            // AZIONI (alla cieca: il server valida e risponde via callback)
            c1.placeTotem(0);
            c2.placeTotem(1);
            sleep(200);
            c1.placeTotem(0);       // probabile errore -> onErrorMessage
            c1.pickCard("upper", 0);
            c1.requestLeaderboard(2);
            sleep(200);

            // DISCONNESSIONE esplicita di un client
            c2.disconnect();
            boolean disconnectSeen = waitUntil(() -> m1.getDisconnectedPlayers().contains("due"), 8000);

            assertTrue(m1.getPlayers().contains("uno") && m1.getPlayers().contains("due"));
            assertTrue(isInGame(m1.getCurrentState()), "Stato client 1 = " + m1.getCurrentState());
            assertTrue(disconnectSeen, "La disconnessione RMI del client 2 deve propagarsi");
        } finally {
            c1.disconnect();
            try { c2.disconnect(); } catch (Exception ignored) {}
        }
    }

    private static RMIGameServer lookupWithRetry(long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                Registry reg = LocateRegistry.getRegistry("localhost", RMI_PORT);
                return (RMIGameServer) reg.lookup("AM54-Server");
            } catch (Exception e) {
                sleep(100);
            }
        }
        return null;
    }

    private static boolean isInGame(String state) {
        return state != null && !state.isEmpty()
                && !"SETUPGAMESTATE".equalsIgnoreCase(state)
                && !"LOBBY".equalsIgnoreCase(state);
    }

    private static boolean waitUntil(BooleanSupplier cond, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (cond.getAsBoolean()) return true;
            sleep(40);
        }
        return cond.getAsBoolean();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
