# Implementazione TUI - AM54

## Panoramica

TUI interattiva con navigazione a frecce, rendering completo della board, visualizzazione di tutti i giocatori con le loro tribe e buildings.

---

## Dipendenze

### pom.xml - Aggiungere JLine

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.24.1</version>
</dependency>
```

---

## Struttura File

```
src/main/java/it/polimi/ingsw/am54/network/client/tui/
├── ClientTUI.java                    # Entry point main()
├── TerminalHandler.java               # Wrapper JLine
├── ScreenBuffer.java                  # Buffer rendering
├── ui/
│   ├── BoardRenderer.java            # Carte sul tavolo
│   ├── PlayerRenderer.java           # Tribe/Building player
│   ├── OfferingTrackRenderer.java    # Tessere offerta
│   └── TurnOrderRenderer.java        # Ordine turno
├── navigation/
│   ├── NavigationState.java          # Posizione cursore
│   ├── ScreenSection.java            # Enum sezioni schermo
│   └── KeyStroke.java                 # Gestione input
└── theme/
    └── Theme.java                    # Color palette ANSI
```

---

## Modifiche Esistenti

### Client.java - Aggiungere sendRequest()

In `src/main/java/it/polimi/ingsw/am54/network/client/Client.java`, aggiungere:

```java
public void sendRequest(Request request) {
    if (connection != null) {
        connection.sendRequest(request);
    }
}
```

---

## Classi da Creare

### 1. Theme.java - Palette Colori ANSI

```java
package it.polimi.ingsw.am54.network.client.tui.theme;

public class Theme {
    // Reset
    public static final String RESET = "\033[0m";
    
    // Character Cards (JobType)
    public static final String HUNTER = "\033[33m";     // Yellow
    public static final String SHAMAN = "\033[35m";     // Magenta
    public static final String CONSTRUCTOR = "\033[34m"; // Blue
    public static final String FARMER = "\033[32m";     // Green
    public static final String INVENTOR = "\033[36m";   // Cyan
    public static final String ARTIST = "\033[91m";      // Bright Red
    public static final String NULL_JOB = "\033[90m";    // Grey
    
    // Building Cards
    public static final String BUILDING = "\033[38;5;202m"; // Orange
    
    // Totem Colors
    public static final String RED = "\033[31m";
    public static final String YELLOW = "\033[33m";
    public static final String WHITE = "\033[37m";
    public static final String BLACK = "\033[30;1m";
    public static final String BLUE = "\033[34m";
    
    // Player Status
    public static final String DISCONNECTED = "\033[90m";  // Grey
    public static final String ACTIVE = "\033[32m";        // Green
    public static final String CURRENT_TURN = "\033[1;32m"; // Bold Green
    
    // UI Elements
    public static final String BORDER = "\033[36m";      // Cyan
    public static final String SELECTED = "\033[44m";      // Blue bg
    public static final String ARROW_UP = "▲";
    public static final String ARROW_DOWN = "▼";
    public static final String ARROW_RIGHT = "→";
    public static final String CONNECTED = "●";
    public static final String DISCONNECTED_INDICATOR = "○";
    
    public static String getTotemColor(String totem) {
        if (totem == null) return RESET;
        return switch (totem.toUpperCase()) {
            case "RED" -> RED;
            case "YELLOW" -> YELLOW;
            case "WHITE" -> WHITE;
            case "BLACK" -> BLACK;
            case "BLUE" -> BLUE;
            default -> RESET;
        };
    }
    
    public static String getJobColor(String job) {
        if (job == null) return RESET;
        return switch (job.toUpperCase()) {
            case "HUNTER" -> HUNTER;
            case "SHAMAN" -> SHAMAN;
            case "CONSTRUCTOR" -> CONSTRUCTOR;
            case "FARMER" -> FARMER;
            case "INVENTOR" -> INVENTOR;
            case "ARTIST" -> ARTIST;
            default -> NULL_JOB;
        };
    }
}
```

---

### 2. KeyStroke.java - Gestione Input

```java
package it.polimi.ingsw.am54.network.client.tui.navigation;

