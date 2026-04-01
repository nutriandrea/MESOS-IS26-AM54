package model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.OfferingTile;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TurnOrderSlot;
import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StartTurnStateTest {

    private Game game;
    private StartTurnState startTurnState;
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
        
        startTurnState = new StartTurnState();
    }

    private void placePlayersInTurnOrder() {
        for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            slot.freeSlot();
        }
        game.getBoard().getTurnOrderTile().get(0).occupy(carbicane);
        game.getBoard().getTurnOrderTile().get(1).occupy(frabbo);
    }

    @Test
    void testPlaceTotem_AllPlayersPlaced_TransitionsToPicking() throws Exception {
        placePlayersInTurnOrder();
        
        game.setCurrentPlayer(carbicane);
        startTurnState.placeTotem(game, "carbicane", 0);
        
        game.setCurrentPlayer(frabbo);
        startTurnState.placeTotem(game, "frabbo", 1);
        
        assertTrue(game.getCurrentState() instanceof PickingState);
    }

    @Test
    void testPlaceTotem_WrongPlayerTurn_Throws() throws Exception {
        placePlayersInTurnOrder();
        game.setCurrentPlayer(carbicane);
        
        assertThrows(Exception.class, () -> {
            startTurnState.placeTotem(game, "frabbo", 0);
        });
    }

    @Test
    void testPlaceTotem_FreesTurnOrderSlot() throws Exception {
        placePlayersInTurnOrder();
        TurnOrderSlot slot = game.getBoard().getTurnOrderTile().get(0);
        assertEquals(carbicane, slot.getOccupiedBy());
        
        game.setCurrentPlayer(carbicane);
        startTurnState.placeTotem(game, "carbicane", 0);
        
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void testPlaceTotem_SetsCurrentTile() throws Exception {
        placePlayersInTurnOrder();
        game.setCurrentPlayer(carbicane);
        
        startTurnState.placeTotem(game, "carbicane", 0);
        
        assertEquals(game.getBoard().getOfferingTrack().get(0), carbicane.getCurrentTile());
    }

    @Test
    void testPlaceTotem_SetsHasPlacedTotem() throws Exception {
        placePlayersInTurnOrder();
        game.setCurrentPlayer(carbicane);
        
        assertFalse(carbicane.hasPlacedTotem());
        
        startTurnState.placeTotem(game, "carbicane", 0);
        
        assertTrue(carbicane.hasPlacedTotem());
    }

    @Test
    void testPlaceTotem_OccupiesOfferingTile() throws Exception {
        placePlayersInTurnOrder();
        game.setCurrentPlayer(carbicane);
        OfferingTile tile = game.getBoard().getOfferingTrack().get(0);
        
        startTurnState.placeTotem(game, "carbicane", 0);
        
        assertEquals(carbicane, tile.getOccupiedBy());
    }

    @Test
    void testPlaceTotem_CallsNextPlayer_WhenNotAllPlaced() throws Exception {
        placePlayersInTurnOrder();
        
        game.setCurrentPlayer(carbicane);
        startTurnState.placeTotem(game, "carbicane", 0);
        
        assertEquals(frabbo, game.getCurrentPlayer());
    }

    @Test
    void testPlaceTotem_NoBonusFoodOnFirstRound() throws Exception {
        placePlayersInTurnOrder();
        game.setCurrentPlayer(carbicane);
        
        int initialFood = carbicane.getFood();
        
        startTurnState.placeTotem(game, "carbicane", 0);
        
        assertEquals(initialFood, carbicane.getFood());
    }

    @Test
    void testPlaceTotem_InvalidPosition_Throws() throws Exception {
        placePlayersInTurnOrder();
        game.setCurrentPlayer(carbicane);
        
        assertThrows(Exception.class, () -> {
            startTurnState.placeTotem(game, "carbicane", 999);
        });
    }
}
