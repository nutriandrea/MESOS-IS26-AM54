package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.IGame;

/**
 * Launches the game server. Configures and starts the game engine,
 * simulating the lobby entry flow, the color selection
 * and the game setup for a standard 4-player test.
 *
 * @author AM54
 */
public class Main {
    /**
     * Entry point that launches the game engine and runs a 4-player setup test.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {

        System.out.println("=== GAME ENGINE STARTUP (TEST 4 PLAYERS) ===");

        IGame game = new Game();

        try {
            System.out.println("\n--- PHASE 1: ENTERING LOBBY ---");
            game.joinGame("carbicane");
            game.joinGame("frabbo");
            game.joinGame("Charlie");
            game.joinGame("Diana");
            System.out.println("4 players have successfully entered the lobby.");

            System.out.println("\n--- PHASE 2: COLOR SELECTION ---");
            game.chooseTotemColor("carbicane", "RED");
            game.chooseTotemColor("frabbo", "YELLOW");
            game.chooseTotemColor("Charlie", "WHITE");
            game.chooseTotemColor("Diana", "BLACK");
            System.out.println("All players have chosen their Totem.");

            System.out.println("\n--- PHASE 3: GAME SETUP ---");
            game.setupGame();

            System.out.println("\n=== SETUP COMPLETE! THE GAME CAN START ===");

        } catch (Exception e) {
            System.err.println("GAME ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
