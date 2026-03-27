package it.polimi.ingsw.am54;

import model.Game;
import model.IGame;

public class Main {
    public static void main(String[] args) {

        System.out.println("=== AVVIO MOTORE DI GIOCO (TEST 4 GIOCATORI) ===");

        IGame game = new Game();

        try {
            System.out.println("\n--- FASE 1: INGRESSO NELLA LOBBY ---");
            game.joinGame("carbicane");
            game.joinGame("frabbo");
            game.joinGame("Charlie");
            game.joinGame("Diana");
            System.out.println("4 giocatori sono entrati con successo nella lobby.");

            System.out.println("\n--- FASE 2: SCELTA DEI COLORI ---");
            game.chooseTotemColor("carbicane", "RED");
            game.chooseTotemColor("frabbo", "YELLOW");
            game.chooseTotemColor("Charlie", "WHITE");
            game.chooseTotemColor("Diana", "BLACK");
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
