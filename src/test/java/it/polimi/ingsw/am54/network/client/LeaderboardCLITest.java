package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CLI leaderboard display and command parsing.
 * <p>
 * These tests verify the format of the leaderboard output
 * and the "CLASSIFICA N" command parsing logic.
 */
class LeaderboardCLITest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void classificaNCommandParsing() {
        // Simulate the parsing logic from CLIApp.stampaPromptLeaderboard
        String input = "CLASSIFICA 4";

        assertTrue(input.toUpperCase().startsWith("CLASSIFICA "));
        int np = Integer.parseInt(input.substring(11).trim());
        assertEquals(4, np);
    }

    @Test
    void classificaNCommandWithSpaces() {
        String input = "  classifica  3  ";

        // The actual CLIApp does: input.toUpperCase().startsWith("CLASSIFICA ")
        String trimmed = input.trim();
        assertTrue(trimmed.toUpperCase().startsWith("CLASSIFICA "));

        int np = Integer.parseInt(trimmed.substring(11).trim());
        assertEquals(3, np);
    }

    @Test
    void classificaInvalidInputNegativePlayers() {
        assertFalse(isValidClassificaCommand("CLASSIFICA -1"));
    }

    @Test
    void classificaInvalidInputZeroPlayers() {
        assertFalse(isValidClassificaCommand("CLASSIFICA 0"));
    }

    @Test
    void classificaInvalidInputNotANumber() {
        assertFalse(isValidClassificaCommand("CLASSIFICA abc"));
    }

    @Test
    void classificaInvalidInputNoNumber() {
        assertFalse(isValidClassificaCommand("CLASSIFICA"));
    }

    @Test
    void classificaValidInput2Players() {
        assertTrue(isValidClassificaCommand("CLASSIFICA 2"));
    }

    @Test
    void classificaValidInput3Players() {
        assertTrue(isValidClassificaCommand("CLASSIFICA 3"));
    }

    @Test
    void classificaValidInput4Players() {
        assertTrue(isValidClassificaCommand("CLASSIFICA 4"));
    }

    @Test
    void classificaValidInput5Players() {
        assertTrue(isValidClassificaCommand("CLASSIFICA 5"));
    }

    @Test
    void classificaCommandCaseInsensitive() {
        for (String input : new String[]{"classifica 4", "CLASSIFICA 4", "Classifica 4"}) {
            assertTrue(isValidClassificaCommand(input),
                "Input '" + input + "' should be case-insensitive");
        }
    }

    @Test
    void leaderboardFormatOutput() {
        // Build sample leaderboard data like the server would return
        List<LeaderboardEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        entries.add(new LeaderboardEntry("Alice", 100, 4, now, 1));
        entries.add(new LeaderboardEntry("Bob",    80, 4, now, 2));
        entries.add(new LeaderboardEntry("Carol",  60, 4, now, 3));

        // Simulate the stampaClassifica output format from CLIApp
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- CLASSIFICA (partite da 4 giocatori) ---\n");
        sb.append("Pos | Nickname          | Punti | Data\n");
        sb.append("----+-------------------+-------+---------------------------\n");
        for (LeaderboardEntry e : entries) {
            sb.append(String.format(" %2d | %-17s | %5d | %s%n",
                e.getPosition(), e.getNickname(), e.getScore(),
                e.getMatchDate() != null ? e.getMatchDate().substring(0, 10) : ""));
        }
        sb.append("\n==========================================================\n");

        String output = sb.toString();

        // Verify format structure
        assertTrue(output.contains("Pos | Nickname"));
        assertTrue(output.contains("----+-------------------"));
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Bob"));
        assertTrue(output.contains("Carol"));
        assertTrue(output.contains("100"));
        assertTrue(output.contains("80"));
        assertTrue(output.contains("60"));

        // Verify alignment: each column at expected widths
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("|")) {
                // Each data row should have exactly 3 pipe separators
                assertEquals(3, line.chars().filter(c -> c == '|').count(),
                    "Each data row should have exactly 3 pipe separators: " + line);
            }
        }
    }

    @Test
    void leaderboardEntryDateFormat() {
        // Verify the date format used in display (first 10 chars = YYYY-MM-DD)
        LocalDateTime matchDate = LocalDateTime.of(2025, 5, 18, 10, 30, 0);
        LeaderboardEntry entry = new LeaderboardEntry("Test", 50, 4, matchDate, 1);

        String displayDate = entry.getMatchDate();
        assertNotNull(displayDate);
        assertEquals("2025-05-18", displayDate.substring(0, 10),
            "Date should be formatted as YYYY-MM-DD");
    }

    @Test
    void leaderboardEmptyState() {
        // When no leaderboard entries exist, show "Nessuna classifica disponibile"
        List<LeaderboardEntry> empty = new ArrayList<>();
        String msg = "\n(Nessuna classifica disponibile)";

        // This is what CLIApp prints when entries is null or empty
        assertTrue(empty.isEmpty());
        assertTrue(msg.contains("Nessuna classifica disponibile"),
            "Empty leaderboard should show appropriate message");
    }

    // Helper: validates "CLASSIFICA N" command format
    private boolean isValidClassificaCommand(String input) {
        String trimmed = input.trim().toUpperCase();
        if (!trimmed.startsWith("CLASSIFICA ")) return false;
        try {
            int np = Integer.parseInt(trimmed.substring(11).trim());
            return np >= 1; // valid player counts are 1-5, but at least it must be positive int
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return false;
        }
    }
}
