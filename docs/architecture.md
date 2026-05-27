# Architecture — Codex Naturalis (AM54)

## 1. Server-Side State Pattern

The game is modelled as a finite state machine using the **State pattern**. Each phase is a concrete subclass of `GameState`.

```
GameState (abstract)
├── SetUpGameState    — Color selection, game initialization
├── StartTurnState    — Totem placement on offering tiles
├── PickingState      — Card picking from upper/lower row
├── EventState        — Auto-resolved event cards
└── EndGameState      — Final scoring, game over
```

### How It Works

```java
// GameState.java — template method pattern
public abstract class GameState {
    public void placeTotem(Game game, String nickname, int pos) throws Exception {
        throw new IllegalStateException("Not allowed in this phase");
    }
    public void pickCard(Game game, String nickname, String row, int idx) throws Exception {
        throw new IllegalStateException("Not allowed in this phase");
    }
    public boolean isAutoResolvable() { return false; }
    // ... more defaults
}
```

Each state overrides **only** the methods it should allow:

```java
// PickingState.java
public class PickingState extends GameState {
    @Override
    public void pickCard(Game game, String nickname, String row, int idx) {
        // actual picking logic
    }
    @Override
    public void endTurn(Game game, String nickname) {
        // end turn + transition to next state
    }
    // placeTotem() NOT overridden → throws IllegalStateException
}
```

This design eliminates:
- **if-else chains** checking `currentState` everywhere
- **Runtime errors** from calling invalid actions in the wrong phase
- **Forgotten state transitions** — each method explicitly transitions the game

### Auto-Resolution

`EventState` is `isAutoResolvable() = true`. The Controller automatically resolves it:

```java
private void checkAndResolveAutomaticPhases() {
    if (game.getCurrentState().isAutoResolvable() && !game.getCurrentState().isGameOver()) {
        game.getCurrentState().resolvePhase(game);
    }
    if (game.getCurrentState().isGameOver()) {
        // save scores to DB
    }
}
```

---

## 2. Client-Side Polymorphic Dispatch

On the client, the same state-driven approach is mirrored with a **Strategy pattern** via `ClientStateRegistry`.

```
ClientStateHandler (interface)
├── SimpleHandler       — generic handler (used internally)
├── StartTurnHandler    — "Scegli dove piazzare il tuo totem!"
├── PickingHandler      — "Scegli che carta prendere!"
├── EventHandler        — "Eventi in corso..."
├── EndGameHandler       — "Partita terminata!"
├── SetUpHandler         — Lobby / color setup
└── DefaultHandler       — Unknown state fallback
```

### Registry

```java
public final class ClientStateRegistry {
    private static final Map<String, ClientStateHandler> HANDLERS = Map.of(
        "STARTTURNSTATE", new StartTurnHandler(),
        "PICKINGSTATE",   new PickingHandler(),
        "EVENTSTATE",     new EventHandler(),
        "ENDGAMESTATE",   new EndGameHandler(),
        "SETUPGAMESTATE", new SetUpHandler(),
        "LOBBY",          new SetUpHandler()
    );

    public static ClientStateHandler getHandler(String stateName) {
        ClientStateHandler h = HANDLERS.get(stateName.toUpperCase());
        return h != null ? h : new DefaultHandler();
    }
}
```

The **server** sends the state name via `newState.getClass().getSimpleName()` (e.g., `"PickingState"`).  
The **client** receives it, looks up the handler, and delegates rendering/input logic.

### CLI Rendering

```java
// CLIApp.render()
ClientStateHandler handler = ClientStateRegistry.getHandler(currentState);
clearConsole();
handler.renderCLI(this);  // polymorphic dispatch — no switches
```

`handler.renderCLI(this)` calls one of:
- `cli.renderSetup()` — print lobby / color choice
- `cli.renderStartTurn()` — print board + totem placement prompt
- `cli.renderPicking()` — print board + card selection menu
- `cli.renderEvent()` — print "Fase Eventi in corso..."
- `cli.renderEndGame()` — print final scores + leaderboard
- `cli.renderDefault()` — print "Stato sconosciuto"

### GUI Action Banner

```java
// BoardController.updateActionBanner()
ClientStateHandler handler = ClientStateRegistry.getHandler(state);
endTurnButton.setVisible(isMyTurn && handler.showsEndTurnButton());
actionInstructionLabel.setText(handler.getInstruction(model, myName));
```

