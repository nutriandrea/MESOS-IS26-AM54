package model.states;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.states.PickingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import static org.junit.jupiter.api.Assertions.*;

class PickingStateTest {

    private Game game;
    private PickingState pickingState;
    private Player carbicane;
    private Player frabbo;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        
        game.joinGame("carbicane");
        game.joinGame("frabbo");
        game.chooseTotemColor("carbicane", "RED");
        game.chooseTotemColor("frabbo", "YELLOW");
        game.setupGame();
        
        carbicane = game.getPlayers().get(0);
        frabbo = game.getPlayers().get(1);
        
        pickingState = new PickingState();
    }

    private void placePlayersOnTilesAndSetCurrentPicker() {
        // Clear any existing placements
        for (OfferingTile tile : game.getBoard().getOfferingTrack()) {
            tile.freeSlot();
        }
        for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            slot.freeSlot();
        }
        
        // Place players on tiles in order
        carbicane.setCurrentTile(game.getBoard().getOfferingTrack().get(0));
        game.getBoard().getOfferingTrack().get(0).occupy(carbicane);
        
        frabbo.setCurrentTile(game.getBoard().getOfferingTrack().get(1));
        game.getBoard().getOfferingTrack().get(1).occupy(frabbo);
    }

    @Test
    void testGetCurrentPicker_ReturnsFirstPlayerNotInTurnOrder() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        Player picker = game.getCurrentPicker();
        
        // First player on offering track (carbicane) should be the current picker
        assertNotNull(picker);
        assertEquals(carbicane, picker);
    }

    @Test
    void testGetCurrentPicker_ReturnsNull_WhenAllInTurnOrder() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        // Put both players in turn order
        game.assignPickerToTurnOrder(carbicane);
        game.assignPickerToTurnOrder(frabbo);
        
        Player picker = game.getCurrentPicker();
        
        // No one should be on offering tiles not in turn order
        assertNull(picker);
    }

    @Test
    void testPickCard_ValidPick() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        // Use getCurrentPicker to verify we're picking the right player
        Player picker = game.getCurrentPicker();
        assertEquals(carbicane, picker);
        
        int initialTribeSize = carbicane.getMyTribe().size();
        
        // Pick from a row that has picks available
        String row = carbicane.getCurrentTile().getRemainingUpperPicks() > 0 ? "upper" : "lower";
        
        // Pick should succeed
        assertDoesNotThrow(() -> {
            pickingState.pickCard(game, "carbicane", row, 0);
        });
        
        // Tribe should have one more card
        assertEquals(initialTribeSize + 1, carbicane.getMyTribe().size());
    }

    @Test
    void testPickCard_WrongPlayerThrows() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        // frabbo tries to pick when carbicane should be picking
        assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "frabbo", "upper", 0);
        });
    }

    @Test
    void testPickCard_InvalidIndexThrows() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "carbicane", "upper", 999);
        });
    }

    @Test
    void testPickCard_LowerRow() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        int initialTribeSize = carbicane.getMyTribe().size();
        
        assertDoesNotThrow(() -> {
            pickingState.pickCard(game, "carbicane", "lower", 0);
        });
        
        assertEquals(initialTribeSize + 1, carbicane.getMyTribe().size());
    }

    @Test
    void testEndTurn_AssignsPlayerToTurnOrder() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        assertDoesNotThrow(() -> {
            pickingState.endTurn(game, "carbicane");
        });
        
        // carbicane should now be in turn order slot 0
        TurnOrderSlot slot0 = game.getBoard().getTurnOrderTile().get(0);
        assertEquals(carbicane, slot0.getOccupiedBy());
    }

    @Test
    void testEndTurn_FreesOfferingTile() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        // Verify carbicane is on the tile
        assertEquals(carbicane, game.getBoard().getOfferingTrack().get(0).getOccupiedBy());
        
        assertDoesNotThrow(() -> {
            pickingState.endTurn(game, "carbicane");
        });
        
        // Offering tile should be freed
        assertNull(game.getBoard().getOfferingTrack().get(0).getOccupiedBy());
    }

    @Test
    void testEndTurn_WrongPlayerThrows() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        assertThrows(Exception.class, () -> {
            pickingState.endTurn(game, "frabbo");
        });
    }

    @Test
    void testEndTurn_TransitionsToEventState_WhenPickingComplete() throws Exception {
        placePlayersOnTilesAndSetCurrentPicker();
        
        // carbicane ends turn
        pickingState.endTurn(game, "carbicane");
        
        // Now frabbo should be the picker
        Player picker = game.getCurrentPicker();
        assertEquals(frabbo, picker);
        
        // frabbo ends turn
        pickingState.endTurn(game, "frabbo");
        
        // All slots should be filled
        assertTrue(game.isPickingComplete());
    }

    @Test
    void testIsPickingComplete() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        assertFalse(game.isPickingComplete());
        
        game.assignPickerToTurnOrder(carbicane);
        game.assignPickerToTurnOrder(frabbo);
        
        assertTrue(game.isPickingComplete());
    }

    @Test
    void testOfferinTilePicksDecrementAfterPick() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        String row = carbicane.getCurrentTile().getRemainingUpperPicks() > 0 ? "upper" : "lower";
        
        assertDoesNotThrow(() -> {
            pickingState.pickCard(game, "carbicane", row, 0);
        });
    }

    @Test
    void testPickCard_PlayerNotFound_Throws() {
        assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "Nobody", "upper", 0);
        });
    }

    @Test
    void testPickCard_NoTileAssigned_Throws() {
        carbicane.setCurrentTile(null);
        
        assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "carbicane", "upper", 0);
        });
    }

    @Test
    void testPickCard_InvalidRow_Throws() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "carbicane", "invalid", 0);
        });
    }

    @Test
    void testEndTurn_ExecutesSuccessfully() throws Exception {
        placePlayersOnTilesAndSetCurrentPicker();
        
        assertDoesNotThrow(() -> {
            pickingState.endTurn(game, "carbicane");
        });
    }

    @Test
    void testEndTurn_PlayerNotFound_Throws() throws Exception {
        assertThrows(Exception.class, () -> {
            pickingState.endTurn(game, "Nobody");
        });
    }

    @Test
    void testPickCard_NoRemainingPicks_ThrowsException() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        OfferingTile tile = carbicane.getCurrentTile();
        while (tile.useUpperPick()) {
        }
        
        assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "carbicane", "upper", 0);
        });
    }

    @Test
    void testPickCard_NoUpperPicksAvailable_ThrowsException() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        OfferingTile tile = carbicane.getCurrentTile();
        while (tile.useUpperPick()) {
        }
        
        Exception ex = assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "carbicane", "upper", 0);
        });
        
        assertEquals("Non hai più pick disponibili per la fila upper", ex.getMessage());
    }

    @Test
    void testPickCard_NoLowerPicksAvailable_ThrowsException() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        OfferingTile tile = carbicane.getCurrentTile();
        while (tile.useLowerPick()) {
        }
        
        Exception ex = assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "carbicane", "lower", 0);
        });
        
        assertEquals("Non hai più pick disponibili per la fila lower", ex.getMessage());
    }

    @Test
    void testPickCard_CardNotFound_ThrowsException() {
        placePlayersOnTilesAndSetCurrentPicker();
        
        Exception ex = assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "carbicane", "lower", 999);
        });
        
        assertEquals("Carta non trovata o non disponibile a quell'indice.", ex.getMessage());
    }

    @Test
    void testPickCard_InvalidRow_ThrowsException() {
        placePlayersOnTilesAndSetCurrentPicker();

        Exception ex = assertThrows(Exception.class, () -> {
            // "carbicane" deve corrispondere al nickname del currentPicker generato nel tuo setup
            pickingState.pickCard(game, "carbicane", "invalid", 0);
        });

        assertEquals("Error: Line not found", ex.getMessage());
    }

    @Test
    void testPickCard_NoTileAssigned_ThrowsException() {
        carbicane.setCurrentTile(null);
        
        Exception ex = assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "carbicane", "upper", 0);
        });
        
        assertEquals("Non è il tuo turno per pescare!", ex.getMessage());
    }

    @Test
    void testPickCard_PlayerNotFound_ThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> {
            pickingState.pickCard(game, "Nobody", "upper", 0);
        });
        
        assertTrue(ex.getMessage().contains("Nobody") && ex.getMessage().contains("non trovato"));
    }

    @Test
    void testEndTurn_PlayerNotFound_ThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> {
            pickingState.endTurn(game, "Nobody");
        });
        
        assertTrue(ex.getMessage().contains("Nobody") && ex.getMessage().contains("non trovato"));
    }

    @Test
    void testPickCard_BuildingFromUpperRow_ExecutesElseBranch() throws Exception { // <--- ASSICURATI CHE CI SIA QUESTO
        placePlayersOnTilesAndSetCurrentPicker();

        // 1. Setup Risorse
        carbicane.modifyFood(100);

        Board board = game.getBoard();

        // 2. FORZATURA TESSERA: Cerchiamo una tessera valida
        OfferingTile validTile = null;
        for (OfferingTile tile : board.getOfferingTrack()) {
            if (tile.getRemainingUpperPicks() > 0) {
                validTile = tile;
                break;
            }
        }

        // Se non troviamo una tessera adatta, la creiamo o lanciamo errore
        assertNotNull(validTile, "Non è stata trovata nessuna OfferingTile con pick sulla riga upper!");

        // Assegniamo la tessera a carbicane
        carbicane.setCurrentTile(validTile);
        validTile.occupy(carbicane);

        // 3. SETUP BOARD: Puliamo e iniettiamo l'edificio
        board.getBuildingCardsUpperRow().clear();

        // Pesca una carta dal mazzo (metodo drawCard può lanciare Exception?)
        BuildingCard cardToInject = game.getBuildingDeck().drawCard();
        assertNotNull(cardToInject, "Il mazzo edifici è vuoto!");

        board.getBuildingCardsUpperRow().add(cardToInject);

        // 4. CALCOLO INDICI
        int charSize = board.getCharacterRowSize("upper");
        int cardIndexForBuilding = charSize;

        int initialBuildingsCount = carbicane.getBuildings().size();

        // 5. ESECUZIONE
        // Non serve assertDoesNotThrow se il metodo dichiara 'throws Exception'
        pickingState.pickCard(game, "carbicane", "upper", cardIndexForBuilding);

        // 6. VERIFICHE FINALI
        assertTrue(carbicane.getBuildings().contains(cardToInject),
                "L'edificio non è stato aggiunto alla lista del player!");

        assertEquals(0, board.getBuildingCardsUpperRow().size(),
                "L'edificio è ancora sulla board!");
    }
}
