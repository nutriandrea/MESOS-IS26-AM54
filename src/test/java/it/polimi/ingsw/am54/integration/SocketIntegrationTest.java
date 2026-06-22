package it.polimi.ingsw.am54.integration;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.SocketServerConnection;
import it.polimi.ingsw.am54.network.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.Socket;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione end-to-end sul trasporto Socket TCP reale.
 *
 * Avvia un vero {@link Server} su una porta dedicata e collega due client socket
 * reali (ognuno con il proprio {@link ClientModel}). Esercita l'intera catena:
 * SocketServerConnection → SocketClientHandler → Controller → Game → VirtualView
 * → ritorno degli Update → ServerHandler → ClientModel.
 *
 * Le asserzioni sono volutamente lasche (verificano che i messaggi fluiscano nei
 * due sensi), perché la correttezza della logica è già coperta dai test unitari.
 */
public class SocketIntegrationTest {

    private static final int TCP_PORT = 25555;
    private static final int RMI_PORT = 25556;

    @AfterAll
    public static void cleanup() {
        new File("backup.json").delete();
        new File("backup.json.tmp").delete();
    }

    @Test
    public void testTwoClientsPlayOverSockets() throws Exception {
        // Avvio pulito: nessun backup da ripristinare
        new File("backup.json").delete();

        Server server = new Server();
        Thread serverThread = new Thread(() -> server.start(TCP_PORT, RMI_PORT));
        serverThread.setDaemon(true);
        serverThread.start();

        boolean portReady = waitUntil(() -> {
            try (Socket s = new Socket("localhost", TCP_PORT)) {
                return s.isConnected();
            } catch (Exception e) {
                return false;
            }
        }, 5000);
        Assumptions.assumeTrue(portReady, "Il server TCP non si è avviato sulla porta " + TCP_PORT);

        // --- Due client reali ---
        ClientModel m1 = new ClientModel();
        ClientModel m2 = new ClientModel();
        SocketServerConnection c1 = new SocketServerConnection("localhost", TCP_PORT, m1);
        SocketServerConnection c2 = new SocketServerConnection("localhost", TCP_PORT, m2);

        try {
            // 1. JOIN
            c1.joinGame("uno");
            assertTrue(waitUntil(() -> m1.getPlayers().contains("uno"), 3000),
                    "Il client 1 deve registrare il proprio join");
            c2.joinGame("due");
            assertTrue(waitUntil(() -> m1.getPlayers().size() >= 2 && m2.getPlayers().size() >= 2, 3000),
                    "Entrambi i client devono vedere 2 giocatori");

            // 2. SCELTA COLORE
            c1.chooseTotem(TotemColors.RED);
            c2.chooseTotem(TotemColors.BLUE);
            assertTrue(waitUntil(() -> m1.getPlayerColors().size() >= 2, 3000),
                    "Le scelte colore devono propagarsi");

            // 3. AVVIO PARTITA -> transizione di stato broadcastata
            c1.setupGame();
            assertTrue(waitUntil(() -> isInGame(m1.getCurrentState()) && isInGame(m2.getCurrentState()), 4000),
                    "Dopo il setup entrambi i client devono uscire dalla lobby");

            // 4. AZIONI DI GIOCO (inviate "alla cieca": il server valida e risponde,
            //    coprendo sia i percorsi validi sia gli ErrorMessageUpdate)
            c1.placeTotem(0);
            c2.placeTotem(1);
            sleep(200);
            c1.placeTotem(0);   // probabile errore: tessera occupata / non è il turno
            c1.pickCard("upper", 0);
            sleep(200);

            // 5. RICHIESTA CLASSIFICA -> LeaderboardUpdate di ritorno
            c1.requestLeaderboard(2);
            sleep(200);

            // 6. DISCONNESSIONE di un client -> il server lo rileva e lo propaga
            c2.disconnect();
            boolean disconnectSeen = waitUntil(
                    () -> m1.getDisconnectedPlayers().contains("due")
                            || "due".equalsIgnoreCase(firstDisconnected(m1)),
                    8000);

            // --- ASSERZIONI FINALI ---
            assertTrue(m1.getPlayers().contains("uno") && m1.getPlayers().contains("due"),
                    "Il client 1 deve conoscere entrambi i giocatori");
            assertTrue(m2.getPlayers().size() >= 2, "Il client 2 deve aver ricevuto lo stato");
            assertTrue(isInGame(m1.getCurrentState()),
                    "Il client 1 deve essere in partita, stato = " + m1.getCurrentState());
            assertTrue(disconnectSeen, "La disconnessione del client 2 deve propagarsi al client 1");
        } finally {
            c1.disconnect();
            try { c2.disconnect(); } catch (Exception ignored) {}
        }
    }

    private static boolean isInGame(String state) {
        return state != null && !state.isEmpty()
                && !"SETUPGAMESTATE".equalsIgnoreCase(state)
                && !"LOBBY".equalsIgnoreCase(state);
    }

    private static String firstDisconnected(ClientModel m) {
        return m.getDisconnectedPlayers().stream().findFirst().orElse(null);
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