### Input Blocking

```java
// CLIApp.render()
if (handler.shouldBlockInput(isMyTurn)) {
    currentInputHandler = null;  // reject input
}
```

| Handler | `shouldBlockInput(myTurn)` | Meaning |
|---------|---------------------------|---------|
| StartTurnHandler | `!isMyTurn` | Block if it's not my turn |
| PickingHandler | `!isMyTurn` | Block if it's not my turn |
| EventHandler | `true` (always) | No player input during events |
| EndGameHandler | `true` (always) | Game over |
| SetUpHandler | `false` (never) | Always allow input in lobby |
| DefaultHandler | `false` (never) | Fallback |

---

## 3. Dual Interface Architecture

Both CLI and GUI share the same underlying `ClientModel` and `ClientStateRegistry`.

```
Server Message
     │
     ▼
ClientModel.updateState()
     │
     ├── CLI: model.setOnModelUpdatedCallback(this::render)
     │         ClientStateRegistry → CLIAbstract.renderCLI()
     │
     └── GUI: model.setOnModelUpdatedCallback(this::refreshBoard)
               ClientStateRegistry → getInstruction(), showsEndTurnButton()
```

### Distinction

| Aspect | CLI | GUI |
|--------|-----|-----|
| Rendering | `System.out`, `Scanner` | JavaFX Scene Graph |
| Input | Synchronous `Scanner.nextLine()` loop | Event-driven (button clicks, mouse) |
| State handling | `renderCLI()` dispatches full screen re-render | `getInstruction()` updates labels only |
| Tile interaction | Totem placement via keyboard | Mouse click on offering tiles (`canInteractWithTiles()`) |

---

## 4. State Mapping

| Server `getSimpleName()` | Client Registry Key | Handler |
|---|---|---|
| `StartTurnState` | `STARTTURNSTATE` | StartTurnHandler |
| `PickingState` | `PICKINGSTATE` | PickingHandler |
| `EventState` | `EVENTSTATE` | EventHandler |
| `SetUpGameState` | `SETUPGAMESTATE` | SetUpHandler |
| `EndGameState` | `ENDGAMESTATE` | EndGameHandler |
| *(client-side default)* | `LOBBY` | SetUpHandler |

---

## 5. Lobby → Game Transition Detection

When the game transitions from lobby/setup to actual gameplay, the GUI switches scenes:

```java
// Both ServerHandler (TCP) and RMIServerConnection (RMI)
if (ClientStateRegistry.getHandler(oldState).isLobbyState()
    && !ClientStateRegistry.getHandler(newState).isLobbyState()) {
    GUIApp.loadBoardScene();
}
```

This is needed because `Lobby` and `SetUpGameState` are both lobby states, but the transition to `StartTurnState` triggers the board scene load.

---

## 6. Thread Safety

### Server Side

| Method | Lock | Rationale |
|--------|------|-----------|
| `handleJoinGame` | `synchronized(game.getPlayers())` | Prevent concurrent joins |
| `handleReconnect` | `synchronized(game.getPlayers())` | Protect addObserver/removeObserver |
| `removeClient` | `synchronized(game.getPlayers())` | Protect observer cleanup |

Supporting structures (`virtualViews`, `disconnectedPlayers`) use `ConcurrentHashMap`.

### Client Side

- `ClientModel` methods are `synchronized` for thread-safe updates from network thread
- `CLIApp.render()` is `synchronized` — network callback and input thread never interleave
- JavaFX `Platform.runLater()` ensures GUI updates on the FX Application Thread

---

## 7. ClientModel

The `ClientModel` is a lightweight client-side projection of the server's `Game`. It contains **only** the data needed for rendering:

```java
public class ClientModel {
    private String currentState;       // e.g. "PickingState"
    private String currentPlayer;      // who is active
    private int currentEra, currentRound;
    private List<String> players;      // nickname list
    private Map<String, Integer> playerFood, playerAura, playerMagic;
    private Map<String, List<String>> playerTribe, playerBuildings;
    private List<String> turnOrderTrack;
    private Map<Integer, String> offeringTrack;
    // ... event cards, building rows, error messages, etc.
}
```

It updates atomically via `updateState(StateChangedUpdate msg)` which clears and reloads all data from the server snapshot.
