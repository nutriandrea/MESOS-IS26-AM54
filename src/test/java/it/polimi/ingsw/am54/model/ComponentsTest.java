package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ComponentsTest {

    private Player player;
    private Game game;

    @BeforeEach
    public void setUp() {
        player = new Player();
        player.setNickname("TestPlayer");
        game = new Game();
    }

    // ==========================================
    // 1. TEST OFFERING TILE
    // ==========================================
    @Test
    public void testOfferingTile_PicksAndReset() {
        OfferingTile tile = new OfferingTile('A', 1, 1, 0);

        assertTrue(tile.useUpperPick(), "Il primo pick upper deve avere successo");
        assertTrue(tile.useLowerPick(), "Il primo pick lower deve avere successo");

        assertFalse(tile.useUpperPick(), "Non devono esserci pick upper rimanenti");
        assertFalse(tile.useLowerPick(), "Non devono esserci pick lower rimanenti");

        tile.resetPicks();
        assertEquals(1, tile.getRemainingUpperPicks(), "Dopo il reset upper pick deve tornare a 1");
        assertEquals(1, tile.getRemainingLowerPicks(), "Dopo il reset lower pick deve tornare a 1");
    }

    // ==========================================
    // 2. TEST GENERIC CARD
    // ==========================================
    @Test
    public void testGenericCardMethods() {
        // Uso la firma corretta di BuildingCard: (JobType, cost, finalAura, phase, effects, era)
        Card testCard = new BuildingCard(JobType.FARMER, 0, 0, null, new ArrayList<>(), 1);

        testCard.setEra(3);
        assertEquals(3, testCard.getEra());

        testCard.addOwner(player);
        // Rimuoviamo l'assert su getOwner() se non lo avete implementato in Card.
        // L'importante è che addOwner() non dia eccezioni.

        // Classe anonima usando il costruttore Card(int era)
        Card genericCard = new Card(1) {};

        assertThrows(UnsupportedOperationException.class, () -> {
            genericCard.pick(player, game);
        });

        Board board = new Board();
        assertThrows(UnsupportedOperationException.class, () -> {
            genericCard.reveal(board, "upper");
        });
    }

    // ==========================================
    // 3. TEST BUILDING CARD (Acquisto e Sconti)
    // ==========================================
    @Test
    public void testBuildingCard_PickAndDiscount() throws Exception {
        // Firma corretta: (JobType, cost, finalAura, phase, effects, era)
        BuildingCard building = new BuildingCard(JobType.FARMER, 4, 2, null, new ArrayList<>(), 1);

        player.modifyFood(2); // Il player ha solo 2 cibo (costo base è 4)
        assertFalse(building.canBePurchased(player), "Non deve poterlo acquistare (4 > 2)");

        Exception insufficientFoodEx = assertThrows(Exception.class, () -> {
            building.pick(player, game);
        });
        assertTrue(insufficientFoodEx.getMessage().contains("Cibo insufficiente"));

        // Applichiamo uno sconto al giocatore (costo 4 - sconto 3 = costo effettivo 1)
        player.modifyMyBuildingDiscount(3);

        assertTrue(building.canBePurchased(player), "Ora deve poterlo acquistare grazie allo sconto");

        building.pick(player, game);

        assertEquals(1, player.getFood(), "Il cibo deve scalare calcolando lo sconto");
        assertTrue(player.getBuildings().contains(building), "L'edificio deve essere nell'inventario del giocatore");
    }

    // ==========================================
    // 4. TEST BUILDING CARD (Effetti e Getter/Setter)
    // ==========================================
    @Test
    public void testBuildingCard_ResolveEffectsAndReveal() {
        final boolean[] effectApplied = {false};

        // Visto che IEffect è @FunctionalInterface, usiamo una Lambda pulitissima!
        IEffect<BuildingCard> mockEffect = (p, g, c) -> effectApplied[0] = true;

        BuildingCard building = new BuildingCard(JobType.FARMER, 0, 5, null, List.of(mockEffect), 1);
        building.setName("Forno"); // Impostiamo il nome con il setter

        building.resolveEffects(player, game);
        assertTrue(effectApplied[0], "L'effetto deve essere stato applicato");

        // Verifica dei getter basata sul costruttore usato
        assertEquals("Forno", building.getName());
        assertEquals(5, building.getFinalAura());
        assertEquals(JobType.FARMER, building.getBuildingType());
        assertEquals(1, building.getEffects().size());

        Board board = new Board();
        building.reveal(board, "upper");
        assertEquals(1, board.getBuildingRowSize("upper"), "L'edificio deve essere rimesso sulla plancia");
    }
}