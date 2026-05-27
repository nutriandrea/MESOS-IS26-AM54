package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import it.polimi.ingsw.am54.gui.states.CLIAbstract;
import it.polimi.ingsw.am54.gui.states.ClientStateRegistry;
import it.polimi.ingsw.am54.gui.states.ClientStateHandler;
import java.util.Scanner;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicBoolean;

public class CLIApp implements CLIAbstract {
    private final VirtualServer server;
    private final ClientModel model;
    private final Scanner scanner;
    private final String myNickname;

    private Consumer<String> currentInputHandler = null;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<String, Function<String[], String>> cardRenderers = Map.ofEntries(
        Map.entry("SHAMAN", parts -> {
            int v1 = Integer.parseInt(parts[4]);
            return "SHAMAN " + (v1 > 0 ? "★".repeat(v1) : "");
        }),
        Map.entry("HUNTER", parts -> {
            int v1 = Integer.parseInt(parts[4]);
            return "HUNTER " + (v1 > 0 ? "🍖".repeat(v1) : "");
        }),
        Map.entry("CONSTRUCTOR", parts -> {
            int v1 = Integer.parseInt(parts[4]);
            int v2 = Integer.parseInt(parts[5]);
            StringBuilder sb = new StringBuilder("CONSTRUCTOR ");
            if (v1 > 0) sb.append("🧱-").append(v1);
            if (v2 > 0) sb.append(" ✨").append(v2);
            return sb.toString();
        }),
        Map.entry("FARMER", parts -> {
            int v1 = Integer.parseInt(parts[4]);
            return "FARMER " + (v1 >= 0 ? "🌾-" + v1 : "");
        }),
        Map.entry("INVENTOR", parts -> {
            int v1 = Integer.parseInt(parts[4]);
            return "INVENTOR " + (v1 >= 0 ? "⚙️ Liv." + v1 : "");
        }),
        Map.entry("ARTIST", parts -> "ARTIST 🎨"),
        Map.entry("BUILDING", parts -> {
            int v1 = Integer.parseInt(parts[4]);
            int v2 = Integer.parseInt(parts[5]);
            String nome = parts.length > 6 ? parts[6] : "Speciale";
            return "🏛️ " + nome + " (Cost:🍖" + v1 + " Aura:✨" + v2 + ")";
        }),
        Map.entry("EVENT", parts -> {
            String nomeEvento = parts.length > 4 ? parts[4] : "Sconosciuto";
            return "📜 " + nomeEvento;
        })
    );

    public CLIApp(VirtualServer server, ClientModel model, String nickname, Scanner scanner) {
        this.server = server;
        this.model = model;
        this.myNickname = nickname;
        this.scanner = scanner;

        this.model.setOnModelUpdatedCallback(this::render);

        startInputListener();
    }

