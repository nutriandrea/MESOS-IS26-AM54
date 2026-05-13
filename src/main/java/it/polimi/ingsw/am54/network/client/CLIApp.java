package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import java.util.Scanner;
import java.util.List;

public class CLIApp {
    private final VirtualServer server;
    private final ClientModel model;
    private final Scanner scanner;
    private final String myNickname;

    // Flag per evitare di stampare mille volte mentre si aspetta input
    private boolean isWaitingForInput = false;

    public CLIApp(VirtualServer server, ClientModel model, String nickname, Scanner scanner) {
        this.server = server;
        this.model = model;
        this.myNickname = nickname;
        this.scanner = scanner;

        // Quando il server aggiorna i dati, la CLI ristampa tutto in automatico!
        this.model.setOnModelUpdatedCallback(this::render);
    }

    // Metodo "Cuore" della CLI: decide cosa stampare in base alla fase del gioco
    public synchronized void render() {
        // Se sto già aspettando che l'utente scriva, non ristampo per non "spaccare" la console
        if (isWaitingForInput) return;

        clearConsole(); // (Vedi metodo helper in fondo)
        System.out.println("=== MESOS - BENVENUTO " + myNickname.toUpperCase() + " ===");

        String state = model.getCurrentState();

        if (state.equals("Lobby") || state.equals("SetUpGameState")) {
            renderSetup();
        } else if (state.equals("StartTurnState")) {
            renderStartTurn();
        } else if (state.equals("PickingState")) {
            renderPicking();
        } else if (state.equals("EventState")) {
            System.out.println("Fase Eventi in corso... Attendi...");
        } else if (state.equals("EndGameState")) {
            renderEndGame();
        } else {
            System.out.println("Stato sconosciuto: " + state);
        }

        // Stampa eventuali messaggi di errore
        if (model.getLastErrorMessage() != null) {
            System.out.println("\n[ERRORE DAL SERVER]: " + model.getLastErrorMessage());
            // Poi puliamo l'errore o gestiamo come vogliamo
        }
    }

    // -------------------------------------------------------------
    // SCHERMATA SETUP
    // -------------------------------------------------------------
    private void renderSetup() {
        System.out.println("\n--- LOBBY E SETUP ---");
        System.out.println("Giocatori connessi: " + model.getPlayers());
        System.out.println("Colori scelti: " + model.getPlayerColors());

        // Se non ho ancora scelto un colore, lo chiedo
        if (!model.getPlayerColors().containsKey(myNickname)) {
            System.out.println("\nScegli il tuo colore (RED, YELLOW, BLACK, WHITE, BLUE): ");
            askForColor();
        } else {
            // Se sono il "capo stanza" (es. il primo giocatore), mostro il comando Start
            if (model.getPlayers().indexOf(myNickname) == 0) {
                System.out.println("\nSei il primo giocatore. Scrivi 'START' per iniziare la partita!");
                askForStart();
            } else {
                System.out.println("\nIn attesa che il creatore della stanza avvii la partita...");
            }
        }
    }

    // -------------------------------------------------------------
    // SCHERMATA START TURN (PIAZZAMENTO TOTEM)
    // -------------------------------------------------------------
    private void renderStartTurn() {
        stampaTabellone();

        System.out.println("\n--- FASE PIAZZAMENTO TOTEM ---");
        System.out.println("Tocca a: " + model.getCurrentPlayer());

        if (myNickname.equals(model.getCurrentPlayer())) {
            System.out.println("E' IL TUO TURNO!");
            System.out.print("Scegli su quale tessera offerta piazzare il tuo totem (indice 0, 1, 2...): ");
            askForTotemPlacement();
        } else {
            System.out.println("Attendi il tuo turno...");
        }
    }

    // -------------------------------------------------------------
    // SCHERMATA PICKING
    // -------------------------------------------------------------
    private void renderPicking() {
        stampaTabellone();

        System.out.println("\n--- FASE DI PESCA ---");
        System.out.println("Tocca a: " + model.getCurrentPlayer());

        if (myNickname.equals(model.getCurrentPlayer())) {
            System.out.println("E' IL TUO TURNO!");
            System.out.println("Opzioni:");
            System.out.println("1) Pesca dalla fila SUPERIORE");
            System.out.println("2) Pesca dalla fila INFERIORE");
            System.out.println("3) Termina Turno");
            askForPick();
        } else {
            System.out.println("Attendi il tuo turno...");
        }
    }

