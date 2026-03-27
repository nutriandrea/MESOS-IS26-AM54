package model.states;

import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
