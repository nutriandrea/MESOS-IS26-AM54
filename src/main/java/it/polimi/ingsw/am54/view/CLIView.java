package it.polimi.ingsw.am54.view;

import it.polimi.ingsw.am54.model.TotemColors;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CLIView {
    private String myNickname;
    private String currentPlayer;
    private String gameState;
    private int currentEra;
    private int roundCount;

    private final Map<String, PlayerDisplayInfo> playersInfo = new ConcurrentHashMap<>();
    private final List<TileDisplayInfo> offeringTiles = new ArrayList<>();
    private final List<TurnOrderDisplayInfo> turnOrder = new ArrayList<>();

    private final List<String> upperRowCards = new ArrayList<>();
    private final List<String> lowerRowCards = new ArrayList<>();
    private final List<String> upperBuildings = new ArrayList<>();
    private final List<String> lowerBuildings = new ArrayList<>();

    private boolean gameStarted = false;
    private boolean isMyTurn = false;

    public CLIView(String nickname) {
        this.myNickname = nickname;
        this.gameState = "SETUP";
        this.currentEra = 1;
        this.roundCount = 0;
    }

    public void updatePlayerJoined(String nickname) {
        playersInfo.putIfAbsent(nickname, new PlayerDisplayInfo(nickname));
        printSystemMessage("👤 " + nickname + " si è unito alla partita!");
    }

    public void updatePlayerReconnected(String nickname) {
        PlayerDisplayInfo info = playersInfo.get(nickname);
        if (info != null) info.connected = true;
        printSystemMessage("🔄 " + nickname + " si è riconnesso!");
    }

    public void updateTotemChosen(String nickname, String colorStr) {
        PlayerDisplayInfo info = playersInfo.get(nickname);
        if (info != null) {
            info.totemColor = TotemColors.valueOf(colorStr);
        }
        if (nickname.equals(myNickname)) {
            printSystemMessage("🎨 Il tuo colore totem è: " + getColorEmoji(info.totemColor) + " " + colorStr);
        }
    }

    public void updateTotemPlaced(String nickname, int position) {
        for (TileDisplayInfo tile : offeringTiles) {
            if (tile.position == position) {
                tile.occupiedBy = nickname;
                tile.available = false;
                break;
            }
        }
        printSystemMessage("📍 " + nickname + " ha piazzato il totem nella posizione " + position);
    }

    public void updateCardPicked(String nickname, String row, int cardIndex) {
        printSystemMessage("🃏 " + nickname + " ha pescato una carta " + row + " [" + cardIndex + "]");
    }

    public void updateTurnChanged(String newCurrentPlayer) {
        this.currentPlayer = newCurrentPlayer;
        this.isMyTurn = newCurrentPlayer.equals(myNickname);
        if (isMyTurn) {
            printBanner("⭐ È IL TUO TURNO! ⭐");
        } else {
            printSystemMessage("🔹 Turno di: " + newCurrentPlayer);
        }
        displayGameState();
    }

    public void updateStateChanged(String newState) {
        this.gameState = newState;
        System.out.println("[CLIView] Stato cambiato a: " + newState);
        printSystemMessage("📋 Fase di gioco: " + formatState(newState));
        displayGameState();
    }

    public void updateGameSetup(int era, int players, List<String> playerNames,
                                List<Integer> tilePositions, List<Integer> tileFood,
                                List<String> upperCards, List<String> lowerCards,
                                List<String> upperBuild, List<String> lowerBuild) {
        this.gameStarted = true;
        this.currentEra = era;
        this.currentPlayer = playerNames.get(0);
        this.roundCount = 1;

        playersInfo.clear();
        for (String name : playerNames) {
            playersInfo.put(name, new PlayerDisplayInfo(name));
        }

        offeringTiles.clear();
        int pos = 1;
        for (int i = 0; i < tilePositions.size(); i++) {
            offeringTiles.add(new TileDisplayInfo(pos, tileFood.get(i), tilePositions.get(i) > 0));
            pos++;
        }

        upperRowCards.clear();
        upperRowCards.addAll(upperCards);
        lowerRowCards.clear();
        lowerRowCards.addAll(lowerCards);
        upperBuildings.clear();
        upperBuildings.addAll(upperBuild);
        lowerBuildings.clear();
        lowerBuildings.addAll(lowerBuild);

        displayGameState();
    }

    public void updateEndGame(List<String> winners) {
        printBanner("🎉 FINE PARTITA! 🎉");
        System.out.println("\n🏆 VINCITORI:");
        for (String w : winners) {
            System.out.println("   " + (w.equals(myNickname) ? "⭐ " : "  ") + w);
        }
        System.out.println();
    }

    public void displayGameState() {
        if (!gameStarted) {
            displayLobby();
            return;
        }

        clearScreen();
        printHeader();

        System.out.println("\n" + BoxUtils.doubleLineBox(
            "🏛️ ERA " + currentEra + " | ROUND " + roundCount + " | " + formatState(gameState),
            60
        ));

        displayPlayersPanel();
        displayBoardPanel();

        if (isMyTurn) {
            displayMyTurnActions();
        }
    }

    public void displayLobby() {
        clearScreen();
        printHeader();
        System.out.println("\n📋 LOBBY DI GIOCO");
        System.out.println(BoxUtils.singleLineBox("In attesa che il gioco inizi...", 40));

        if (!playersInfo.isEmpty()) {
            System.out.println("\n👥 Giocatori connessi (" + playersInfo.size() + "):");
            for (PlayerDisplayInfo p : playersInfo.values()) {
                String status = p.totemColor != null ? " [Colore: " + p.totemColor + "]" : " [In attesa di colore]";
                System.out.println("   • " + p.nickname + (p.nickname.equals(myNickname) ? " (TU)" : "") + status);
            }
        }
        System.out.println();
    }

    private void displayPlayersPanel() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│ 👥 GIOCATORI                                              │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");

        for (PlayerDisplayInfo p : playersInfo.values()) {
            String turnMark = p.nickname.equals(currentPlayer) ? " ▶" : "  ";
            String youMark = p.nickname.equals(myNickname) ? " (TU)" : "      ";
            String color = p.totemColor != null ? getColorEmoji(p.totemColor) + " " + p.totemColor : "❓";

            sb.append(String.format("│ %s %-10s %s %-6s                        │\n",
                turnMark, p.nickname, youMark, color));
        }
        sb.append("└──────────────────────────────────────────────────────────────┘");
        System.out.println(sb);
    }

    private void displayBoardPanel() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 🎯 OFFERING TRACK                                             │");
        System.out.println("├───────────────────────────────────────────────────────────────┤");

        for (TileDisplayInfo tile : offeringTiles) {
            String status = tile.available ? "✅ Libero" : "❌ Occupato da " + tile.occupiedBy;
            String food = "🍖 +" + tile.foodBonus;
            System.out.printf("│ [%d] %s %-35s %s   │\n", tile.position, food, status, "");
        }

        System.out.println("└──────────────────────────────────────────────────────────────┘");

        if (!upperRowCards.isEmpty()) {
            System.out.println("\n📚 UPPER ROW (" + upperRowCards.size() + " carte):");
            System.out.print("   ");
            for (int i = 0; i < Math.min(upperRowCards.size(), 8); i++) {
                System.out.print("[" + i + "] " + truncate(upperRowCards.get(i), 12) + " ");
            }
            if (upperRowCards.size() > 8) System.out.print("... (" + (upperRowCards.size() - 8) + " più)");
            System.out.println();
        }

        if (!lowerRowCards.isEmpty()) {
            System.out.println("\n📖 LOWER ROW (" + lowerRowCards.size() + " carte):");
            System.out.print("   ");
            for (int i = 0; i < Math.min(lowerRowCards.size(), 8); i++) {
                System.out.print("[" + i + "] " + truncate(lowerRowCards.get(i), 12) + " ");
            }
            System.out.println();
        }

        if (!upperBuildings.isEmpty()) {
            System.out.println("\n🏗️ EDIFICI UPPER (" + upperBuildings.size() + "):");
            System.out.print("   ");
            for (int i = 0; i < Math.min(upperBuildings.size(), 6); i++) {
                System.out.print("[" + i + "] " + truncate(upperBuildings.get(i), 15) + " ");
            }
            System.out.println();
        }
    }

    private void displayMyTurnActions() {
        System.out.println("\n" + BoxUtils.doubleLineBox("🎮 AZIONI DISPONIBILI", 60));

        String action = "";
        switch (gameState) {
            case "SetUpGameState", "SetUpColorState" -> action = "1. Scegli colore totem (1 RED|YELLOW|WHITE|BLACK)";
            case "StartTurnState" -> action = "2. Piazza totem nella offering track";
            case "PickingState" -> action = "3. Piazza totem o 4. Pescaricarta";
            default -> action = "Attendi il tuo turno...";
        }
        System.out.println("   " + action);
    }

    private void printHeader() {
        String title = "⚔️ MESOS - GIOCO DI CARTE ⚔️";
        int padding = (60 - title.length()) / 2;
        String pad = " ".repeat(Math.max(0, padding));
        System.out.println("\n" + BoxUtils.heavyLineBox(pad + title, 60));
    }

    private void printBanner(String message) {
        clearScreen();
        int padding = (60 - message.length()) / 2;
        String pad = " ".repeat(Math.max(0, padding));
        System.out.println("\n" + BoxUtils.heavyDoubleLineBox(pad + message, 60));
    }

    private void printSystemMessage(String msg) {
        System.out.println("\n📢 " + msg);
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private String formatState(String state) {
        return switch (state) {
            case "SetUpGameState" -> "🎲 Setup Partita";
            case "SetUpColorState" -> "🎨 Scelta Colore";
            case "StartTurnState" -> "🎯 Inizio Turno";
            case "PickingState" -> "🃏 Pescaggio";
            case "EventState" -> "⚡ Eventi";
            case "EndGameState" -> "🏁 Fine Partita";
            default -> state;
        };
    }

    private String getColorEmoji(TotemColors color) {
        if (color == null) return "❓";
        return switch (color) {
            case RED -> "🔴";
            case YELLOW -> "🟡";
            case WHITE -> "⚪";
            case BLACK -> "⚫";
            case BLUE -> "🔵";
        };
    }

    private String truncate(String s, int len) {
        return s.length() <= len ? s : s.substring(0, len - 2) + "..";
    }

    // Inner classes for display state
    private static class PlayerDisplayInfo {
        String nickname;
        TotemColors totemColor;
        boolean connected = true;

        PlayerDisplayInfo(String nickname) {
            this.nickname = nickname;
        }
    }

    private static class TileDisplayInfo {
        int position;
        int foodBonus;
        boolean available;
        String occupiedBy;

        TileDisplayInfo(int pos, int food, boolean avail) {
            this.position = pos;
            this.foodBonus = food;
            this.available = avail;
        }
    }

    private static class TurnOrderDisplayInfo {
        int position;
        String nickname;
        int foodBonus;
    }
}