    // -------------------------------------------------------------
    // METODI CHE CHIEDONO INPUT (SBLOCCANO IL THREAD)
    // -------------------------------------------------------------
    // (Devi lanciarli in un Thread separato per non bloccare il render di rete)

    private void askForColor() {
        isWaitingForInput = true;
        new Thread(() -> {
            String colorStr = scanner.nextLine().toUpperCase();
            try {
                TotemColors color = TotemColors.valueOf(colorStr);
                server.chooseTotem(color);
            } catch (Exception e) {
                System.out.println("Colore non valido. Riprova.");
            }
            isWaitingForInput = false;
        }).start();
    }

    private void askForStart() {
        isWaitingForInput = true;
        new Thread(() -> {
            String cmd = scanner.nextLine();
            if (cmd.equalsIgnoreCase("START")) {
                server.setupGame();
            }
            isWaitingForInput = false;
        }).start();
    }

    private void askForTotemPlacement() {
        isWaitingForInput = true;
        new Thread(() -> {
            try {
                int pos = Integer.parseInt(scanner.nextLine());
                server.placeTotem(pos);
            } catch (NumberFormatException e) {
                System.out.println("Inserisci un numero valido.");
            }
            isWaitingForInput = false;
        }).start();
    }

    private void askForPick() {
        isWaitingForInput = true;
        new Thread(() -> {
            String choice = scanner.nextLine();
            if (choice.equals("1")) {
                System.out.print("Indice della carta (0, 1...): ");
                int idx = Integer.parseInt(scanner.nextLine());
                server.pickCard("upper", idx);
            } else if (choice.equals("2")) {
                System.out.print("Indice della carta (0, 1...): ");
                int idx = Integer.parseInt(scanner.nextLine());
                server.pickCard("lower", idx);
            } else if (choice.equals("3")) {
                server.endTurn();
            }
            isWaitingForInput = false;
        }).start();
    }

    // -------------------------------------------------------------
    // METODI HELPER GRAFICI
    // -------------------------------------------------------------
    private String renderizzaCarta(String cardData) {
        if (cardData == null || cardData.equals("null")) return "[ VUOTO ]";
        if (cardData.equals("EVENT")) return "[ ! EVENTO ! ]";

        String[] parts = cardData.split("\\|");
        if (parts.length < 3) return "[" + cardData + "]";

        String tipo = parts[0];
        int val1 = Integer.parseInt(parts[1]);
        int val2 = Integer.parseInt(parts[2]);

        StringBuilder visual = new StringBuilder();
        visual.append("[ ").append(tipo).append(" ");

        switch (tipo) {
            case "SHAMAN":
                if (val1 > 0) visual.append("★".repeat(val1));
                break;
            case "HUNTER":
                if (val1 > 0) visual.append("🍖".repeat(val1));
                break;
            case "CONSTRUCTOR":
                if (val1 > 0) visual.append("🧱-").append(val1);
                if (val2 > 0) visual.append(" ✨").append(val2);
                break;
            case "FARMER":
                if (val1 > 0) visual.append("🌾-").append(val1);
                break;
            case "INVENTOR":
                if (val1 > 0) visual.append("⚙️ Liv.").append(val1);
                break;
            case "ARTIST":
                visual.append("🎨");
                break;
            case "BUILDING":
                String nome = parts.length > 3 ? parts[3] : "Speciale";
                visual.append("🏛️ ").append(nome).append(" (Cost:🍖").append(val1).append(" Aura:✨").append(val2).append(")");
                break;
            default:
                break;
        }
        visual.append(" ]");
        return visual.toString();
    }

