package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test della plancia di gioco: gestione delle file di carte, tracciato offerte,
 * turn order e inizializzazione del layout dai file JSON di configurazione.
 */
public class BoardTest {

    private Board board;

    @BeforeEach
    public void setUp() {
        board = new Board();
    }

    private CharacterCard ch(JobType t) {
        return new CharacterCard(1, t, 0, null, new ArrayList<>(), 1);
    }

    private EventCard ev(int era) {
        return new EventCard(era, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0);
    }

    private BuildingCard bld() {
        return new BuildingCard(JobType.HUNTER, 0, 0, null, new ArrayList<>(), 1);
    }

    @Test
    public void testAddAndTakeCharacterCards() {
        board.addCharacterCard(ch(JobType.FARMER), "upper");
        board.addCharacterCard(ch(JobType.HUNTER), "lower");
        assertEquals(1, board.getCharacterRowSize("upper"));
        assertEquals(1, board.getCharacterRowSize("lower"));

        CharacterCard taken = board.takeCharacterCard("upper", 0);
        assertNotNull(taken);
        assertEquals(0, board.getCharacterRowSize("upper"));

        assertNull(board.takeCharacterCard("upper", 5), "Indice fuori range -> null");
        assertNull(board.takeCharacterCard("lower", -1));
    }

    @Test
    public void testAddAndTakeBuildingCards() {
        board.addBuildingCard(bld(), "upper");
        board.addBuildingCard(bld(), "lower");
        assertEquals(1, board.getBuildingRowSize("upper"));
        assertEquals(1, board.getBuildingRowSize("lower"));

        assertNotNull(board.takeBuildingCard("upper", 0));
        assertNull(board.takeBuildingCard("upper", 10));
    }

    @Test
    public void testEventCardsAlwaysGoUpper() {
        // addEventCard ignora il parametro row e inserisce sempre in upper
        board.addEventCard(ev(1), "lower");
        assertEquals(1, board.getEventRowSize("upper"));
        assertEquals(0, board.getEventRowSize("lower"));
    }

    @Test
    public void testMoveUpperToLowerAndBuildings() {
        board.addCharacterCard(ch(JobType.ARTIST), "upper");
        board.addEventCard(ev(1), "upper");
        board.addBuildingCard(bld(), "upper");

        board.moveUpperToLower();
        assertEquals(0, board.getCharacterRowSize("upper"));
        assertEquals(1, board.getCharacterRowSize("lower"));
        assertEquals(1, board.getEventRowSize("lower"));

        board.moveBuildings();
        assertEquals(0, board.getBuildingRowSize("upper"));
        assertEquals(1, board.getBuildingRowSize("lower"));

        board.discardLowerRowBuildings();
        assertEquals(0, board.getBuildingRowSize("lower"));

        board.clearLowerRow();
        assertEquals(0, board.getCharacterRowSize("lower"));
    }

    @Test
    public void testPopNextEventPrioritisesEraThenFarmerLast() {
        EventCard era2 = ev(2);
        EventCard era1 = ev(1);
        EventCard farmer = ev(1); farmer.setName("FARMER");

        board.getEventLowerRow().add(era2);
        board.getEventLowerRow().add(farmer);
        board.getEventLowerRow().add(era1);

        // Prima l'era più bassa NON farmer
        assertEquals(era1, board.popNextEvent());
        // Poi l'era 2 (farmer va sempre per ultimo)
        assertEquals(era2, board.popNextEvent());
        // Infine il farmer
        assertEquals(farmer, board.popNextEvent());
        assertNull(board.popNextEvent(), "Riga eventi vuota -> null");
    }

    @Test
    public void testPopNextVisibleEventFallsBackToUpper() {
        assertNull(board.popNextVisibleEvent(), "Nessun evento -> null");

        EventCard upper = ev(1);
        board.getEventUpperRow().add(upper);
        // Niente in lower: deve pescare dall'upper
        assertEquals(upper, board.popNextVisibleEvent());
    }

    @Test
    public void testAddTotemToPositionFreesTurnOrderSlot() {
        Player p = new Player(); p.setNickname("A");
        TurnOrderSlot slot = new TurnOrderSlot(1, 0);
        slot.occupy(p);
        board.getTurnOrderTile().add(slot);

        OfferingTile tile = new OfferingTile('A', 1, 1, 0);
        board.getOfferingTrack().add(tile);

        board.addTotemToPosition(p, tile);
        assertEquals(p, tile.getOccupiedBy());
        assertNull(slot.getOccupiedBy(), "Lo slot turn order del giocatore deve essere liberato");
    }

