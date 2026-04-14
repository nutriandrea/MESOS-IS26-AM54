package model;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Game game;
    private Board board;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "YELLOW");
        game.setupGame();
        board = game.getBoard();
        alice = game.getPlayers().get(0);
        bob = game.getPlayers().get(1);
    }

    @Test
    void testOfferingTrackNotEmpty() {
        assertNotNull(board.getOfferingTrack());
        assertFalse(board.getOfferingTrack().isEmpty());
    }

    @Test
    void testTurnOrderTileNotEmpty() {
        assertNotNull(board.getTurnOrderTile());
        assertEquals(2, board.getTurnOrderTile().size());
    }

    @Test
    void testUpperRowHasCards() {
        int upperSize = board.getCharacterRowSize("upper");
        assertTrue(upperSize > 0);
    }

    @Test
    void testLowerRowHasCards() {
        int lowerSize = board.getCharacterRowSize("lower");
        assertTrue(lowerSize > 0);
    }

    @Test
    void testGetBuildingCardsUpperRow() {
        List<BuildingCard> upperBuildings = board.getBuildingCardsUpperRow();
        assertNotNull(upperBuildings);
    }

    @Test
    void testGetBuildingCardsLowerRow() {
        List<BuildingCard> lowerBuildings = board.getBuildingCardsLowerRow();
        assertNotNull(lowerBuildings);
    }

    @Test
    void testTakeCharacterCard() {
        int initialSize = board.getCharacterRowSize("upper");
        CharacterCard card = board.takeCharacterCard("upper", 0);
        
        assertNotNull(card);
        assertEquals(initialSize - 1, board.getCharacterRowSize("upper"));
    }

    @Test
    void testTakeCharacterCard_InvalidIndex() {
        CharacterCard card = board.takeCharacterCard("upper", 999);
        assertNull(card);
    }

    @Test
    void testTakeCharacterCard_LowerRow() {
        int initialSize = board.getCharacterRowSize("lower");
        CharacterCard card = board.takeCharacterCard("lower", 0);
        
        assertNotNull(card);
    }

    @Test
    void testAddCharacterCard_UpperRow() {
        int initialSize = board.getCharacterRowSize("upper");
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        
        board.addCharacterCard(card, "upper");
        
        assertEquals(initialSize + 1, board.getCharacterRowSize("upper"));
    }

    @Test
    void testAddCharacterCard_LowerRow() {
        int initialSize = board.getCharacterRowSize("lower");
        CharacterCard card = new CharacterCard(1, JobType.ARTIST, 0, Phases.EVENT, null, 1);
        
        board.addCharacterCard(card, "lower");
        
        assertEquals(initialSize + 1, board.getCharacterRowSize("lower"));
    }

    @Test
    void testTakeBuildingCard_Success() {
        int initialUpperSize = board.getBuildingRowSize("upper");
        BuildingCard card = board.takeBuildingCard("upper", 0);
        
        if (initialUpperSize > 0) {
            assertNotNull(card);
        }
    }

    @Test
    void testTakeBuildingCard_InvalidIndex() {
        BuildingCard card = board.takeBuildingCard("upper", 999);
        assertNull(card);
    }

    @Test
    void testGetBuildingRowSize_Upper() {
        int size = board.getBuildingRowSize("upper");
        assertTrue(size >= 0);
    }

    @Test
    void testGetBuildingRowSize_Lower() {
        int size = board.getBuildingRowSize("lower");
        assertTrue(size >= 0);
    }

    @Test
    void testAddEventCard() {
        EventCard eventCard = new EventCard(1, false, null, 2, 1, 0, 0, 0, 0, 0);
        
        assertDoesNotThrow(() -> board.addEventCard(eventCard, "lower"));
    }

    @Test
    void testMoveUpperToLower() {
        int upperInitialSize = board.getCharacterRowSize("upper");
        int lowerInitialSize = board.getCharacterRowSize("lower");
        
        board.moveUpperToLower();
        
        assertEquals(0, board.getCharacterRowSize("upper"));
        assertEquals(lowerInitialSize + upperInitialSize, board.getCharacterRowSize("lower"));
    }

    @Test
    void testClearLowerRow() {
        int initialSize = board.getCharacterRowSize("lower");
        assertTrue(initialSize > 0);
        
        board.clearLowerRow();
        
        assertEquals(0, board.getCharacterRowSize("lower"));
    }

    @Test
    void testRefillUpperRow() throws Exception {
        board.clearLowerRow();
        board.moveUpperToLower();
        
        int sizeBefore = board.getCharacterRowSize("upper");
        board.refillUpperRow(game);
        
        assertTrue(board.getCharacterRowSize("upper") >= sizeBefore);
    }

    @Test
    void testResolveAllEventsInOrder() {
        assertDoesNotThrow(() -> board.resolveAllEventsInOrder(game));
    }

    @Test
    void testResolveAllVisibleEvents() {
        assertDoesNotThrow(() -> board.resolveAllVisibleEvents(game));
    }

    @Test
    void testOfferingTileOccupation() {
        OfferingTile tile = board.getOfferingTrack().get(0);
        tile.occupy(alice);
        
        assertEquals(alice, tile.getOccupiedBy());
        
        tile.freeSlot();
        assertNull(tile.getOccupiedBy());
    }

    @Test
    void testAddTotemToPosition() {
        OfferingTile tile = board.getOfferingTrack().get(0);
        
        assertDoesNotThrow(() -> board.addTotemToPosition(alice, tile));
    }

    @Test
    void testTurnOrderSlotOccupation() {
        TurnOrderSlot slot = board.getTurnOrderTile().get(0);
        slot.occupy(alice);
        
        assertEquals(alice, slot.getOccupiedBy());
        
        slot.freeSlot();
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void testTurnOrderSlotBonusFood() {
        TurnOrderSlot slot = board.getTurnOrderTile().get(0);
        assertTrue(slot.getBonusFood() >= 0);
    }

    @Test
    void testGetRow_Upper() {
        List<?> row = board.getRow("upper");
        assertNotNull(row);
    }

    @Test
    void testGetRow_Lower() {
        List<?> row = board.getRow("lower");
        assertNotNull(row);
    }

    @Test
    void testGetCharacterRowSize() {
        assertTrue(board.getCharacterRowSize("upper") > 0);
        assertTrue(board.getCharacterRowSize("lower") >= 0);
    }

    @Test
    void testMoveBuildings() {
        assertDoesNotThrow(() -> board.moveBuildings());
    }

    @Test
    void testPopulateUpperRowBuildings() {
        assertDoesNotThrow(() -> board.populateUpperRowBuildings(game));
    }

    // ==================== Exception Tests ====================

    @Test
    void testPopulateUpperRowBuildings_InvalidPlayerCount_LessThanTwo() {
        Game invalidGame = new Game();
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            board.populateUpperRowBuildings(invalidGame);
        });
        
        assertEquals("Errore critico: Numero di giocatori non valido (0)", ex.getMessage());
    }

    @Test
    void testPopulateUpperRowBuildings_InvalidPlayerCount_MoreThanFive() throws Exception {
        Game testGame = createValidGame();
        
        java.lang.reflect.Field playersField = Game.class.getDeclaredField("players");
        playersField.setAccessible(true);
        java.util.List<Player> players = (java.util.List<Player>) playersField.get(testGame);
        
        for (int i = 0; i < 4; i++) {
            Player p = new Player();
            java.lang.reflect.Field nickField = Player.class.getDeclaredField("nickname");
            nickField.setAccessible(true);
            nickField.set(p, "P" + (i + 3));
            
            java.lang.reflect.Field totemField = Player.class.getDeclaredField("totemColor");
            totemField.setAccessible(true);
            totemField.set(p, TotemColors.WHITE);
            
            players.add(p);
        }
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            board.populateUpperRowBuildings(testGame);
        });
        
        assertEquals("Errore critico: Numero di giocatori non valido (6)", ex.getMessage());
    }

    @Test
    void testPopulateUpperRowBuildings_InvalidEra_BelowOne() throws Exception {
        Game testGame = createValidGame();
        
        java.lang.reflect.Field field = Game.class.getDeclaredField("currentEra");
        field.setAccessible(true);
        field.setInt(testGame, 0);
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            board.populateUpperRowBuildings(testGame);
        });
        
        assertEquals("Errore critico: Era non valida (0)", ex.getMessage());
    }

    @Test
    void testPopulateUpperRowBuildings_InvalidEra_AboveThree() throws Exception {
        Game testGame = createValidGame();
        
        java.lang.reflect.Field field = Game.class.getDeclaredField("currentEra");
        field.setAccessible(true);
        field.setInt(testGame, 4);
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            board.populateUpperRowBuildings(testGame);
        });
        
        assertEquals("Errore critico: Era non valida (4)", ex.getMessage());
    }

    @Test
    void testPopulateUpperRowBuildings_InvalidJson_ZerosInCounts() throws Exception {
        Game testGame = createValidGame();
        
        String tempJson = "{\n  \"buildingCounts\": {\n    \"2\": [0, 0, 0]\n  }\n}\n";
        Path tempFile = Files.createTempFile("eraCounts_inv_" + System.nanoTime(), ".json");
        Files.writeString(tempFile, tempJson);
        
        Path original = Paths.get("data/EraCounts.json");
        Path backup = Paths.get(System.getProperty("java.io.tmpdir")).resolve("eraCounts_backup_" + System.nanoTime() + ".json");
        try {
            Files.copy(original, backup);
        } catch (Exception e) {
        }
        
        try {
            Files.copy(tempFile, original, StandardCopyOption.REPLACE_EXISTING);
            
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
                board.populateUpperRowBuildings(testGame);
            });
            
            assertEquals("Errore: Il JSON dice di pescare 0 carte. Deve essere > 0.", ex.getMessage());
        } finally {
            try {
                Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(backup);
            } catch (Exception e) {
            }
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testPopulateUpperRowBuildings_MissingJsonFile() throws Exception {
        Game testGame = createValidGame();
        
        Path original = Paths.get("data/EraCounts.json");
        Path backup = Paths.get(System.getProperty("java.io.tmpdir")).resolve("eraCounts_backup_" + System.nanoTime() + ".json");
        try {
            Files.copy(original, backup);
        } catch (Exception e) {
        }
        
        try {
            Files.deleteIfExists(original);
            
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
                board.populateUpperRowBuildings(testGame);
            });
            
            assertTrue(ex.getMessage().contains("Impossibile leggere il file EraCounts.json"));
        } finally {
            try {
                Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(backup);
            } catch (Exception e) {
            }
        }
    }

    private Game createValidGame() throws Exception {
        Game game = new Game();
        game.joinGame("Player1");
        game.joinGame("Player2");
        game.chooseTotemColor("Player1", "RED");
        game.chooseTotemColor("Player2", "YELLOW");
        game.setupGame();
        return game;
    }

    @Test
    void testInitializeOfferingTiles_JsonParseError() throws IOException {
        Path original = Paths.get("data/OfferingTilesSetup.json");
        Path backup = Paths.get("data/OfferingTilesSetup.json.bak");

        // Spostiamo il file buono e mettiamo uno corrotto
        Files.move(original, backup, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(original, "{ questa non è una stringa json valida : [ }");

        try {

            //si deve aspettare una eccezione
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                board.generateBoard(game);
            });

            // Verifichiamo che il messaggio sia quello giusto:
            assertTrue(ex.getMessage().contains("Offering Tiles"));
        } finally {
            // RIPRISTINO FONDAMENTALE
            Files.move(backup, original, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    void testInitializeTurnOrderTile_FileNotFound() throws IOException {
        Path original = Paths.get("data/TurnOrderConfig.json");
        Path backup = Paths.get("data/TurnOrderConfig.json.bak");

        // Rinominiamo il file per renderlo inesistente
        Files.move(original, backup, StandardCopyOption.REPLACE_EXISTING);

        try {
            // ci aspettiamo l'eccezione:
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                board.initializeTurnOrderTile(2);
            });
            // verifichiamo che il messaggio sia quello corretto:
            assertTrue(ex.getMessage().contains("Turn Order"));
        } finally {

            // RIPRISTINO
            Files.move(backup, original, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    void testPopulateUpperRowBuildings_MissingBuildingCountsNode() throws Exception {
        Path original = Paths.get("data/EraCounts.json");
        Path backup = Paths.get("data/EraCounts.json.bak");

        // Creiamo un JSON che NON contiene la chiave per "2" giocatori
        String incompleteJson = "{\"buildingCounts\": { \"99\": [1,2,3] }}";
        Files.move(original, backup, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(original, incompleteJson);

        try {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
                board.populateUpperRowBuildings(game);
            });

            assertTrue(ex.getMessage().contains("mancante o non valido"));
        } finally {
            // RIPRISTINO
            Files.move(backup, original, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
