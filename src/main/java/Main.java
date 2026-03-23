import model.Game;
import model.IGame;

public class Main {
    public static void main(String[] args) {

        System.out.println("=== AVVIO MOTORE DI GIOCO (TEST 4 GIOCATORI) ===");

        // Creiamo il gioco usando l'interfaccia (il nostro famoso "telecomando")
        IGame game = new Game();

        try {
            System.out.println("\n--- FASE 1: INGRESSO NELLA LOBBY ---");
            // Simuliamo il Controller che fa entrare 4 giocatori
            game.joinGame("Alice");
            game.joinGame("Bob");
            game.joinGame("Charlie");
            game.joinGame("Diana");
            System.out.println("4 giocatori sono entrati con successo nella lobby.");

            System.out.println("\n--- FASE 2: SCELTA DEI COLORI ---");
            // Assegniamo i colori basandoci sul tuo UML (TotemColors)
            game.chooseTotemColor("Alice", "RED");
            game.chooseTotemColor("Bob", "BLUE");
            game.chooseTotemColor("Charlie", "YELLOW");
            game.chooseTotemColor("Diana", "BLACK");
            System.out.println("Tutti i giocatori hanno scelto il loro Totem.");

            System.out.println("\n--- FASE 3: SETUP DELLA PARTITA ---");
            // Il Controller dà il via libera. Qui dentro il Model creerà la Board,
            // posizionerà i 4 Totem casualmente e distribuirà i cosciotti!
            game.setupGame();

            System.out.println("\n=== IL SETUP E' COMPLETATO! LA PARTITA PUO' INIZIARE ===");

        } catch (Exception e) {
            // Se scatta un'eccezione (es. colori duplicati, troppi giocatori), ce ne accorgiamo subito!
            System.err.println("ERRORE DI GIOCO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
