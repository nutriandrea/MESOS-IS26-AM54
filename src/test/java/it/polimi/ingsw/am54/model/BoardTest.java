package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BoardTest {

    private Board board;

    @BeforeEach
    public void setUp() {
        board = new Board();
    }

    // =========================================================================
    // 1. TEST INITIALIZATION & JSON LOADING
    // =========================================================================

    @Test
    public void testInitializeLayout_Success() {
        // Testiamo il caricamento per 3 giocatori (copre initializeOfferingTiles e initializeTurnOrderTile)
        assertDoesNotThrow(() -> board.initializeLayout(3));

        assertFalse(board.getOfferingTrack().isEmpty(), "Il tracciato offerte deve essere popolato");
        assertEquals(3, board.getTurnOrderTile().size(), "Devono esserci 3 slot ordine di turno");
    }

    @Test
    public void testInitializeLayout_InvalidPlayers() {
        // Copre il lancio dell'eccezione IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> board.initializeLayout(1));
        assertThrows(IllegalArgumentException.class, () -> board.initializeLayout(6));
    }

    // =========================================================================
    // 2. TEST GESTIONE DELLE FILE (CARD MANAGEMENT)
    // =========================================================================

    @Test
    public void testAddAndTakeCards_UpperLower() {
        CharacterCard charCard = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1);
        BuildingCard buildCard = new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1);

        // Test Upper
        board.addCharacterCard(charCard, "upper");
        board.addBuildingCard(buildCard, "upper");
        assertEquals(1, board.getCharacterRowSize("upper"));
        assertEquals(1, board.getBuildingRowSize("upper"));

        // Test Lower
        board.addCharacterCard(charCard, "lower");
        board.addBuildingCard(buildCard, "lower");
        assertEquals(1, board.getCharacterRowSize("lower"));
        assertEquals(1, board.getBuildingRowSize("lower"));

        // Test Take con indici errati (copre i rami null)
        assertNull(board.takeCharacterCard("upper", 5));
        assertNull(board.takeBuildingCard("lower", -1));

        // Test Take corretto
        assertNotNull(board.takeCharacterCard("upper", 0));
        assertNotNull(board.takeBuildingCard("lower", 0));
    }

    @Test
    public void testGetRow_Combination() {
        CharacterCard charCard = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1);
        // Usiamo una sottoclasse di EventCard o una anonima per il test
        EventCard eventCard = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {};

        board.addCharacterCard(charCard, "upper");
        board.addEventCard(eventCard, "upper");

        List<Card> combined = board.getRow("upper");
        assertEquals(2, combined.size(), "getRow deve unire personaggi ed eventi");
    }

    // =========================================================================
    // 3. TEST MOVIMENTO E PULIZIA (BOARD EVOLUTION)
    // =========================================================================

    @Test
    public void testMoveAndClear() {
        CharacterCard c = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1);
        BuildingCard b = new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1);

        board.addCharacterCard(c, "upper");
        board.addBuildingCard(b, "upper");

        board.moveUpperToLower();
        board.moveBuildings();

        assertEquals(0, board.getCharacterRowSize("upper"));
        assertEquals(1, board.getCharacterRowSize("lower"));
        assertEquals(1, board.getBuildingRowSize("lower"));

        board.clearLowerRow();
        board.discardLowerRowBuildings();

        assertEquals(0, board.getCharacterRowSize("lower"));
        assertEquals(0, board.getBuildingRowSize("lower"));
    }

    // =========================================================================
    // 4. TEST LOGICA COMPLESSA: ORDINAMENTO EVENTI (Il cuore del Coverage)
    // =========================================================================

    @Test
    public void testEventPopping_PriorityLogic() {
        // Creiamo 3 eventi: uno normale Era 2, uno normale Era 1, uno Sostentamento (shouldResolveLast=true)
        EventCard e1 = new EventCard(2, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override public boolean shouldResolveLast() { return false; }
        };
        EventCard e2 = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override public boolean shouldResolveLast() { return false; }
        };
        EventCard farmer = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override public boolean shouldResolveLast() { return true; }
        };

        // Li aggiungiamo alla riga in alto e poi li spostiamo in basso (dove vengono risolti)
        board.addEventCard(e1, "upper");
        board.addEventCard(e2, "upper");
        board.addEventCard(farmer, "upper");
        board.moveUpperToLower();

        // L'ordine di "pop" deve essere: e2 (Era 1), e1 (Era 2), farmer (Sostentamento per ultimo)
        assertEquals(e2, board.popNextEvent());
        assertEquals(e1, board.popNextEvent());
        assertEquals(farmer, board.popNextEvent());
        assertNull(board.popNextEvent(), "Deve restituire null se finiti");
    }

    // =========================================================================
    // 5. TEST STATUS E TRACCIATO
    // =========================================================================

    @Test
    public void testOfferingTrackStatus() {
        board.initializeLayout(2);
        Player p = new Player();
        OfferingTile tile = board.getOfferingTrack().get(0);

        assertFalse(board.allTotemsPlaced(1));

        board.addTotemToPosition(p, tile);

        assertTrue(board.allTotemsPlaced(1));
        assertEquals(p, board.getFirstPlayerFromOfferingTrack());
    }

    // =========================================================================
    // 6. TEST REFILL LOGIC (Interazione con Game e Deck)
    // =========================================================================

    @Test
    public void testRefillUpperRow_ReachesTargetSize() {
        Game game = new Game();
        // Setup per 3 giocatori -> Target = 3 + 4 = 7 carte
        game.getPlayers().add(new Player());
        game.getPlayers().add(new Player());
        game.getPlayers().add(new Player());

        // Prepariamo lo Stack perché Deck non ha addCard
        Stack<TribeCard> cardsStack = new Stack<>();
        for(int i=0; i<10; i++) {
            cardsStack.push(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1));
        }

        // Usiamo il costruttore con lo stack
        Deck<TribeCard> deck = new Deck<>(cardsStack);
        game.setTribeDeck(deck);

        board.refillUpperRow(game);

        assertEquals(7, board.getCharacterUpperRow().size(), "La riga deve avere numPlayers + 4 carte");
    }

    // =========================================================================
    // 7. TEST LOGICA EVENTI VISIBILI (Cross-Row Priority)
    // =========================================================================

    @Test
    public void testPopNextVisibleEvent_CorrectSequence() {
        // 1. Evento "normale" Era 1 in UPPER
        EventCard eUpper = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override public boolean shouldResolveLast() { return false; }
        };
        // 2. Evento "Sostentamento" Era 3 in LOWER
        EventCard eLowerSost = new EventCard(3, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override public boolean shouldResolveLast() { return true; }
        };

        board.addEventCard(eUpper, "upper");
        board.getEventLowerRow().add(eLowerSost);

        // ESECUZIONE
        // Primo Pop: DEVE restituire eLowerSost (anche se è un Sostentamento Era 3)
        // perché la fila inferiore deve essere svuotata per prima.
        EventCard firstPop = board.popNextVisibleEvent();
        assertEquals(eLowerSost, firstPop, "Deve svuotare prima la fila inferiore");

        // Secondo Pop: Ora che la fila sotto è vuota, il tuo codice passerà alla fila sopra.
        EventCard secondPop = board.popNextVisibleEvent();
        assertEquals(eUpper, secondPop, "Solo ora deve pescare dalla fila superiore");

        // Terzo Pop: Tutto vuoto
        assertNull(board.popNextVisibleEvent());
    }

    // =========================================================================
    // 8. TEST ORDINE DI TURNO INIZIALE
    // =========================================================================

    @Test
    public void testSetupInitialTurnOrder() {
        board.initializeLayout(3);
        List<Player> players = new ArrayList<>();
        players.add(new Player());
        players.add(new Player());
        players.add(new Player());

        board.setupInitialTurnOrder(players);

        // Verifichiamo che ogni slot della turnOrderTile sia occupato
        for (TurnOrderSlot slot : board.getTurnOrderTile()) {
            assertNotNull(slot.getOccupiedBy(), "Lo slot dell'ordine di turno deve essere occupato");
        }
    }

    @Test
    public void testTakeBuildingCard_Comprehensive() {
        BuildingCard b1 = new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1);
        BuildingCard b2 = new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1);

        // Inseriamo nelle due file
        board.addBuildingCard(b1, "upper");
        board.addBuildingCard(b2, "lower");

        // 1. Test prelievo corretto Upper
        assertEquals(b1, board.takeBuildingCard("upper", 0), "Dovrebbe restituire la carta b1");
        assertEquals(0, board.getBuildingRowSize("upper"), "La lista upper dovrebbe essere vuota ora");

        // 2. Test prelievo corretto Lower
        assertEquals(b2, board.takeBuildingCard("lower", 0), "Dovrebbe restituire la carta b2");
        assertEquals(0, board.getBuildingRowSize("lower"), "La lista lower dovrebbe essere vuota ora");

        // 3. Test indici fuori limite (copre i rami rossi degli if)
        assertNull(board.takeBuildingCard("upper", 0), "Deve restituire null se la lista è vuota");
        assertNull(board.takeBuildingCard("lower", -1), "Deve restituire null per indici negativi");
    }

    @Test
    public void testPopNextVisibleEvent_UpperRowLogic() {
        // 1. Prepariamo 3 eventi nella riga UPPER
        // e1: Normale Era 2
        EventCard eNormalEra2 = new EventCard(2, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override public boolean shouldResolveLast() { return false; }
        };
        // e2: Normale Era 1 (Deve vincere per Era)
        EventCard eNormalEra1 = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override public boolean shouldResolveLast() { return false; }
        };
        // e3: Sostentamento (Deve essere l'ultimo in assoluto)
        EventCard eSostentamento = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override public boolean shouldResolveLast() { return true; }
        };

        board.addEventCard(eNormalEra2, "upper");
        board.addEventCard(eNormalEra1, "upper");
        board.addEventCard(eSostentamento, "upper");

        // Assicuriamoci che la riga LOWER sia vuota per entrare nel ramo IF corretto
        assertTrue(board.getEventLowerRow().isEmpty());

        // 2. ESECUZIONE - Il sistema deve attivare lo stream sulla fila superiore

        // Primo Pop: Deve prendere Era 1 (eNormalEra1)
        assertEquals(eNormalEra1, board.popNextVisibleEvent(), "Dovrebbe pescare l'evento con Era minore dalla fila superiore");

        // Secondo Pop: Deve prendere Era 2 (eNormalEra2)
        assertEquals(eNormalEra2, board.popNextVisibleEvent());

        // Terzo Pop: Deve prendere Sostentamento (anche se Era 1) perché shouldResolveLast è true
        assertEquals(eSostentamento, board.popNextVisibleEvent(), "Sostentamento deve essere pescato per ultimo");

        // Quarto Pop: Tutto vuoto
        assertNull(board.popNextVisibleEvent());
    }
}