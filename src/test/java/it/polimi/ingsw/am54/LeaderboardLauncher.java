package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.database.DatabaseConfig;
import it.polimi.ingsw.am54.database.DatabaseManager;
import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Launcher di test per la classifica (modalità CLI e GUI).
 * <p>
 * Permette di verificare il funzionamento della leaderboard
 * senza dover avviare una partita completa.
 * <p>
 * Utilizzo:
 * <pre>
 *   # Modalità CLI (default): stampa la classifica formattata nel terminale
 *   ./mvnw compile test-compile exec:java \
 *     -Dexec.mainClass="it.polimi.ingsw.am54.LeaderboardLauncher" \
 *     -Dexec.classpathScope=test \
 *     -Dexec.args="--cli"
 *
 *   # Modalità GUI: istruzioni per aprire la finestra JavaFX
 *   ./mvnw compile test-compile exec:java \
 *     -Dexec.mainClass="it.polimi.ingsw.am54.LeaderboardLauncher" \
 *     -Dexec.classpathScope=test \
 *     -Dexec.args="--gui"
 * </pre>
 * <p>
 * Il launcher carica automaticamente le credenziali DB dal file .env
 * (se presente nella directory di lavoro) tramite DatabaseConfig.
 * Inserisce dati di test nel database, li legge e li visualizza.
 * Se il database non è raggiungibile, utilizza dati finti (mock).
 */
public class LeaderboardLauncher {

    private static final String SEP = "+" + "-".repeat(4) + "+" + "-".repeat(22) + "+" + "-".repeat(8) + "+" + "-".repeat(22) + "+" + "-".repeat(10) + "+" + "-".repeat(6) + "+";

    /** Prefisso per i nickname di test (per pulire i dati alla fine). */
    private static final String TEST_PREFIX = "TEST_";