    @Test
    public void testAllTotemsPlacedByOccupiedTiles() {
        Player p1 = new Player(); p1.setNickname("A");
        Player p2 = new Player(); p2.setNickname("B");

        OfferingTile t1 = new OfferingTile('A', 1, 1, 0); t1.occupy(p1);
        OfferingTile t2 = new OfferingTile('B', 1, 1, 0); t2.occupy(p2);
        board.getOfferingTrack().add(t1);
        board.getOfferingTrack().add(t2);

        assertTrue(board.allTotemsPlaced(2), "2 tessere occupate, 2 attesi -> completo");
    }

    @Test
    public void testAllTotemsPlacedWhenNobodyLeftToPlace() {
        Player p1 = new Player(); p1.setNickname("A");
        // Giocatore nel turn order che HA già piazzato
        TurnOrderSlot slot = new TurnOrderSlot(1, 0);
        slot.occupy(p1);
        p1.setHasPlacedTotem(true);
        board.getTurnOrderTile().add(slot);

        assertTrue(board.allTotemsPlaced(2),
                "Nessuno nel turn order deve ancora piazzare -> fase completa");
    }

    @Test
    public void testGetFirstPlayerFromOfferingTrack() {
        assertNull(board.getFirstPlayerFromOfferingTrack(), "Tracciato vuoto -> null");

        Player p = new Player(); p.setNickname("First");
        OfferingTile free = new OfferingTile('A', 1, 1, 0);
        OfferingTile occupied = new OfferingTile('B', 1, 1, 0); occupied.occupy(p);
        board.getOfferingTrack().add(free);
        board.getOfferingTrack().add(occupied);

        assertEquals(p, board.getFirstPlayerFromOfferingTrack(),
                "Deve restituire il primo occupante trovato");
    }

    @Test
    public void testRemovePlayerFromTurnOrderAndShiftUp() {
        Player p1 = new Player(); p1.setNickname("A");
        Player p2 = new Player(); p2.setNickname("B");
        Player p3 = new Player(); p3.setNickname("C");

        for (int i = 0; i < 4; i++) board.getTurnOrderTile().add(new TurnOrderSlot(i + 1, 0));
        board.getTurnOrderTile().get(0).occupy(p1);
        board.getTurnOrderTile().get(1).occupy(p2);
        board.getTurnOrderTile().get(2).occupy(p3);

        // Rimuove p1: p2 e p3 scalano in alto
        board.removePlayerFromTurnOrderAndShiftUp(p1);
        assertEquals(p2, board.getTurnOrderTile().get(0).getOccupiedBy());
        assertEquals(p3, board.getTurnOrderTile().get(1).getOccupiedBy());
        assertNull(board.getTurnOrderTile().get(2).getOccupiedBy());
    }

    @Test
    public void testSetupInitialTurnOrder() {
        for (int i = 0; i < 3; i++) board.getTurnOrderTile().add(new TurnOrderSlot(i + 1, 0));
        Player p1 = new Player(); p1.setNickname("A");
        Player p2 = new Player(); p2.setNickname("B");
        List<Player> order = List.of(p1, p2);

        board.setupInitialTurnOrder(order);
        assertEquals(p1, board.getTurnOrderTile().get(0).getOccupiedBy());
        assertEquals(p2, board.getTurnOrderTile().get(1).getOccupiedBy());
    }

    // ==========================================
    // INIZIALIZZAZIONE DA JSON
    // ==========================================
    @Test
    public void testInitializeLayoutFromJson() {
        board.initializeLayout(3);
        assertFalse(board.getOfferingTrack().isEmpty(), "Le offering tile devono essere caricate dal JSON");
        assertFalse(board.getTurnOrderTile().isEmpty(), "Il turn order deve essere caricato dal JSON");

        // Le tessere devono essere ordinate per id
        List<OfferingTile> track = board.getOfferingTrack();
        for (int i = 1; i < track.size(); i++) {
            assertTrue(track.get(i - 1).getId() <= track.get(i).getId(),
                    "Le offering tile devono essere ordinate per id");
        }
    }

    @Test
    public void testInitializeLayoutRejectsInvalidPlayerCount() {
        assertThrows(IllegalArgumentException.class, () -> board.initializeLayout(1));
        assertThrows(IllegalArgumentException.class, () -> board.initializeLayout(6));
    }
}