    private String formattaRiga(List<String> riga) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < riga.size(); i++) {
            sb.append(i).append(":").append(renderizzaCarta(riga.get(i))).append("   ");
        }
        return sb.toString();
    }

    private void stampaTabellone() {
        System.out.println("\n================= TABELLONE ==================");
        System.out.println("Era: " + model.getCurrentEra());
        // 1. CARTE IN ALTO
        System.out.println("\n▲ --- FILA SUPERIORE --- ▲");
        System.out.println("Eventi:     " + formattaRiga(model.getEventUpperRow()));
        System.out.println("Personaggi: " + formattaRiga(model.getCharacterUpperRow()));
        System.out.println("Edifici:    " + formattaRiga(model.getUpperBuildingRow()));

        // 2. TRACCIATI CENTRALI
        System.out.println("\n▶ --- TRACCIATO --- ◀");
        System.out.println("Ordine Turno: " + String.join(" -> ", model.getTurnOrderTrack()));
        System.out.println("Offerte:      " + model.getOfferingTrack());

        // 3. CARTE IN BASSO
        System.out.println("\n▼ --- FILA INFERIORE --- ▼");
        System.out.println("Eventi:     " + formattaRiga(model.getEventLowerRow()));
        System.out.println("Personaggi: " + formattaRiga(model.getCharacterLowerRow()));
        System.out.println("Edifici:    " + formattaRiga(model.getLowerBuildingRow()));
        System.out.println("==============================================");
    }

    private void renderEndGame() {
        System.out.println("\n!!! PARTITA TERMINATA !!!");

        // Show winners
        System.out.println("Vincitori: " + model.getWinners());

        // Show final scores
        if (model.getLastMatchScores() != null && !model.getLastMatchScores().isEmpty()) {
            System.out.println("\n--- PUNTEGGI FINALI ---");
            model.getLastMatchScores().forEach((nick, score) ->
                System.out.println("  " + nick + ": " + score + " punti")
            );
        }

        // Show leaderboard
        stampaClassifica();

        // Offer possibility to request leaderboard for different player counts
        stampaPromptLeaderboard();
    }

    private void stampaClassifica() {
        List<LeaderboardEntry> entries = model.getLeaderboardEntries();
        if (entries == null || entries.isEmpty()) {
            System.out.println("\n(Nessuna classifica disponibile)");
            return;
        }

        System.out.println("\n--- CLASSIFICA (partite da " + model.getLeaderboardNumPlayers() + " giocatori) ---");
        System.out.println("Pos | Nickname          | Punti | Data");
        System.out.println("----+-------------------+-------+---------------------------");
        for (LeaderboardEntry e : entries) {
            String highlight = (e.getNickname().equalsIgnoreCase(myNickname)) ? " <<<" : "";
            System.out.printf(" %2d | %-17s | %5d | %s%s%n",
                e.getPosition(), e.getNickname(), e.getScore(),
                e.getMatchDate() != null ? e.getMatchDate().substring(0, 10) : "",
                highlight);
        }

        int pos = model.getLeaderboardPosition();
        if (pos > 0) {
            System.out.println("\nLa tua posizione in classifica: #" + pos);
        }
        System.out.println("==========================================================");
    }

    private void stampaPromptLeaderboard() {
        System.out.println("\nScrivi 'CLASSIFICA N' per vedere la classifica delle partite da N giocatori.");
        System.out.println("Scrivi 'ESCI' per uscire.");

        currentInputHandler = (input) -> {
            if (input.equalsIgnoreCase("ESCI")) {
                currentInputHandler = null;
                System.out.println("Arrivederci!");
                System.exit(0);
            } else if (input.toUpperCase().startsWith("CLASSIFICA ")) {
                try {
                    int np = Integer.parseInt(input.substring(11).trim());
                    server.requestLeaderboard(np);
                    currentInputHandler = null;
                } catch (NumberFormatException e) {
                    System.out.println("Formato non valido. Usa: CLASSIFICA 4");
                }
            } else {
                System.out.println("Comando non riconosciuto. Scrivi CLASSIFICA N o ESCI.");
            }
        };
    }

    public static void clearConsole() {
        // Stampa tante righe vuote per pulire lo schermo
        for (int i = 0; i < 50; i++) System.out.println();
    }
}