    public static void main(String[] args) {
        boolean guiMode = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--gui") || arg.equalsIgnoreCase("-g")) {
                guiMode = true;
            }
        }

        System.out.println("=== Leaderboard Launcher ===");
        System.out.println();

        // 1. Ottieni i dati della classifica (da DB o mock)
        List<LeaderboardEntry> entries = getLeaderboardData();

        if (entries.isEmpty()) {
            System.out.println("Nessun dato disponibile per la classifica.");
            return;
        }

        // 2. Mostra i dati
        if (guiMode) {
            showGui(entries);
        } else {
            showCli(entries);
        }

        // 3. Verifica finale
        System.out.println();
        System.out.println("=== Leaderboard Launcher completato ===");
        System.out.println("Record visualizzati: " + entries.size());
    }

    /**
     * Recupera i dati della classifica dal database MySQL.
     * Se connesso, inserisce dati di test → legge classifica → pulisce.
     * Se non raggiungibile, usa dati mock.
     */
    private static List<LeaderboardEntry> getLeaderboardData() {
        try {
            DatabaseConfig config = new DatabaseConfig();
            System.out.println("Connessione a: " + config.getJdbcUrl());
            System.out.println("  User: " + config.getUser());

            // Connessione diretta per inserire dati di test
            Connection conn = DriverManager.getConnection(
                config.getJdbcUrl(), config.getUser(), config.getPassword());

            System.out.println("✓ Database raggiunto!");
            System.out.println();

            // Crea tabella se non esiste
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS match_results (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nickname VARCHAR(50) NOT NULL," +
                    "score INTEGER NOT NULL," +
                    "match_date TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "num_players INTEGER NOT NULL)");
            }

            // Pulisci vecchi dati di test
            try (Statement st = conn.createStatement()) {
                int deleted = st.executeUpdate("DELETE FROM match_results WHERE nickname LIKE '" + TEST_PREFIX + "%'");
                if (deleted > 0) {
                    System.out.println("  Puliti " + deleted + " vecchi record di test");
                }
            }

            // Inserisci dati di test
            LocalDateTime now = LocalDateTime.now();
            String[][] testData = {
                {TEST_PREFIX + "Luna",    "120", "4"},
                {TEST_PREFIX + "Sole",    "100", "4"},
                {TEST_PREFIX + "Stella",   "85", "4"},
                {TEST_PREFIX + "Nube",     "70", "4"},
                {TEST_PREFIX + "Thor",    "150", "3"},
                {TEST_PREFIX + "Odin",    "130", "3"},
                {TEST_PREFIX + "Freya",   "110", "3"},
                {TEST_PREFIX + "Apollo",   "90", "2"},
                {TEST_PREFIX + "Artemis",  "80", "2"},
                {TEST_PREFIX + "Zeus",    "200", "5"},
                {TEST_PREFIX + "Era",     "180", "5"},
                {TEST_PREFIX + "Poseidon","160", "5"},
                {TEST_PREFIX + "Ade",     "140", "5"},
                {TEST_PREFIX + "Estia",   "120", "5"},
            };

            try (Statement st = conn.createStatement()) {
                for (String[] row : testData) {
                    st.addBatch("INSERT INTO match_results (nickname, score, num_players) VALUES ('"
                        + row[0] + "', " + row[1] + ", " + row[2] + ")");
                }
                int[] results = st.executeBatch();
                System.out.println("  Inseriti " + results.length + " record di test");
            }

            conn.close();
            System.out.println();

            // Leggi classifica dal DatabaseManager
            DatabaseManager db = new DatabaseManager(config);
            db.init();

            List<LeaderboardEntry> dbEntries = new ArrayList<>();
            for (int np = 2; np <= 5; np++) {
                List<LeaderboardEntry> byPlayers = db.getLeaderboard(np);
                // Filtra solo i dati di test
                for (LeaderboardEntry e : byPlayers) {
                    if (e.getNickname().startsWith(TEST_PREFIX)) {
                        dbEntries.add(e);
                    }
                }
            }

            System.out.println("✓ Dati letti dal database MySQL (" + dbEntries.size() + " record)");
            System.out.println("  URL: " + config.getJdbcUrl());
            System.out.println();

            // Ordina: per num_players ASC, poi score DESC
            dbEntries.sort(Comparator
                .comparingInt(LeaderboardEntry::getNumPlayers)
                .thenComparingInt((LeaderboardEntry e) -> -e.getScore()));

            return dbEntries;

        } catch (Exception e) {
            System.out.println("! Database non raggiungibile: " + e.getMessage());
            System.out.println("  Utilizzo dati mock per la dimostrazione.");
            System.out.println();
            return generateMockData();
        }
    }

    /**
     * Genera dati finti per testare la visualizzazione della classifica.
     */
    private static List<LeaderboardEntry> generateMockData() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        entries.add(new LeaderboardEntry("Luna",     120, 4, now.minusDays(1),  1));
        entries.add(new LeaderboardEntry("Sole",     100, 4, now.minusDays(1),  2));
        entries.add(new LeaderboardEntry("Stella",    85, 4, now.minusHours(5), 3));
        entries.add(new LeaderboardEntry("Nube",      70, 4, now.minusDays(2),  4));
        entries.add(new LeaderboardEntry("Thor",      150, 3, now.minusHours(3), 1));
        entries.add(new LeaderboardEntry("Odin",      130, 3, now.minusDays(1),  2));
        entries.add(new LeaderboardEntry("Freya",     110, 3, now.minusDays(3),  3));
        entries.add(new LeaderboardEntry("Apollo",     90, 2, now.minusHours(1), 1));
        entries.add(new LeaderboardEntry("Artemis",    80, 2, now.minusHours(2), 2));
        entries.add(new LeaderboardEntry("Zeus",       200, 5, now.minusDays(5), 1));
        entries.add(new LeaderboardEntry("Era",        180, 5, now.minusDays(5), 2));
        entries.add(new LeaderboardEntry("Poseidon",   160, 5, now.minusDays(5), 3));
        entries.add(new LeaderboardEntry("Ade",        140, 5, now.minusDays(5), 4));
        entries.add(new LeaderboardEntry("Estia",      120, 5, now.minusDays(5), 5));

        entries.sort(Comparator
            .comparingInt(LeaderboardEntry::getNumPlayers)
            .thenComparingInt((LeaderboardEntry e) -> -e.getScore()));

        return entries;
    }

    // ──────────────────────────────────────────────────────
    // CLI
    // ──────────────────────────────────────────────────────

    /**
     * Mostra la classifica in formato tabellare nel terminale.
     */
    private static void showCli(List<LeaderboardEntry> entries) {
        System.out.println("=== CLASSIFICA (CLI) ===");
        System.out.println();

        // Raggruppa per numero giocatori
        int currentPlayers = 0;
        for (LeaderboardEntry entry : entries) {
            if (entry.getNumPlayers() != currentPlayers) {
                currentPlayers = entry.getNumPlayers();
                System.out.println();
                System.out.println("Partite da " + currentPlayers + " giocatori:");
                System.out.println(SEP);
                System.out.println("| " + padRight("Rank", 3)
                    + "| " + padRight("Nickname", 20)
                    + "| " + padRight("Score", 6)
                    + "| " + padRight("Data", 20)
                    + "| " + padRight("Giocatori", 8)
                    + "|");
                System.out.println(SEP);
            }

            String md = entry.getMatchDate();
            String dateStr = (md != null && md.length() >= 10) ? md.substring(0, 10) : "N/A";
            System.out.printf("| %-3d | %-20s | %-6d | %-20s | %-8d |%n",
                entry.getPosition(),
                entry.getNickname(),
                entry.getScore(),
                dateStr,
                entry.getNumPlayers());
        }
        System.out.println(SEP);
    }

    // ──────────────────────────────────────────────────────
    // GUI
    // ──────────────────────────────────────────────────────

    /**
     * Tenta di aprire la finestra JavaFX della classifica.
     * Se JavaFX non è disponibile (esecuzione da riga di comando semplice),
     * mostra un messaggio con le istruzioni alternative.
     */
    private static void showGui(List<LeaderboardEntry> entries) {
        System.out.println("=== CLASSIFICA (GUI) ===");
        System.out.println();
        System.out.println("La classifica è visibile nella schermata di login del gioco.");
        System.out.println("Avvia il client GUI per vederla.");
        System.out.println();
        System.out.println("--- Anteprima dati (tabella CLI) ---");
        showCli(entries);
    }

    // ──────────────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────────────

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