public class KeyStroke {
    public enum Type { UP, DOWN, LEFT, RIGHT, ENTER, ESC, BACKSPACE, CHAR }
    
    public final Type type;
    public final char character;
    
    public KeyStroke(Type type) {
        this.type = type;
        this.character = 0;
    }
    
    public KeyStroke(Type type, char character) {
        this.type = type;
        this.character = character;
    }
    
    // Costanti
    public static final KeyStroke UP = new KeyStroke(Type.UP);
    public static final KeyStroke DOWN = new KeyStroke(Type.DOWN);
    public static final KeyStroke LEFT = new KeyStroke(Type.LEFT);
    public static final KeyStroke RIGHT = new KeyStroke(Type.RIGHT);
    public static final KeyStroke ENTER = new KeyStroke(Type.ENTER);
    public static final KeyStroke ESC = new KeyStroke(Type.ESC);
    public static final KeyStroke BACKSPACE = new KeyStroke(Type.BACKSPACE);
}
```

---

### 3. ScreenSection.java - Sezioni Schermo

```java
package it.polimi.ingsw.am54.network.client.tui.navigation;

public enum ScreenSection {
    UPPER_BOARD,      // Character/Event/Building upper row
    LOWER_BOARD,      // Character/Event lower row
    OFFERING_TRACK,   // A-G tessere
    TURN_ORDER,       // Turn order slots
    PLAYERS_LIST,     // Tutti i player con stats
    COMMAND_LINE;     // Input comando
    
    public ScreenSection nextVertical() {
        return switch (this) {
            case UPPER_BOARD -> LOWER_BOARD;
            case LOWER_BOARD -> OFFERING_TRACK;
            case OFFERING_TRACK -> TURN_ORDER;
            case TURN_ORDER -> PLAYERS_LIST;
            case PLAYERS_LIST -> COMMAND_LINE;
            case COMMAND_LINE -> UPPER_BOARD;
        };
    }
    
    public ScreenSection prevVertical() {
        return switch (this) {
            case UPPER_BOARD -> COMMAND_LINE;
            case LOWER_BOARD -> UPPER_BOARD;
            case OFFERING_TRACK -> LOWER_BOARD;
            case TURN_ORDER -> OFFERING_TRACK;
            case PLAYERS_LIST -> TURN_ORDER;
            case COMMAND_LINE -> PLAYERS_LIST;
        };
    }
}
```

---

### 4. NavigationState.java - Gestione Navigazione

```java
package it.polimi.ingsw.am54.network.client.tui.navigation;

import it.polimi.ingsw.am54.network.client.ClientModel;

public class NavigationState {
    private ScreenSection currentSection;
    private int currentIndex;
    private ClientModel model;
    
    public NavigationState(ClientModel model) {
        this.model = model;
        this.currentSection = ScreenSection.UPPER_BOARD;
        this.currentIndex = 0;
    }
    
    public void moveUp() {
        currentSection = currentSection.prevVertical();
        currentIndex = 0;
    }
    
    public void moveDown() {
        currentSection = currentSection.nextVertical();
        currentIndex = 0;
    }
    
    public void moveLeft() {
        currentIndex = Math.max(0, currentIndex - 1);
    }
    
    public void moveRight() {
        int max = getMaxIndex();
        currentIndex = Math.min(max, currentIndex + 1);
    }
    
    public int getMaxIndex() {
        return switch (currentSection) {
            case UPPER_BOARD -> Math.max(0, model.getCharacterUpperRow().size() - 1);
            case LOWER_BOARD -> Math.max(0, model.getCharacterLowerRow().size() - 1);
            case OFFERING_TRACK -> Math.max(0, model.getOfferingTrack().size() - 1);
            case TURN_ORDER -> Math.max(0, model.getTurnOrderTrack().size() - 1);
            case PLAYERS_LIST -> Math.max(0, model.getPlayers().size() - 1);
            case COMMAND_LINE -> 0;
        };
    }
    
    public ScreenSection getCurrentSection() { return currentSection; }
    public int getCurrentIndex() { return currentIndex; }
    
