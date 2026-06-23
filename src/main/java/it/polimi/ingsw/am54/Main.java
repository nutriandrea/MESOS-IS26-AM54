package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.IGame;

/**
 * Piccola demo da console del solo motore di gioco (senza rete): crea una partita,
 * fa entrare quattro giocatori, sceglie i colori ed esegue il setup. Utile come
 * verifica rapida del modello, non è il punto d'ingresso dell'applicazione.
 */
public class Main {
    /**
     * Esegue la demo di setup del motore di gioco.
     *
     * @param args argomenti da riga di comando (non utilizzati)
     */
    public static void main(String[] args) {

        System.out.println("=== AVVIO MOTORE DI GIOCO (TEST 4 GIOCATORI) ===");

        IGame game = new Game();

        try {
            System.out.println("\n--- FASE 1: INGRESSO NELLA LOBBY ---");
            game.joinGame("carbicane");
            game.joinGame("frabbo");
            game.joinGame("nutria");
            game.joinGame("Diana");
            System.out.println("4 giocatori sono entrati con successo nella lobby.");

            System.out.println("\n--- FASE 2: SCELTA DEI COLORI ---");
            game.chooseTotemColor("carbicane", "RED");
            game.chooseTotemColor("frabbo", "YELLOW");
            game.chooseTotemColor("nutria", "BLACK");
            game.chooseTotemColor("Diana", "WHITE");
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
