package it.polimi.ingsw.am54.database;

import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseConfig and DatabaseManager.
 * <p>
 * These tests do NOT require a running MySQL server.
 * They verify configuration parsing and graceful degradation
 * when the database is unreachable.
 */
class DatabaseManagerTest {

    private static final String PROP_HOST = "db.host";
    private static final String PROP_PORT = "db.port";
    private static final String PROP_NAME = "db.name";
    private static final String PROP_USER = "db.user";
    private static final String PROP_PASSWORD = "db.password";
    private static final String PROP_SSL_MODE = "db.sslMode";
    private static final String PROP_PARAMS = "db.params";

    @BeforeEach
    void clearSystemProperties() {
        // Ripristina i default per isolarsi dal file .env
        System.setProperty(PROP_HOST, "localhost");
        System.setProperty(PROP_PORT, "3306");
        System.setProperty(PROP_NAME, "am54");
        System.setProperty(PROP_USER, "root");
        System.setProperty(PROP_PASSWORD, "");
        System.setProperty(PROP_SSL_MODE, "PREFERRED");
        System.setProperty(PROP_PARAMS, "");
    }

    @AfterEach
    void restoreCleanState() {
        System.clearProperty(PROP_HOST);
        System.clearProperty(PROP_PORT);
        System.clearProperty(PROP_NAME);
        System.clearProperty(PROP_USER);
        System.clearProperty(PROP_PASSWORD);
        System.clearProperty(PROP_SSL_MODE);
        System.clearProperty(PROP_PARAMS);
    }

    // ──────────────────────────────────────────────
    // DatabaseConfig tests
    // ──────────────────────────────────────────────

    @Test
    void defaultConfig() {
        DatabaseConfig config = new DatabaseConfig();
        assertEquals("jdbc:mysql://localhost:3306/am54?sslMode=PREFERRED", config.getJdbcUrl());
        assertEquals("root", config.getUser());
        assertEquals("", config.getPassword());
    }

    @Test
    void customConfigViaSystemProperties() {
        System.setProperty(PROP_HOST, "db.example.com");
        System.setProperty(PROP_PORT, "4000");
        System.setProperty(PROP_NAME, "testdb");
        System.setProperty(PROP_USER, "admin");
        System.setProperty(PROP_PASSWORD, "secret");

        DatabaseConfig config = new DatabaseConfig();
        assertEquals("jdbc:mysql://db.example.com:4000/testdb?sslMode=PREFERRED", config.getJdbcUrl());
        assertEquals("admin", config.getUser());
        assertEquals("secret", config.getPassword());
    }

    @Test
    void partialCustomConfig() {
        System.setProperty(PROP_HOST, "192.168.1.1");
        // port, name, user, password all use defaults

        DatabaseConfig config = new DatabaseConfig();
        assertEquals("jdbc:mysql://192.168.1.1:3306/am54?sslMode=PREFERRED", config.getJdbcUrl());
        assertEquals("root", config.getUser());
        assertEquals("", config.getPassword());
    }

    @Test
    void customSslMode() {
        System.setProperty(PROP_SSL_MODE, "REQUIRED");

        DatabaseConfig config = new DatabaseConfig();
        assertEquals("jdbc:mysql://localhost:3306/am54?sslMode=REQUIRED", config.getJdbcUrl());
    }

    @Test
    void customExtraParams() {
        System.setProperty(PROP_PARAMS, "allowPublicKeyRetrieval=true");

        DatabaseConfig config = new DatabaseConfig();
        assertEquals("jdbc:mysql://localhost:3306/am54?sslMode=PREFERRED&allowPublicKeyRetrieval=true",
            config.getJdbcUrl());
    }

    @Test
    void sslModeAndExtraParams() {
        System.setProperty(PROP_SSL_MODE, "REQUIRED");
        System.setProperty(PROP_PARAMS, "allowPublicKeyRetrieval=true");

        DatabaseConfig config = new DatabaseConfig();
        assertEquals("jdbc:mysql://localhost:3306/am54?sslMode=REQUIRED&allowPublicKeyRetrieval=true",
            config.getJdbcUrl());
    }

    // ──────────────────────────────────────────────
    // DatabaseManager graceful degradation tests
    // ──────────────────────────────────────────────

    @Test
    void initFailsWhenMySQLUnavailable() {
        // Default config points to localhost:3306 which should not have MySQL running in tests
        DatabaseConfig config = new DatabaseConfig();
        DatabaseManager manager = new DatabaseManager(config);
        manager.init();
        assertFalse(manager.isAvailable(),
            "DatabaseManager should report unavailable when MySQL is not reachable");
    }

    @Test
    void saveMatchResultDoesNotThrowWhenUnavailable() {
        DatabaseConfig config = new DatabaseConfig();
        DatabaseManager manager = new DatabaseManager(config);
        manager.init();

        // Should not throw any exception — just log a warning
        assertDoesNotThrow(() -> manager.saveMatchResult("Player1", 100, 4));
    }

    @Test
    void getLeaderboardReturnsEmptyWhenUnavailable() {
        DatabaseConfig config = new DatabaseConfig();
        DatabaseManager manager = new DatabaseManager(config);
        manager.init();

        List<LeaderboardEntry> entries = manager.getLeaderboard(4);
        assertNotNull(entries);
        assertTrue(entries.isEmpty(), "Leaderboard should be empty when DB unavailable");
    }

    @Test
    void getPlayerPositionReturnsMinusOneWhenUnavailable() {
        DatabaseConfig config = new DatabaseConfig();
        DatabaseManager manager = new DatabaseManager(config);
        manager.init();

        int position = manager.getPlayerPosition("Player1", 4);
        assertEquals(-1, position, "Position should be -1 when DB unavailable");
    }

    // ──────────────────────────────────────────────
    // LeaderboardEntry tests
    // ──────────────────────────────────────────────

    @Test
    void leaderboardEntryCreation() {
        LocalDateTime matchDate = LocalDateTime.of(2025, 5, 18, 10, 30, 0);
        LeaderboardEntry entry = new LeaderboardEntry("Player1", 100, 4, matchDate, 1);

        assertEquals("Player1", entry.getNickname());
        assertEquals(100, entry.getScore());
        assertEquals(4, entry.getNumPlayers());
        assertEquals(1, entry.getPosition());
        assertEquals("2025-05-18T10:30", entry.getMatchDate());
    }

    @Test
    void leaderboardEntryNullDate() {
        LeaderboardEntry entry = new LeaderboardEntry("Player2", 50, 2, null, 3);

        assertEquals("Player2", entry.getNickname());
        assertEquals(50, entry.getScore());
        assertEquals(2, entry.getNumPlayers());
        assertEquals(3, entry.getPosition());
        assertNull(entry.getMatchDate(), "Match date should be null when constructed with null");
    }

    @Test
    void leaderboardEntryNegativePosition() {
        LeaderboardEntry entry = new LeaderboardEntry("Alice", 0, 4, LocalDateTime.now(), -1);
        assertEquals(-1, entry.getPosition(), "Position can be -1 (player not found)");
        assertEquals(0, entry.getScore(), "Score can be 0");
    }
}
