package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.network.server.PersistenceManager;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduces the crash/recovery desync bug during the PickingState.
 */
public class PickingRecoveryTest {

    private Game driveToPicking() throws Exception {
        Game game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.joinGame("Charlie");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "BLUE");
        game.chooseTotemColor("Charlie", "BLACK");
        game.getCurrentState().setupGame(game); // -> StartTurnState

        int guard = 0;
        while (!(game.getCurrentState() instanceof PickingState) && guard++ < 20) {
            Player cur = game.getCurrentPlayer();
            int pos = -1;
            for (int i = 0; i < game.getBoard().getOfferingTrack().size(); i++) {
                if (game.getBoard().getOfferingTrack().get(i).isAvailable()) { pos = i; break; }
            }
            game.placeTotem(cur.getNickname(), pos);
        }
        return game;
    }

    @Test
    public void testPickingSurvivesSaveLoad() throws Exception {
        Game game = driveToPicking();
        assertTrue(game.getCurrentState() instanceof PickingState, "Should be in PickingState");

        Player picker = game.getCurrentPlayer();
        OfferingTile tile = picker.getCurrentTile();
        int upBefore = tile.getRemainingUpperPicks();
        int lowBefore = tile.getRemainingLowerPicks();
        System.out.println("PRE-CRASH picker=" + picker.getNickname()
                + " remaining upper=" + upBefore + " lower=" + lowBefore);

        // --- SIMULATE SERVER CRASH: save then reload (as Server does on restart) ---
        File tmp = File.createTempFile("backup-test", ".json");
        tmp.deleteOnExit();
        PersistenceManager pm = new PersistenceManager(tmp.getAbsolutePath());
        pm.saveGame(game);

        Game reloaded = pm.loadGame();
        assertNotNull(reloaded, "PRIMARY BUG: reload returned null (backup lost on save).");

        System.out.println("POST-CRASH state=" + reloaded.getCurrentState().getClass().getSimpleName()
                + " currentPlayer=" + (reloaded.getCurrentPlayer() != null ? reloaded.getCurrentPlayer().getNickname() : "null"));

        assertTrue(reloaded.getCurrentState() instanceof PickingState,
                "State must remain PickingState after reload");

        // Simulate Controller.validateGameStateAfterRecovery()
        Player recovered = reloaded.getCurrentState().recoverActivePlayer(reloaded);
        assertNotNull(recovered, "Recovered active picker must not be null");
        reloaded.setCurrentPlayer(recovered);
        assertEquals(picker.getNickname(), recovered.getNickname(),
                "Recovered picker must match the pre-crash picker");

        // Picks must be preserved (not reset by occupy()).
        Player reloadedPicker = reloaded.getPlayerByNickname(picker.getNickname());
        assertNotNull(reloadedPicker.getCurrentTile(), "Picker's currentTile must be restored (transient field)");
        assertEquals(upBefore, reloadedPicker.getCurrentTile().getRemainingUpperPicks(),
                "Remaining UPPER picks must be preserved across reload");
        assertEquals(lowBefore, reloadedPicker.getCurrentTile().getRemainingLowerPicks(),
                "Remaining LOWER picks must be preserved across reload");

        // The recovered picker must be able to pick without "not your turn" / "wrong phase".
        String row = reloadedPicker.getCurrentTile().getRemainingUpperPicks() > 0 ? "upper" : "lower";
        reloadedPicker.modifyFood(50);
        assertDoesNotThrow(() -> reloaded.pickCard(recovered.getNickname(), row, 0),
                "Recovered picker must be able to pick after recovery");
        System.out.println("POST-CRASH pick succeeded for " + recovered.getNickname() + " on row " + row);
    }
}
