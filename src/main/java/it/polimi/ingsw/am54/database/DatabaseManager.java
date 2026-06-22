package it.polimi.ingsw.am54.database;

import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages the MySQL database connection (Aiven cloud) and all database
 * operations for persisting match results and querying the leaderboard.
 *
 * The connection is configured via a .env file (see .env.example).
 * Falls back to localhost if .env is missing.
 *
 * Table schema (auto-created on init):
 *   match_results (
 *       id          INT AUTO_INCREMENT PRIMARY KEY,
 *       nickname    VARCHAR(50)  NOT NULL,
 *       score       INTEGER      NOT NULL,
 *       match_date  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *       num_players INTEGER      NOT NULL
 *   )
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private final DatabaseConfig config;
    private boolean available;

    public DatabaseManager(DatabaseConfig config) {
        this.config = config;
        this.available = false;
    }

    /**
     * Initialises the database connection and creates the table if it does not exist.
     * Call once at server startup.
     */
    public void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS match_results (" +
                "    id          INT AUTO_INCREMENT PRIMARY KEY," +
                "    nickname    VARCHAR(50)  NOT NULL," +
                "    score       INTEGER      NOT NULL," +
                "    match_date  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "    num_players INTEGER      NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            this.available = true;
            LOGGER.info("Database connesso: " + config.getHost() + ":" + config.getPort() + "/" + config.getDbName());

            seedIfEmpty(conn);
        } catch (SQLException e) {
            LOGGER.warning("Database MySQL non raggiungibile – leaderboard disabilitata: " + e.getMessage());
            this.available = false;
        }
    }

    /**
     * Inserisce nutria 1312 pts per 2-5 players solo se la tabella è vuota (primo avvio).
     */
    private void seedIfEmpty(Connection conn) {
        try (Statement countStmt = conn.createStatement();
             ResultSet rs = countStmt.executeQuery("SELECT COUNT(*) FROM match_results")) {
            if (rs.next() && rs.getInt(1) == 0) {
                LOGGER.info("Database vuoto — inserimento dati seed...");
                String sql = "INSERT INTO match_results (nickname, score, match_date, num_players) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    LocalDateTime base = LocalDateTime.of(2026, 5, 22, 10, 0);
                    for (int np = 2; np <= 5; np++) {
                        ps.setString(1, "nutria");
                        ps.setInt(2, -1312);
                        ps.setTimestamp(3, Timestamp.valueOf(base.plusHours(np)));
                        ps.setInt(4, np);
                        ps.executeUpdate();
                    }
                }
                LOGGER.info("Seed completato: 4 entry per nutria (2-5 players) da 1312 pts.");
            }
        } catch (SQLException e) {
            LOGGER.warning("Seed fallito: " + e.getMessage());
        }
    }

    /**
     * Returns true if the database connection is working.
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Saves the result of a single completed player match.
     *
     * @param nickname   the player's nickname
     * @param score      the final score
     * @param numPlayers the number of players in that match
     */
    public void saveMatchResult(String nickname, int score, int numPlayers) {
        if (!available) return;
        String sql = "INSERT INTO match_results (nickname, score, match_date, num_players) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            ps.setInt(2, score);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, numPlayers);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("Failed to save match result for " + nickname + ": " + e.getMessage());
        }
    }

    /**
     * Returns the full leaderboard for matches with a given number of players,
     * ordered by score descending. Each entry includes the player's rank (position).
     *
     * @param numPlayers filter matches with this many players
     * @return list of leaderboard entries, ordered by score descending
     */
    public List<LeaderboardEntry> getLeaderboard(int numPlayers) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (!available) return entries;

        String sql =
            "SELECT nickname, score, match_date, num_players, " +
            "       RANK() OVER (ORDER BY score DESC) AS position " +
            "FROM match_results " +
            "WHERE num_players = ? " +
            "ORDER BY position, match_date DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numPlayers);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(
                        rs.getString("nickname"),
                        rs.getInt("score"),
                        rs.getInt("num_players"),
                        rs.getTimestamp("match_date").toLocalDateTime(),
                        rs.getInt("position")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to query leaderboard: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Returns ALL leaderboard entries regardless of numPlayers,
     * ordered by score descending. Used for the login-screen global leaderboard.
     */
    public List<LeaderboardEntry> getAllLeaderboardEntries() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (!available) return entries;

        String sql =
            "SELECT nickname, score, match_date, num_players, " +
            "       RANK() OVER (ORDER BY score DESC) AS position " +
            "FROM match_results " +
            "ORDER BY position, match_date DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(
                        rs.getString("nickname"),
                        rs.getInt("score"),
                        rs.getInt("num_players"),
                        rs.getTimestamp("match_date").toLocalDateTime(),
                        rs.getInt("position")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to query global leaderboard: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Finds the position of a specific player in the leaderboard.
     *
     * @param nickname   the player's nickname
     * @param numPlayers the number of players filter
     * @return position (1-based), or -1 if not found
     */
    public int getPlayerPosition(String nickname, int numPlayers) {
        if (!available) return -1;

        String sql =
            "SELECT position FROM (" +
            "    SELECT nickname, RANK() OVER (ORDER BY score DESC) AS position " +
            "    FROM match_results WHERE num_players = ?" +
            ") ranked WHERE nickname = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numPlayers);
            ps.setString(2, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("position");
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to get player position: " + e.getMessage());
        }
        return -1;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.getJdbcUrl(), config.getUser(), config.getPassword());
    }
}
