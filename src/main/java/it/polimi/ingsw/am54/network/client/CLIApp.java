package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import java.util.Scanner;
import java.util.List;
import java.util.function.Consumer;

public class CLIApp {
    private final VirtualServer server;
    private final ClientModel model;
    private final Scanner scanner;
    private final String myNickname;

    private Consumer<String> currentInputHandler = null;

    public CLIApp(VirtualServer server, ClientModel model, String nickname, Scanner scanner) {
        this.server = server;
        this.model = model;
        this.myNickname = nickname;
        this.scanner = scanner;

        // Quando il server aggiorna i dati, la CLI ristampa tutto in automatico!
        this.model.setOnModelUpdatedCallback(this::render);

        startInputListener();
    }

    private void startInputListener() {
        Thread inputThread = new Thread(() -> {
            while (true) {
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine();

                    if (currentInputHandler != null) {
                        // 1. È il nostro turno! Passiamo l'input alla logica di gioco.
                        currentInputHandler.accept(input);
                    } else {
                        // 2. NON è il nostro turno! Ignoriamo l'input e avvisiamo.
                        System.out.println("⏳ Non è il tuo turno! Attendi la tua mossa.");
                    }
                }
            }
        });
        inputThread.setDaemon(true); // Permette al programma di chiudersi correttamente
        inputThread.start();
    }

    // Metodo "Cuore" della CLI: decide cosa stampare in base alla fase del gioco
    public synchronized void render() {
        // Se sto già aspettando che l'utente scriva, non ristampo per non "spaccare" la console
        if (currentInputHandler != null) return;

        clearConsole(); // (Vedi metodo helper in fondo)

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
            System.out.println("\n❌ [ERRORE DAL SERVER]: " + model.getLastErrorMessage());
            model.clearError();
        }
    }

    // -------------------------------------------------------------
    // SCHERMATA SETUP
    // -------------------------------------------------------------
    private void renderSetup() {
        System.out.println("=== MESOS - BENVENUTO " + myNickname.toUpperCase() + " ===");
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
    // METODI CHE CHIEDONO INPUT
    // -------------------------------------------------------------

    private void askForColor() {
        currentInputHandler = (input) -> {
            try {
                TotemColors color = TotemColors.valueOf(input.toUpperCase());
                server.chooseTotem(color);
                currentInputHandler = null; // Chiudiamo l'ascolto.
            } catch (Exception e) {
                System.out.println("Colore non valido. Riprova.");
            }
        };
    }

    private void askForStart() {
        currentInputHandler = (input) -> {
            if (input.equalsIgnoreCase("START")) {
                server.setupGame();
                currentInputHandler = null; // Chiudiamo l'ascolto.
            }
        };
    }

    private void askForTotemPlacement() {
        currentInputHandler = (input) -> {
            try {
                int pos = Integer.parseInt(input);
                server.placeTotem(pos);
                currentInputHandler = null;
            } catch (NumberFormatException e) {
                System.out.println("Inserisci un numero valido.");
            }
        };
    }

    private void askForPick() {
        currentInputHandler = (input) -> {
            if (input.equals("1") || input.equals("2")) {
                String row = input.equals("1") ? "upper" : "lower";
                System.out.print("Indice della carta (0, 1...): ");

                // Mossa avanzata: Cambiamo l'handler "al volo" per aspettare il secondo input!
                currentInputHandler = (idxInput) -> {
                    try {
                        int idx = Integer.parseInt(idxInput);
                        server.pickCard(row, idx);
                        currentInputHandler = null;
                    } catch (NumberFormatException e) {
                        System.out.println("Indice non valido. Operazione annullata.");
                        currentInputHandler = null;
                        render(); // Ridisegna per riniziare la scelta
                    }
                };
            } else if (input.equals("3")) {
                server.endTurn();
                currentInputHandler = null;
            } else {
                System.out.println("Scelta non valida. Inserisci 1, 2 o 3.");
            }
        };
    }

    // -------------------------------------------------------------
    // METODI HELPER GRAFICI
    // -------------------------------------------------------------
    private String renderizzaCarta(String cardData) {
        // 1. Controllo sicurezza base
        if (cardData == null || cardData.equals("null")) return "[ VUOTO ]";

        // 2. Divisione dei dati inviati dal Server
        String[] parts = cardData.split("\\|");

        // Se il server ha inviato per sbaglio solo il nome crudo (fallback di emergenza)
        if (parts.length < 2) return "[ " + cardData + " ]";

        String tipo = parts[0];

        // 3. Gestione speciale per gli EVENTI (che hanno solo TIPO e NOME)
        if (tipo.equals("EVENT")) {
            String nomeEvento = parts.length > 1 ? parts[1] : "Sconosciuto";
            return "[ 📜 " + nomeEvento + " ]";
        }

        // 4. Da qui in poi assumiamo sia Personaggio o Edificio (Servono almeno 3 parametri)
        if (parts.length < 3) return "[ " + cardData + " ]";

        int val1 = Integer.parseInt(parts[1]); // Livello o Costo
        int val2 = Integer.parseInt(parts[2]); // Aura

        StringBuilder visual = new StringBuilder();
        visual.append("[ ");

        // 5. Disegno della carta con Emoji
        switch (tipo) {
            case "SHAMAN":
                visual.append("SHAMAN ");
                if (val1 > 0) visual.append("★".repeat(val1));
                break;
            case "HUNTER":
                visual.append("HUNTER ");
                if (val1 > 0) visual.append("🍖".repeat(val1));
                break;
            case "CONSTRUCTOR":
                visual.append("CONSTRUCTOR ");
                if (val1 > 0) visual.append("🧱-").append(val1);
                if (val2 > 0) visual.append(" ✨").append(val2);
                break;
            case "FARMER":
                visual.append("FARMER ");
                if (val1 > 0) visual.append("🌾-").append(val1);
                break;
            case "INVENTOR":
                visual.append("INVENTOR ");
                if (val1 > 0) visual.append("⚙️ Liv.").append(val1);
                break;
            case "ARTIST":
                visual.append("ARTIST 🎨");
                break;
            case "BUILDING":
                // L'edificio ha un 4° parametro: il Nome
                String nome = parts.length > 3 ? parts[3] : "Speciale";
                visual.append("🏛️ ").append(nome)
                        .append(" (Cost:🍖").append(val1)
                        .append(" Aura:✨").append(val2).append(")");
                break;
            default:
                visual.append(tipo);
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
        System.out.println("\n=========================== TABELLONE ===========================");
        System.out.println("Era: " + model.getCurrentEra());

        System.out.println("\n▲ --- FILA SUPERIORE --- ▲");
        System.out.println("Eventi:     " + formattaRiga(model.getEventUpperRow()));
        System.out.println("Personaggi: " + formattaRiga(model.getCharacterUpperRow()));
        System.out.println("Edifici:    " + formattaRiga(model.getUpperBuildingRow()));

        System.out.println("\n▶ --- TRACCIATI CENTRALI --- ◀");
        System.out.println("Ordine Turno:");
        for (String slot : model.getTurnOrderTrack()) {
            System.out.println("  " + slot);
        }
        System.out.println("\nOfferte:");
        for (java.util.Map.Entry<Integer, String> entry : model.getOfferingTrack().entrySet()) {
            System.out.println("  " + entry.getKey() + ") " + entry.getValue());
        }

        loweRow();

        System.out.println("\n👥 --- STATISTICHE GIOCATORI --- 👥");
        for (String p : model.getPlayers()) {
            Integer food = model.getPlayerFood().get(p);
            Integer aura = model.getPlayerAura().get(p);
            System.out.println("🔹 " + p.toUpperCase() + " | 🍖 Cibo: " + (food != null ? food : 0) + " | ✨ Aura: " + (aura != null ? aura : 0));

            List<String> tribe = model.getPlayerTribe().get(p);
            List<String> bldgs = model.getPlayerBuildings().get(p);
            System.out.println("   Tribù:   " + (tribe != null && !tribe.isEmpty() ? formattaRiga(tribe) : "[ VUOTA ]"));
            System.out.println("   Edifici: " + (bldgs != null && !bldgs.isEmpty() ? formattaRiga(bldgs) : "[ VUOTI ]"));
        }
        System.out.println("=================================================================");
    }

    private void loweRow() {
        System.out.println("\n▼ --- FILA INFERIORE --- ▼");
        System.out.println("Eventi:     " + formattaRiga(model.getEventLowerRow()));
        System.out.println("Personaggi: " + formattaRiga(model.getCharacterLowerRow()));
        System.out.println("Edifici:    " + formattaRiga(model.getLowerBuildingRow()));
    }

    private void renderEndGame() {
        System.out.println("\n!!! PARTITA TERMINATA !!!");
        System.out.println("Vincitori: " + model.getWinners());
    }

    public static void clearConsole() {
        // Stampa tante righe vuote per pulire lo schermo
        for (int i = 0; i < 20; i++) System.out.println();
    }

    private void clearInputBuffer() {
        try {
            // System.in.available() restituisce il numero di byte pronti nel buffer
            while (System.in.available() > 0) {
                System.in.read(); // Legge e scarta i byte accumulati
            }
        } catch (java.io.IOException e) {
            // Se c'è un errore nella lettura del buffer, non facciamo nulla
        }
    }
}
