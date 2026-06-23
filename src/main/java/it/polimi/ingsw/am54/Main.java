package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.IGame;

/**
 * Small console demo of the game engine alone (no network): creates a game,
 * joins four players, chooses colors and performs the setup. Useful as a
 * quick verification of the model, it is not the application's entry point.
 */
public class Main {
    /**
     * Runs the game engine setup demo.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {

        System.out.println("=== AVVIO MOTORE DI GIOCO (TEST 4 GIOCATORI) ===");

        IGame game = new Game();

        try {
            System.out.println("\n--- FASE 1: INGRESSO NELLA LOBBY ---");
            game.joinGame("carbi");
            game.joinGame("frabbo");
            game.joinGame("cacio");
            game.joinGame("gabri");
            System.out.println("4 giocatori sono entrati con successo nella lobby.");

            System.out.println("\n--- FASE 2: SCELTA DEI COLORI ---");
            game.chooseTotemColor("carbi", "RED");
            game.chooseTotemColor("frabbo", "YELLOW");
            game.chooseTotemColor("cacio", "BLACK");
            game.chooseTotemColor("gabri", "WHITE");
            System.out.println("Tutti i giocatori hanno scelto il loro Totem.");

            System.out.println("\n--- FASE 3: SETUP DELLA PARTITA ---");
            game.setupGame();

            System.out.println("\n=== IL SETUP E' COMPLETATO! LA PARTITA PUO' INIZIARE ===");

        } catch (Exception e) {
            System.err.println("ERRORE DI GIOCO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}