    public boolean isSelected(int itemIndex) {
        return currentSection != ScreenSection.COMMAND_LINE && itemIndex == currentIndex;
    }
}
```

---

### 5. TerminalHandler.java - Wrapper JLine

```java
package it.polimi.ingsw.am54.network.client.tui;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import it.polimi.ingsw.am54.network.client.tui.navigation.KeyStroke;
import java.io.IOException;

public class TerminalHandler {
    private Terminal terminal;
    private LineReader reader;
    
    public void init() throws IOException {
        terminal = TerminalBuilder.builder()
            .system(true)
            .jline(true)
            .build();
        
        reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .build();
    }
    
    public KeyStroke readKey() throws IOException {
        int read = terminal.reader().read();
        
        // Gestione sequenze escape (frecce)
        if (read == 27) {
            int next = terminal.reader().read();
            if (next == 91) {
                int arrow = terminal.reader().read();
                return switch (arrow) {
                    case 65 -> KeyStroke.UP;
                    case 66 -> KeyStroke.DOWN;
                    case 67 -> KeyStroke.RIGHT;
                    case 68 -> KeyStroke.LEFT;
                    default -> KeyStroke.ESC;
                };
            }
            return KeyStroke.ESC;
        }
        
        if (read == 10 || read == 13) return KeyStroke.ENTER;
        if (read == 27) return KeyStroke.ESC;
        
        return new KeyStroke(KeyStroke.Type.CHAR, (char) read);
    }
    
    public void clearScreen() throws IOException {
        terminal.writer().print("\033[2J");
        terminal.writer().print("\033[H");
        terminal.writer().flush();
    }
    
    public void write(String s) {
        terminal.writer().print(s);
    }
    
    public void flush() {
        terminal.writer().flush();
    }
    
    public int getWidth() { return terminal.getWidth(); }
    public int getHeight() { return terminal.getHeight(); }
    
    public void close() throws IOException {
        if (terminal != null) terminal.close();
    }
}
```

---

### 6. ScreenBuffer.java - Buffer Rendering

```java
package it.polimi.ingsw.am54.network.client.tui;

public class ScreenBuffer {
    private char[][] buffer;
    private int width, height;
    
