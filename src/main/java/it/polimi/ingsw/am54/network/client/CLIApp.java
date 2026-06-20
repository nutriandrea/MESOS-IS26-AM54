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

/**
 * Command-line interface for the game.
 * Manages the board display, user input,
 * and command dispatch to the VirtualServer.
 * Registers as callback on ClientModel for automatic updates.
 */
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

    /**
     * Constructs the CLI application, registers as a callback on the model
     * for automatic re-rendering, and starts the input listener thread.
     *
     * @param server   the virtual server used to send commands
     * @param model    the local client model
     * @param nickname the player's nickname
     * @param scanner  the scanner reading user input
     */
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
                        System.out.println("⏳ It is not your turn! Wait for your turn.");
                    }
                }
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();
    }

    /**
     * Renders the current game state by delegating to the state-specific handler
     * and printing any pending system or error messages.
     */
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

        // 4. PRINT ALL ALERTS
        if (hasSystemAlert) {
            System.out.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(" 🚨 NOTIFICA DI RETE: " + model.getLastSystemMessage().toUpperCase());
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
            model.clearSystemMessage();
        }

        if (model.getLastErrorMessage() != null) {
            System.out.println("\n❌ [SERVER ERROR]: " + model.getLastErrorMessage());
            model.clearError();
        }
    }

    /** Renders the lobby/setup phase, prompting for color choice or game start. */
    public void renderSetup() {
        System.out.println("=== MESOS - WELCOME " + myNickname.toUpperCase() + " ===");
        System.out.println("\n--- LOBBY AND SETUP ---");
        System.out.println("Connected players: " + model.getPlayers());
        System.out.println("Chosen colors: " + model.getPlayerColors());

        if (!model.getPlayerColors().containsKey(myNickname)) {
            System.out.println("\nChoose your color (RED, YELLOW, BLACK, WHITE, BLUE): ");
            askForColor();
        } else {
            if (model.getPlayers().indexOf(myNickname) == 0) {
                System.out.println("\nYou are the first player. Type 'START' to begin the game!");
                askForStart();
            } else {
                System.out.println("\nWaiting for the room creator to start the game...");
            }
        }
    }

    /** Renders the totem-placement phase, prompting for a tile choice if it is the player's turn. */
    public void renderStartTurn() {
        printBoard();

        System.out.println("\n--- TOTEM PLACEMENT PHASE ---");
        System.out.println("Turn: " + model.getCurrentPlayer());

        if (myNickname.equals(model.getCurrentPlayer())) {
            System.out.println("IT'S YOUR TURN!");
            System.out.print("Choose which offering tile to place your totem on (index 0, 1, 2...): ");
            askForTotemPlacement();
        } else {
            System.out.println("Wait for your turn...");
        }
    }

    /** Renders the card-drawing phase, prompting for a row and index if it is the player's turn. */
    public void renderPicking() {
        printBoard();

        System.out.println("\n--- DRAW PHASE ---");
        System.out.println("Turn: " + model.getCurrentPlayer());

        if (myNickname.equals(model.getCurrentPlayer())) {
            System.out.println("IT'S YOUR TURN!");
            System.out.println("Options:");
            System.out.println("1) Draw from UPPER row");
            System.out.println("2) Draw from LOWER row");
            System.out.println("3) End Turn");
            askForPick();
        } else {
            System.out.println("Wait for your turn...");
        }
    }

    private void askForColor() {
        currentInputHandler = (input) -> {
            try {
                TotemColors color = TotemColors.valueOf(input.toUpperCase());
                server.chooseTotem(color);
                currentInputHandler = null;
            } catch (Exception e) {
                System.out.println("Invalid color. Try again.");
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
                System.out.println("Enter a valid number.");
            }
        };
    }

    private void askForPick() {
        currentInputHandler = (input) -> {
            if (input.equals("1") || input.equals("2")) {
                String row = input.equals("1") ? "upper" : "lower";
                System.out.print("Card index (0, 1...): ");

                currentInputHandler = (idxInput) -> {
                    try {
                        int idx = Integer.parseInt(idxInput);
                        server.pickCard(row, idx);
                        currentInputHandler = null;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid index. Operation cancelled.");
                        currentInputHandler = null;
                        render();
                    }
                };
            } else if (input.equals("3")) {
                server.endTurn();
                currentInputHandler = null;
            } else {
                System.out.println("Invalid choice. Enter 1, 2 or 3.");
            }
        };
    }

    private String renderizzaCarta(String cardData) {
        if (cardData == null || cardData.equals("null") || cardData.isEmpty()) return "[ EMPTY ]";

        // Now all fields are separated by '|'
        String[] parts = cardData.split("\\|");

        if (parts.length < 4) return "[ " + cardData + " ]";

        // ID is parts[0], Era is parts[1], MinPlayers is parts[2] and type/name is parts[3]
        String tipo = parts[3];

        StringBuilder visual = new StringBuilder();
        visual.append("[ ");

        String rendered = cardRenderers.getOrDefault(tipo, p -> tipo).apply(parts);
        visual.append(rendered);

        visual.append(" ]");
        return visual.toString();
    }

    private String formatRow(List<String> riga) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < riga.size(); i++) {
            sb.append(i).append(":").append(renderizzaCarta(riga.get(i))).append("   ");
        }
        return sb.toString();
    }

    private void printBoard() {
        System.out.println("\n============================== BOARD ==============================");
        System.out.println("ERA: " + model.getCurrentEra() + "  |  ROUND: " + model.getRoundCount());
        System.out.println("\n▲ --- UPPER ROW --- ▲");
        System.out.println("Events:     " + formatRow(model.getEventUpperRow()));
        System.out.println("Characters: " + formatRow(model.getCharacterUpperRow()));
        System.out.println("Buildings:  " + formatRow(model.getUpperBuildingRow()));

        System.out.println("\n▶ --- CENTRAL TRACKS --- ◀");
        System.out.println("Turn Order:");
        for (String slot : model.getTurnOrderTrack()) {
            System.out.println("  " + slot);
        }
        System.out.println("\nOfferings:");
        for (java.util.Map.Entry<Integer, String> entry : model.getOfferingTrack().entrySet()) {
            System.out.println("  " + entry.getKey() + ") " + entry.getValue());
        }

        lowerRow();

        System.out.println("\n👥 --- PLAYER STATS --- 👥");
        for (String p : model.getPlayers()) {
            Integer food = model.getPlayerFood().get(p);
            Integer aura = model.getPlayerAura().get(p);

            String status = model.isPlayerDisconnected(p) ? " [🔴 OFFLINE]" : "";
            System.out.println("🔹 " + p.toUpperCase() + status + " | 🍖 Food: " + (food != null ? food : 0) + " | ✨ Aura: " + (aura != null ? aura : 0));

            List<String> tribe = model.getPlayerTribe().get(p);
            List<String> bldgs = model.getPlayerBuildings().get(p);
            System.out.println("   Tribe:   " + (tribe != null && !tribe.isEmpty() ? formatRow(tribe) : "[ EMPTY ]"));
            System.out.println("   Buildings: " + (bldgs != null && !bldgs.isEmpty() ? formatRow(bldgs) : "[ EMPTY ]"));
        }
        System.out.println("=================================================================");
    }

    private void lowerRow() {
        System.out.println("\n▼ --- LOWER ROW --- ▼");
        System.out.println("Events:     " + formatRow(model.getEventLowerRow()));
        System.out.println("Characters: " + formatRow(model.getCharacterLowerRow()));
        System.out.println("Buildings:  " + formatRow(model.getLowerBuildingRow()));
    }

    /** Renders the end-game screen with winners, final scores and the leaderboard. */
    public void renderEndGame() {
        System.out.println("\n!!! GAME OVER !!!");
        System.out.println("Winners: " + model.getWinners());

        if (model.getLastMatchScores() != null && !model.getLastMatchScores().isEmpty()) {
            System.out.println("\n--- FINAL SCORES ---");
            model.getLastMatchScores().forEach((nick, score) ->
                    System.out.println("  " + nick + ": " + score + " points")
            );
        }

        printLeaderboard();
        printLeaderboardPrompt();
    }

    /** Renders a placeholder message while the event phase is being resolved. */
    public void renderEvent() {
        System.out.println("Event phase in progress... Wait for automatic resolution...");
    }

    /** Renders a fallback message when the current state is not recognised. */
    public void renderDefault() {
        String currentState = model.getCurrentState();
        System.out.println("Unknown state: " + currentState);
    }

    private void printLeaderboard() {
        List<LeaderboardEntry> entries = model.getLeaderboardEntries();
        if (entries == null || entries.isEmpty()) {
            System.out.println("\n(No leaderboard available)");
            return;
        }

        System.out.println("\n--- LEADERBOARD (games with " + model.getLeaderboardNumPlayers() + " players) ---");
        System.out.println("Pos | Nickname          | Score | Date");
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
            System.out.println("\nYour leaderboard position: #" + pos);
        }
        System.out.println("==========================================================");
    }

    private void printLeaderboardPrompt() {
        System.out.println("\nType 'LEADERBOARD N' to see the leaderboard for N-player games.");
        System.out.println("Type 'EXIT' to leave.");

        currentInputHandler = (input) -> {
            if (input.equalsIgnoreCase("EXIT")) {
                currentInputHandler = null;
                System.out.println("Goodbye!");
                System.exit(0);
            } else if (input.toUpperCase().startsWith("LEADERBOARD ")) {
                try {
                    int np = Integer.parseInt(input.substring(12).trim());
                    if (np < 2 || np > 5) {
                        System.out.println("Invalid number of players. Use a number between 2 and 5.");
                    } else {
                        currentInputHandler = null;
                        server.requestLeaderboard(np);
                        System.out.println("Requesting leaderboard for " + np + "-player games...");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid format. Use: LEADERBOARD 4");
                }
            } else {
                System.out.println("Unrecognized command. Type LEADERBOARD N or EXIT.");
            }
        };
    }

    /** Clears the console by printing blank lines. */
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