    private void startInputListener() {
        Thread inputThread = new Thread(() -> {
            while (running.get()) {
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine();

                    if (currentInputHandler != null) {
                        currentInputHandler.accept(input);
                    } else {
                        System.out.println("⏳ Non è il tuo turno! Attendi la tua mossa.");
                    }
                }
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();
    }

    public synchronized void render() {
        String currentState = model.getCurrentState();

        boolean isMyTurn = myNickname.equals(model.getCurrentPlayer());
        boolean hasSystemAlert = model.getLastSystemMessage() != null;
        ClientStateHandler handler = ClientStateRegistry.getHandler(currentState);

        if (handler.shouldBlockInput(isMyTurn)) {
            currentInputHandler = null;
        }

        clearConsole();
        handler.renderCLI(this);

        // 4. STAMPA DIROMPENTE DEGLI ALLARMI
        if (hasSystemAlert) {
            System.out.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(" 🚨 NOTIFICA DI RETE: " + model.getLastSystemMessage().toUpperCase());
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
            model.clearSystemMessage();
        }

        if (model.getLastErrorMessage() != null) {
            System.out.println("\n❌ [ERRORE DAL SERVER]: " + model.getLastErrorMessage());
            model.clearError();
        }
    }

    public void renderSetup() {
        System.out.println("=== MESOS - BENVENUTO " + myNickname.toUpperCase() + " ===");
        System.out.println("\n--- LOBBY E SETUP ---");
        System.out.println("Giocatori connessi: " + model.getPlayers());
        System.out.println("Colori scelti: " + model.getPlayerColors());

        if (!model.getPlayerColors().containsKey(myNickname)) {
            System.out.println("\nScegli il tuo colore (RED, YELLOW, BLACK, WHITE, BLUE): ");
            askForColor();
        } else {
            if (model.getPlayers().indexOf(myNickname) == 0) {
                System.out.println("\nSei il primo giocatore. Scrivi 'START' per iniziare la partita!");
                askForStart();
            } else {
                System.out.println("\nIn attesa che il creatore della stanza avvii la partita...");
            }
        }
    }

    public void renderStartTurn() {
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

    public void renderPicking() {
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

    private void askForColor() {
        currentInputHandler = (input) -> {
            try {
                TotemColors color = TotemColors.valueOf(input.toUpperCase());
                server.chooseTotem(color);
                currentInputHandler = null;
            } catch (Exception e) {
                System.out.println("Colore non valido. Riprova.");
            }
        };
    }

    private void askForStart() {
        currentInputHandler = (input) -> {
            if (input.equalsIgnoreCase("START")) {
                server.setupGame();
                currentInputHandler = null;
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

                currentInputHandler = (idxInput) -> {
                    try {
                        int idx = Integer.parseInt(idxInput);
                        server.pickCard(row, idx);
                        currentInputHandler = null;
                    } catch (NumberFormatException e) {
                        System.out.println("Indice non valido. Operazione annullata.");
                        currentInputHandler = null;
                        render();
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

    private String renderizzaCarta(String cardData) {
        if (cardData == null || cardData.equals("null") || cardData.isEmpty()) return "[ VUOTO ]";

        // Adesso tutti i campi sono separati da '|'
        String[] parts = cardData.split("\\|");

        if (parts.length < 4) return "[ " + cardData + " ]";

        // L'ID è parts[0], l'Era è parts[1], i MinPlayers sono parts[2] e il tipo/nome è parts[3]
        String tipo = parts[3];

        StringBuilder visual = new StringBuilder();
        visual.append("[ ");

        String rendered = cardRenderers.getOrDefault(tipo, p -> tipo).apply(parts);
        visual.append(rendered);

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
        System.out.println("ERA: " + model.getCurrentEra() + "  |  ROUND: " + model.getRoundCount());
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

            String status = model.isPlayerDisconnected(p) ? " [🔴 OFFLINE]" : "";
            System.out.println("🔹 " + p.toUpperCase() + status + " | 🍖 Cibo: " + (food != null ? food : 0) + " | ✨ Aura: " + (aura != null ? aura : 0));

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

    public void renderEndGame() {
        System.out.println("\n!!! PARTITA TERMINATA !!!");
        System.out.println("Vincitori: " + model.getWinners());

        if (model.getLastMatchScores() != null && !model.getLastMatchScores().isEmpty()) {
            System.out.println("\n--- PUNTEGGI FINALI ---");
            model.getLastMatchScores().forEach((nick, score) ->
                    System.out.println("  " + nick + ": " + score + " punti")
            );
        }

        stampaClassifica();
        stampaPromptLeaderboard();
    }

    public void renderEvent() {
        System.out.println("Fase Eventi in corso... Attendi la risoluzione automatica...");
    }

    public void renderDefault() {
        String currentState = model.getCurrentState();
        System.out.println("Stato sconosciuto: " + currentState);
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
                    if (np < 2 || np > 5) {
                        System.out.println("Numero giocatori non valido. Usa un numero tra 2 e 5.");
                    } else {
                        currentInputHandler = null;
                        server.requestLeaderboard(np);
                        System.out.println("Richiesta classifica per partite da " + np + " giocatori inviata...");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Formato non valido. Usa: CLASSIFICA 4");
                }
            } else {
                System.out.println("Comando non riconosciuto. Scrivi CLASSIFICA N o ESCI.");
            }
        };
    }

    public static void clearConsole() {
        for (int i = 0; i < 20; i++) System.out.println();
    }

    private void clearInputBuffer() {
        try {
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (java.io.IOException e) {}
    }
}