    public ScreenBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.buffer = new char[height][width];
        clear();
    }
    
    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = ' ';
            }
        }
    }
    
    public void setString(int x, int y, String s) {
        if (y < 0 || y >= height) return;
        for (int i = 0; i < s.length() && (x + i) < width; i++) {
            buffer[y][x + i] = s.charAt(i);
        }
    }
    
    public void setChar(int x, int y, char c) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            buffer[y][x] = c;
        }
    }
    
    public void setBox(int x, int y, int w, int h, String border) {
        // Top
        setString(x, y, "┌" + "─".repeat(w - 2) + "┐");
        // Sides
        for (int i = 1; i < h - 1; i++) {
            setChar(x, y + i, '│');
            setChar(x + w - 1, y + i, '│');
        }
        // Bottom
        setString(x, y + h - 1, "└" + "─".repeat(w - 2) + "┘");
    }
    
    public String render() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(buffer[y][x]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
```

---

### 7. BoardRenderer.java - Rendering Board

```java
package it.polimi.ingsw.am54.network.client.tui.ui;

import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.tui.ScreenBuffer;
import it.polimi.ingsw.am54.network.client.tui.theme.Theme;

public class BoardRenderer {
    
    public void render(ScreenBuffer buffer, int startX, int startY,
                       ClientModel model, int selectedCard) {
        
        // Title
        buffer.setString(startX, startY, "UPPER ROW");
        
        // Character Upper Row
        var chars = model.getCharacterUpperRow();
        for (int i = 0; i < chars.size(); i++) {
            renderCharacterCard(buffer, startX + (i * 12), startY + 2, 
                               chars.get(i), selectedCard == i);
        }
        
        // Event Upper Row
        var events = model.getEventUpperRow();
        for (int i = 0; i < events.size(); i++) {
            renderEventCard(buffer, startX + (i * 12), startY + 10, 
                           events.get(i), selectedCard == chars.size() + i);
        }
        
        // Building Upper Row
        var buildings = model.getUpperBuildingRow();
        for (int i = 0; i < buildings.size(); i++) {
            renderBuildingCard(buffer, startX + (i * 12), startY + 18, 
                              buildings.get(i), selectedCard == chars.size() + events.size() + i);
        }
        
        // LOWER ROW
        buffer.setString(startX, startY + 28, "LOWER ROW");
        
        var lowerChars = model.getCharacterLowerRow();
        for (int i = 0; i < lowerChars.size(); i++) {
            renderCharacterCard(buffer, startX + (i * 12), startY + 30, 
                               lowerChars.get(i), false);
        }
    }
    
    private void renderCharacterCard(ScreenBuffer buffer, int x, int y, 
                                      String cardName, boolean selected) {
        String color = Theme.getJobColor(cardName);
        String sel = selected ? Theme.SELECTED : "";
        
        buffer.setString(x, y, sel + "┌─────────┐" + Theme.RESET);
        buffer.setString(x, y + 1, sel + "│" + color + padCenter(cardName, 9) + 
                        Theme.RESET + "│" + Theme.RESET);
        buffer.setString(x, y + 2, sel + "│         │" + Theme.RESET);
        buffer.setString(x, y + 3, sel + "└─────────┘" + Theme.RESET);
    }
    
    private void renderEventCard(ScreenBuffer buffer, int x, int y, 
                                  String cardName, boolean selected) {
        String sel = selected ? Theme.SELECTED : "";
        buffer.setString(x, y, sel + "┌─────────┐" + Theme.RESET);
        buffer.setString(x, y + 1, sel + "│" + Theme.SHAMAN + padCenter(cardName, 9) + 
                        Theme.RESET + "│" + Theme.RESET);
        buffer.setString(x, y + 2, sel + "│         │" + Theme.RESET);
        buffer.setString(x, y + 3, sel + "└─────────┘" + Theme.RESET);
    }
    
    private void renderBuildingCard(ScreenBuffer buffer, int x, int y, 
                                     String cardName, boolean selected) {
        String sel = selected ? Theme.SELECTED : "";
        buffer.setString(x, y, sel + "┌─────────┐" + Theme.RESET);
        buffer.setString(x, y + 1, sel + "│" + Theme.BUILDING + padCenter(cardName, 9) + 
                        Theme.RESET + "│" + Theme.RESET);
        buffer.setString(x, y + 2, sel + "│         │" + Theme.RESET);
        buffer.setString(x, y + 3, sel + "└─────────┘" + Theme.RESET);
    }
    
    private String padCenter(String s, int w) {
        if (s == null) s = "";
        if (s.length() >= w) return s.substring(0, w);
        int left = (w - s.length()) / 2;
        return " ".repeat(left) + s + " ".repeat(w - left - s.length());
    }
}
```

---

### 8. OfferingTrackRenderer.java - Rendering Offerte

```java
package it.polimi.ingsw.am54.network.client.tui.ui;

import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.tui.ScreenBuffer;
import it.polimi.ingsw.am54.network.client.tui.theme.Theme;

public class OfferingTrackRenderer {
    
    public void render(ScreenBuffer buffer, int startX, int startY,
                       ClientModel model, int selectedTile) {
        
        buffer.setString(startX, startY, "OFFERING TRACK");
        
        var track = model.getOfferingTrack();
        for (int i = 0; i < track.size(); i++) {
            String tileOwner = track.get(i + 1); // 1-based positions
            
            // Render tile
            String sel = selectedTile == i ? Theme.SELECTED : "";
            buffer.setString(startX + (i * 10), startY + 2, sel + "┌────┐" + Theme.RESET);
            
            // Letter (A, B, C...)
            String letter = String.valueOf((char) ('A' + i));
            buffer.setString(startX + (i * 10) + 2, startY + 3, letter);
            
            // Upper picks (arrows up)
            buffer.setString(startX + (i * 10), startY + 4, sel + "│" + Theme.ARROW_UP.repeat(4) + "│" + Theme.RESET);
            
            // Owner or empty
            String owner = tileOwner != null ? tileOwner.substring(0, 3) : "   ";
            buffer.setString(startX + (i * 10), startY + 5, sel + "│" + owner + "│" + Theme.RESET);
            
            // Lower picks (arrows down)
            buffer.setString(startX + (i * 10), startY + 6, sel + "│" + Theme.ARROW_DOWN.repeat(4) + "│" + Theme.RESET);
            
            buffer.setString(startX + (i * 10), startY + 7, sel + "└────┘" + Theme.RESET);
            
            // Arrow to next (except last)
            if (i < track.size() - 1) {
                buffer.setString(startX + (i * 10) + 5, startY + 4, Theme.ARROW_RIGHT);
            }
        }
        
        // Legend
        buffer.setString(startX, startY + 10, "▲ = upper picks  ▼ = lower picks");
    }
}
```

---

### 9. TurnOrderRenderer.java - Rendering Ordine Turno

```java
package it.polimi.ingsw.am54.network.client.tui.ui;

import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.tui.ScreenBuffer;
import it.polimi.ingsw.am54.network.client.tui.theme.Theme;
import it.polimi.ingsw.am54.model.TotemColors;

public class TurnOrderRenderer {
    
    public void render(ScreenBuffer buffer, int startX, int startY,
                       ClientModel model, int selectedSlot) {
        
        buffer.setString(startX, startY, "TURN ORDER");
        
        var turnOrder = model.getTurnOrderTrack();
        for (int i = 0; i < turnOrder.size(); i++) {
            String player = turnOrder.get(i);
            TotemColors color = model.getPlayerColors().get(player);
            
            boolean isCurrent = player != null && 
                               player.equals(model.getCurrentPlayer());
            boolean isSelected = selectedSlot == i;
            
            String statusColor = isCurrent ? Theme.CURRENT_TURN : 
                                (player == null ? Theme.DISCONNECTED : Theme.ACTIVE);
            String sel = isSelected ? Theme.SELECTED : "";
            
            String totemStr = color != null ? color.name() : "   ";
            String totemColor = Theme.getTotemColor(totemStr);
            
            buffer.setString(startX + (i * 15), startY + 2, 
                           sel + "┌───────────┐" + Theme.RESET);
            buffer.setString(startX + (i * 15), startY + 3, 
                           sel + "│" + totemColor + " " + totemStr + "  " + Theme.RESET + "│" + Theme.RESET);
            buffer.setString(startX + (i * 15), startY + 4, 
                           sel + "│" + statusColor + padCenter(player != null ? player : "---", 9) + Theme.RESET + "│" + Theme.RESET);
            buffer.setString(startX + (i * 15), startY + 5, 
                           sel + "└───────────┘" + Theme.RESET);
        }
    }
    
    private String padCenter(String s, int w) {
        if (s == null) s = "";
        if (s.length() >= w) return s.substring(0, w);
        int left = (w - s.length()) / 2;
        return " ".repeat(left) + s + " ".repeat(w - left - s.length());
    }
}
```

---

### 10. PlayerRenderer.java - Rendering Giocatori

```java
package it.polimi.ingsw.am54.network.client.tui.ui;

import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.tui.ScreenBuffer;
import it.polimi.ingsw.am54.network.client.tui.theme.Theme;

public class PlayerRenderer {
    
    public void render(ScreenBuffer buffer, int startX, int startY,
                       ClientModel model, int selectedPlayer) {
        
        buffer.setString(startX, startY, "ALL PLAYERS");
        
        int y = startY + 2;
        for (int i = 0; i < model.getPlayers().size(); i++) {
            String player = model.getPlayers().get(i);
            var color = model.getPlayerColors().get(player);
            Integer food = model.getPlayerFood().get(player);
            Integer aura = model.getPlayerAura().get(player);
            var tribe = model.getPlayerTribe().get(player);
            var buildings = model.getPlayerBuildings().get(player);
            boolean disconnected = model.getDisconnectedPlayers().contains(player);
            
            boolean isSelected = selectedPlayer == i;
            boolean isCurrent = player.equals(model.getCurrentPlayer());
            
            String sel = isSelected ? Theme.SELECTED : "";
            String statusColor = isCurrent ? Theme.CURRENT_TURN :
                                (disconnected ? Theme.DISCONNECTED : Theme.ACTIVE);
            String totemColorStr = color != null ? color.name() : "NONE";
            String totemColor = Theme.getTotemColor(totemColorStr);
            String connIndicator = disconnected ? Theme.DISCONNECTED_INDICATOR : Theme.CONNECTED;
            
            // Player name and status
            buffer.setString(startX, y, sel + "┌────────────────────────────────────────┐" + Theme.RESET);
            y++;
            buffer.setString(startX, y, sel + "│ " + totemColor + totemColorStr + Theme.RESET + " " + 
                           statusColor + player + Theme.RESET + " " + connIndicator + Theme.RESET);
            y++;
            buffer.setString(startX, y, sel + "│ Food: " + (food != null ? food : 0) + 
                           "  Aura: " + (aura != null ? aura : 0) + Theme.RESET +
                           " ".repeat(25) + "│" + Theme.RESET);
            y++;
            
            // Tribe
            StringBuilder tribeStr = new StringBuilder("│ Tribe: ");
            if (tribe != null) {
                for (String card : tribe) {
                    String jobColor = Theme.getJobColor(card);
                    tribeStr.append(jobColor).append("[").append(card.substring(0, 4)).append("]").append(Theme.RESET);
                }
            }
            tribeStr.append(" ".repeat(40)).append("│");
            buffer.setString(startX, y, tribeStr.toString().substring(0, 46));
            y++;
            
            // Buildings
            StringBuilder buildStr = new StringBuilder("│ Build: ");
            if (buildings != null) {
                for (String b : buildings) {
                    buildStr.append(Theme.BUILDING).append("[").append(b.substring(0, 4)).append("]").append(Theme.RESET);
                }
            }
            buildStr.append(" ".repeat(40)).append("│");
            buffer.setString(startX, y, buildStr.toString().substring(0, 46));
            y++;
            
            buffer.setString(startX, y, sel + "└────────────────────────────────────────┘" + Theme.RESET);
            y += 2;
        }
    }
    
    // Render per il player corrente (mie carte)
    public void renderMyHand(ScreenBuffer buffer, int startX, int startY, ClientModel model) {
        String myNickname = model.getCurrentPlayer(); // o il proprio nickname
        if (myNickname == null) return;
        
        buffer.setString(startX, startY, "MY TRIBE");
        var tribe = model.getPlayerTribe().get(myNickname);
        if (tribe != null) {
            for (int i = 0; i < tribe.size(); i++) {
                String card = tribe.get(i);
                String color = Theme.getJobColor(card);
                buffer.setString(startX + (i * 10), startY + 2, "┌────────┐");
                buffer.setString(startX + (i * 10), startY + 3, "│" + color + card.substring(0, 6) + Theme.RESET + "│");
                buffer.setString(startX + (i * 10), startY + 4, "└────────┘");
            }
        }
        
        buffer.setString(startX, startY + 8, "MY BUILDINGS");
        var buildings = model.getPlayerBuildings().get(myNickname);
        if (buildings != null) {
            for (int i = 0; i < buildings.size(); i++) {
                String b = buildings.get(i);
                buffer.setString(startX + (i * 10), startY + 10, "┌────────┐");
                buffer.setString(startX + (i * 10), startY + 11, "│" + Theme.BUILDING + b.substring(0, 6) + Theme.RESET + "│");
                buffer.setString(startX + (i * 10), startY + 12, "└────────┘");
            }
        }
    }
}
```

---

### 11. ClientTUI.java - Main Loop

```java
package it.polimi.ingsw.am54.network.client.tui;

import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.client.tui.navigation.*;
import it.polimi.ingsw.am54.network.client.tui.ui.*;
import it.polimi.ingsw.am54.network.client.tui.theme.Theme;
import it.polimi.ingsw.am54.network.messages.*;
import java.util.Scanner;

public class ClientTUI {
    private Client client;
    private TerminalHandler terminal;
    private NavigationState navigation;
    private BoardRenderer boardRenderer;
    private OfferingTrackRenderer offeringRenderer;
    private TurnOrderRenderer turnOrderRenderer;
    private PlayerRenderer playerRenderer;
    private boolean running = true;
    
    public static void main(String[] args) {
        new ClientTUI().run();
    }
    
    private void run() {
        try {
            // Inizializza terminale
            terminal = new TerminalHandler();
            terminal.init();
            
            // Inizializza renderer
            boardRenderer = new BoardRenderer();
            offeringRenderer = new OfferingTrackRenderer();
            turnOrderRenderer = new TurnOrderRenderer();
            playerRenderer = new PlayerRenderer();
            
            // Menu connessione
            showConnectionMenu();
            
            // Menu login
            String nickname = doLogin();
            client = Client.getInstance();
            client.setNickname(nickname);
            
            // Inizializza navigazione
            navigation = new NavigationState(client.getModel());
            
            // Main loop
            while (running) {
                render();
                KeyStroke key = terminal.readKey();
                handleInput(key);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { terminal.close(); } catch (Exception ignored) {}
        }
    }
    
    private void showConnectionMenu() {
        System.out.println("=== AM54 - Connessione ===");
        System.out.println("1. Socket TCP");
        System.out.println("2. RMI");
        System.out.print("Scegli (1/2): ");
        
        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine();
        
        try {
            ServerConnection connection;
            if (choice.equals("2")) {
                // RMI
                System.out.print("Host (default localhost): ");
                String host = scanner.nextLine();
                if (host.isEmpty()) host = "localhost";
                connection = new RMIServerConnection(host, 1099, client.getModel());
            } else {
                // Socket
                System.out.print("Host (default localhost): ");
                String host = scanner.nextLine();
                if (host.isEmpty()) host = "localhost";
                connection = new SocketServerConnection(host, 12345, client.getModel());
            }
            client.setConnection(connection);
            System.out.println("Connesso!");
        } catch (Exception e) {
            System.err.println("Errore connessione: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private String doLogin() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Nickname: ");
        return scanner.nextLine();
    }
    
    private void render() throws Exception {
        terminal.clearScreen();
        ClientModel model = client.getModel();
        
        // Header
        terminal.write("AM54 - MESOS");
        terminal.write("  Turn: " + model.getCurrentPlayer());
        terminal.write("\n");
        
        // Board (left)
        boardRenderer.render(new ScreenBuffer(terminal.getWidth() / 2, terminal.getHeight()), 
                           0, 2, model, navigation.getCurrentSection() == ScreenSection.UPPER_BOARD ? 
                           navigation.getCurrentIndex() : -1);
        
        // Turn Order (right)
        turnOrderRenderer.render(new ScreenBuffer(terminal.getWidth() / 2, terminal.getHeight()),
                                 terminal.getWidth() / 2 + 2, 2, model,
                                 navigation.getCurrentSection() == ScreenSection.TURN_ORDER ? 
                                 navigation.getCurrentIndex() : -1);
        
        // Offering Track
        offeringRenderer.render(new ScreenBuffer(terminal.getWidth(), terminal.getHeight()),
                               0, 35, model,
                               navigation.getCurrentSection() == ScreenSection.OFFERING_TRACK ?
                               navigation.getCurrentIndex() : -1);
        
        // Players
        playerRenderer.render(new ScreenBuffer(terminal.getWidth(), terminal.getHeight()),
                            0, 50, model,
                            navigation.getCurrentSection() == ScreenSection.PLAYERS_LIST ?
                            navigation.getCurrentIndex() : -1);
        
        terminal.write("\nNavigazione: [↑↓] sezione  [←→] elemento  [Enter] conferma  [q] quit  [h] help");
        terminal.flush();
    }
    
    private void handleInput(KeyStroke key) {
        switch (key.type) {
            case UP -> navigation.moveUp();
            case DOWN -> navigation.moveDown();
            case LEFT -> navigation.moveLeft();
            case RIGHT -> navigation.moveRight();
            case ENTER -> confirmSelection();
            case CHAR -> {
                if (key.character == 'q' || key.character == 'Q') {
                    running = false;
                }
                if (key.character == 'h' || key.character == 'H') {
                    showHelp();
                }
            }
        }
    }
    
    private void confirmSelection() {
        ScreenSection section = navigation.getCurrentSection();
        int index = navigation.getCurrentIndex();
        ClientModel model = client.getModel();
        
        switch (section) {
            case OFFERING_TRACK -> {
                // Piazza totem
                int position = index + 1;
                client.sendRequest(new PlaceTotemRequest(position));
            }
            case UPPER_BOARD, LOWER_BOARD -> {
                // Picks card
                String row = section == ScreenSection.UPPER_BOARD ? "upper" : "lower";
                client.sendRequest(new PickCardRequest(row, index));
            }
            case PLAYERS_LIST -> {
                // Info player
                System.out.println("Player: " + model.getPlayers().get(index));
            }
            default -> {}
        }
    }
    
    private void showHelp() {
        System.out.println("\n=== HELP ===");
        System.out.println("↑/↓ - Muovi tra sezioni");
        System.out.println("←/→ - Muovi all'interno sezione");
        System.out.println("Enter - Conferma selezione (esegue azione)");
        System.out.println("q - Esci");
        System.out.println("h - Questo help");
    }
}
```

---

## Layout Finale

```
┌────────────────────────────────────────────────────────────────────────┐
│ AM54 - MESOS                                        Turn: Mario (RED)  │
├────────────────────────────────────────────────────────────────────────┤
│ UPPER ROW                    │ TURN ORDER                              │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐  │ [RED] [YEL] [WHI] [BLA]                 │
│ │HUNT│ │SHAM│ │ ══ │ │ ══ │  │ Mario  Luigi  Anna  --                  │
│ │ L1 │ │ L2 │ │C:5 │ │C:3 │  │ +2    +3    +3    --                    │
│ └────┘ └────┘ └────┘ └────┘  │                                         │
├──────────────────────────────┼─────────────────────────────────────────┤
│ LOWER ROW                    │ OFFERING TRACK                          │
│ ┌────┐ ┌────┐ ┌────┐         │  A → B → C → D → E → F → G              │
│ │ ══ │ │ ══ │ │ ══ │         │ ↑↑↑   ↑↑    ↓↓    ↓↓                    │
│ └────┘ └────┘ └────┘         │ Food:0 Food:0 Food:1                    │
├──────────────────────────────┴─────────────────────────────────────────┤
│ MY HAND (TRIBE)              │ MY BUILDINGS                            │
│ ┌────┐ ┌────┐ ┌────┐         │ ┌────┐ ┌────┐                           │
│ │HUNT│ │SHAM│ │FARM│         │ │TOWN│ │FARM│                           │
│ └────┘ └────┘ └────┘         │ └────┘ └────┘                           │
├────────────────────────────────────────────────────────────────────────┤
│ ALL PLAYERS                                                            │
│ ┌────────────────────────────────────────────────────────────────┐     │
│ │ Mario (RED ●)   Food:5 Aura:3                                  │     │
│ │   Tribe: [HUNT][SHAM][FARM]                                    │     │
│ │   Buildings: [TOWN][FARM]                                      │     │
│ ├────────────────────────────────────────────────────────────────┤     │
│ │ Luigi (YEL ●)   Food:3 Aura:2                                  │     │
│ │   Tribe: [CONS][INVE][ARTI]                                    │     │
│ │   Buildings: [TOWER]                                           │     │
│ ├────────────────────────────────────────────────────────────────┤     │
│ │ Anna (WHI ○)   Food:4 Aura:1    ← DISCONNECTED                 │     │
│ │   Tribe: [SHAM][HUNT]                                          │     │
│ │   Buildings: [FARM]                                            │     │
│ └────────────────────────────────────────────────────────────────┘     │
├────────────────────────────────────────────────────────────────────────┤
│ > [CURSOR]                                                             │
│ Help: [←→] navigate  [Enter] confirm  [q] quit  [h] help               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Note di Implementazione

1. **JLine**: Garantisce input non-bloccante e supporto per frecce
2. **ScreenBuffer**: Rendering in buffer, poi flush a schermo - evita flicker
3. **Thread Safety**: ClientModel è già synchronized
4. **Selection**: Una volta confermato, l'azione viene eseguita automaticamente
5. **Colori**: Usare sequenze ANSI per terminali compatibili