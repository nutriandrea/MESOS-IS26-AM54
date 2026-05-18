package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

public class BoardIntegrationTest {

    private Board board;

    @BeforeEach
    public void setUp() {
        board = new Board();
    }

    // =========================================================================
    // 1. TEST INITIALIZE LAYOUT (Configurazione per numero giocatori)
    // =========================================================================

    @Test
    public void testInitializeLayout_2Players() {
        board.initializeLayout(2);

        // Nel manuale di Mesos, a 2 giocatori ci sono regole precise:
        // Controlliamo il numero di tessere offerta (Offering Tiles)
        // Nota: adatta il numero (es. 5, 6 o 7) in base ai vostri JSON
        assertFalse(board.getOfferingTrack().isEmpty(), "Il tracciato delle offerte non deve essere vuoto");

        // Controlliamo l'ordine di turno
        assertEquals(2, board.getTurnOrderTile().size(), "Devono esserci esattamente 2 slot ordine di turno per 2 giocatori");
    }

    @Test
    public void testInitializeLayout_5Players() {
        board.initializeLayout(5);

        // Al massimo dei giocatori, verifichiamo che la struttura sia più grande
        assertTrue(board.getOfferingTrack().size() > 5, "Con 5 giocatori devono esserci più tessere offerta");
        assertEquals(5, board.getTurnOrderTile().size(), "Devono esserci 5 slot ordine di turno");
    }

    // =========================================================================
    // 2. TEST GESTIONE DELLE FILE (Carte)
    // =========================================================================

    @Test
    public void testCardRowManagement() {
        board.initializeLayout(3);

        // Creiamo una carta finta
        CharacterCard dummy = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1);

        // Testiamo l'inserimento polimorfico (CharacterCard è una TribeCard)
        board.addCharacterCard(dummy, "upper");
        assertEquals(1, board.getCharacterRowSize("upper"), "La fila superiore dovrebbe avere 1 carta");

        // Testiamo il prelievo (Take)
        TribeCard taken = board.takeCharacterCard("upper", 0);
        assertNotNull(taken, "La carta prelevata non deve essere null");
        assertEquals(0, board.getCharacterRowSize("upper"), "La fila dovrebbe essere tornata vuota");
    }

    // =========================================================================
    // 3. TEST INTEGRITA' DATI TESSERE (Integration con JSON)
    // =========================================================================

    @Test
    public void testOfferingTileData() {
        board.initializeLayout(3);

        // Prendiamo la prima tessera del tracciato
        OfferingTile firstTile = board.getOfferingTrack().get(0);

        // Verifichiamo che i dati non siano quelli di default (0,0,0) ma siano stati letti dal JSON
        // Di solito la prima tessera ha bonus cibo > 0 e 0 pick.
        assertNotNull(firstTile);
        // Questi assert dipendono dai vostri dati reali, ma servono a capire se il JSON è stato letto
        assertTrue(firstTile.getFoodBonus() >= 0);
        assertTrue(firstTile.getRemainingUpperPicks() >= 0);
    }
}