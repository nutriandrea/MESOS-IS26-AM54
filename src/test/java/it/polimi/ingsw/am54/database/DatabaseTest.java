package it.polimi.ingsw.am54.database;

import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del layer database. Verifica la formattazione dell'URL JDBC e il
 * comportamento sicuro del DatabaseManager quando il database non è disponibile
 * (senza chiamare init(), così non viene toccato alcun database reale).
 */
public class DatabaseTest {

    @Test
    public void testJdbcUrlFormat() {
        DatabaseConfig config = new DatabaseConfig();
        String url = config.getJdbcUrl();

        assertTrue(url.startsWith("jdbc:postgresql://"), "URL PostgreSQL: " + url);
        assertTrue(url.contains("sslmode="), "Deve includere il parametro sslmode: " + url);
        assertNotNull(config.getUser());
        assertNotNull(config.getDbName());
        assertNotNull(config.getHost());
        assertTrue(config.getPort() > 0, "La porta deve essere valida");
    }

    @Test
    public void testManagerSafeWhenUnavailable() {
        // Nessuna init() -> available resta false: tutti i metodi devono degradare con grazia
        DatabaseManager db = new DatabaseManager(new DatabaseConfig());
        assertFalse(db.isAvailable(), "Senza init() il DB non è disponibile");

        List<LeaderboardEntry> board = db.getLeaderboard(2);
        assertNotNull(board);
        assertTrue(board.isEmpty(), "Classifica vuota quando il DB non è disponibile");

        assertTrue(db.getAllLeaderboardEntries().isEmpty());
        assertEquals(-1, db.getPlayerPosition("Alice", 2), "Posizione -1 quando non disponibile");

        // saveMatchResult deve essere un no-op silenzioso (nessuna eccezione, nessuna connessione)
        assertDoesNotThrow(() -> db.saveMatchResult("Alice", 42, 2));
    }
}
