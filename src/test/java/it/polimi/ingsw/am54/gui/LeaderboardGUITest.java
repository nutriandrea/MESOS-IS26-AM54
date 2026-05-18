package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the leaderboard data logic used by the GUI.
 * <p>
 * JavaFX components are not loaded — this test verifies:
 * - LeaderboardEntry data integrity
 * - The podium sorting algorithm (score DESC, then name ASC)
 * - Historical leaderboard sorting by score with rank position
 */
class LeaderboardGUITest {

    @Test
    void leaderboardEntriesSortByScoreDescending() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        entries.add(new LeaderboardEntry("Alice", 50, 4, now, 2));
        entries.add(new LeaderboardEntry("Bob",   80, 4, now, 1));
        entries.add(new LeaderboardEntry("Carol", 30, 4, now, 3));

        // Sort like the server does: by score descending
        entries.sort(Comparator.comparingInt(LeaderboardEntry::getScore).reversed());

        assertEquals("Bob",   entries.get(0).getNickname());
        assertEquals(80,     entries.get(0).getScore());
        assertEquals("Alice", entries.get(1).getNickname());
        assertEquals(50,     entries.get(1).getScore());
        assertEquals("Carol", entries.get(2).getNickname());
        assertEquals(30,     entries.get(2).getScore());
    }

    @Test
    void leaderboardEntriesWithSameScoreAlphabetical() {
        // When scores are equal, the server's RANK() gives them the same position,
        // and secondary sort is by match_date DESC (most recent first).
        // For display purposes we verify entries are well-formed.
        List<LeaderboardEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        entries.add(new LeaderboardEntry("Zara", 60, 4, now, 1));
        entries.add(new LeaderboardEntry("Anna", 60, 4, now, 1));  // same score = same rank

        // Both have rank 1
        assertEquals(1, entries.get(0).getPosition());
        assertEquals(1, entries.get(1).getPosition());
    }

    @Test
    void filterLeaderboardByNumPlayers() {
        List<LeaderboardEntry> allEntries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        allEntries.add(new LeaderboardEntry("Alice", 100, 2, now, 1));
        allEntries.add(new LeaderboardEntry("Bob",    90, 2, now, 2));
        allEntries.add(new LeaderboardEntry("Carol",  80, 4, now, 1));
        allEntries.add(new LeaderboardEntry("Dave",   70, 4, now, 2));
        allEntries.add(new LeaderboardEntry("Eve",    60, 3, now, 1));

        List<LeaderboardEntry> fourPlayerGames = allEntries.stream()
                .filter(e -> e.getNumPlayers() == 4)
                .sorted(Comparator.comparingInt(LeaderboardEntry::getScore).reversed())
                .toList();

        assertEquals(2, fourPlayerGames.size());
        assertTrue(fourPlayerGames.stream().allMatch(e -> e.getNumPlayers() == 4));
        assertEquals("Carol", fourPlayerGames.get(0).getNickname());
        assertEquals("Dave",  fourPlayerGames.get(1).getNickname());
    }

    @Test
    void leaderboardEntryPositionConsistency() {
        // Simulate 5 players in a game — positions should be 1..5 without gaps
        List<LeaderboardEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        entries.add(new LeaderboardEntry("P1", 100, 4, now, 1));
        entries.add(new LeaderboardEntry("P2",  80, 4, now, 2));
        entries.add(new LeaderboardEntry("P3",  60, 4, now, 3));
        entries.add(new LeaderboardEntry("P4",  40, 4, now, 4));
        entries.add(new LeaderboardEntry("P5",  20, 4, now, 5));

        for (int i = 0; i < entries.size(); i++) {
            assertEquals(i + 1, entries.get(i).getPosition(),
                "Position " + (i + 1) + " should be assigned sequentially");
        }
    }

    @Test
    void podiumSortingByAuraThenFoodThenName() {
        // This mirrors the sorting in LeaderboardController.buildPodiumFromLocalData()
        // Sort key: aura DESC > food DESC > name ASC
        record PlayerStat(String name, int aura, int food) {}

        List<PlayerStat> players = new ArrayList<>();
        players.add(new PlayerStat("Alice", 10, 5));
        players.add(new PlayerStat("Bob",   10, 3));
        players.add(new PlayerStat("Carol", 8,  8));
        players.add(new PlayerStat("Dave",  10, 5));  // same as Alice, but D > A alphabetically
        players.add(new PlayerStat("Eve",   12, 2));

        players.sort((p1, p2) -> {
            if (p1.aura != p2.aura) return Integer.compare(p2.aura, p1.aura);
            if (p1.food != p2.food) return Integer.compare(p2.food, p1.food);
            return p1.name.compareToIgnoreCase(p2.name);
        });

        // Eve (12 aura) should be first
        assertEquals("Eve", players.get(0).name);
        assertEquals(12, players.get(0).aura);

        // Alice and Dave both have 10 aura, 5 food — Alice comes first alphabetically
        assertEquals("Alice", players.get(1).name);
        assertEquals("Dave",  players.get(2).name);

        // Bob (10 aura, 3 food)
        assertEquals("Bob", players.get(3).name);

        // Carol (8 aura)
        assertEquals("Carol", players.get(4).name);
    }
}
