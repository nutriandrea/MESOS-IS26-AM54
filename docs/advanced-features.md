# Advanced Features — Codex Naturalis (AM54)

## 1. Client Disconnection Handling

### 1.1 Heartbeat Detection

A `ScheduledExecutorService` on the server sends a ping to every connected client every **5 seconds**:

```
[Server] sendPing() ───▶ [Client]
                              └── reply Pong immediately
[Server] 10s timeout
  └── no Pong received → markDisconnected(nickname)
```

The ping-pong is invisible to the user — it uses dedicated `PingRequest` messages that are intercepted by `ServerHandler` before they reach `ClientModel`.

### 1.2 Disconnection Flow

```
Server detects missing Pong
         │
         ▼
Controller.markDisconnected(nickname)
  ├── add to disconnectedPlayers set
  ├── broadcast PlayerDisconnectedUpdate to all clients
  └── autoHandleDisconnection(nickname)
         │
         ▼
GameState.handleDisconnection(game, player, isCurrentPlayer)
```

The behaviour depends on the game state:

| State | What happens |
|-------|-------------|
| **Lobby / SetUp** | No effect — player can rejoin |
| **StartTurn** | If current player: totem placed randomly, turn advances. If not: no-op |
| **Picking** | If current player: turn ends automatically, next player goes. If not: no-op |
| **Event** | Disconnection blocked (`isDisconnectBlocked() = true`), handled after events resolve |
| **EndGame** | Disconnection blocked — scores already calculated |

### 1.3 Temporary Disconnection (`removeClient`)

When the socket closes (client crash, network outage):

```java
public void removeClient(String nickname) {
    if (game.getCurrentState().isRemovePermanent()) {
        // Lobby/Setup: remove player entirely, free color
        game.removePlayerEntirely(nickname);
    } else {
        // In-game: mark as temporarily disconnected
        disconnectedPlayers.add(nickname);
        game.removeObserver(view);
        broadcastPlayerDisconnected(nickname);
        autoHandleDisconnection(nickname);
    }
}
```

### 1.4 Reconnect Window

The disconnected player has a **60-second window** to reconnect:

```
[Disconnection] ── 60s ──▶ [Permanent removal / auto-win]
```

During this window:
- The player's data is preserved in the `Game` model
- Other players see a "🔴 OFFLINE" marker on the disconnected player
- The player's totem is still on the board (if placed)
- If the disconnected player is the current active player, the server automatically ends their turn

### 1.5 Reconnection

```java
Controller.handleReconnect(nickname, connection):
  // 1. Remove old VirtualView
  // 2. Create new VirtualView with the new connection
  // 3. Re-register as observer on Game
  // 4. Send full state snapshot (sendFullStateOnReconnect)
  //     ├── StateChangedUpdate (complete board state)
  //     ├── Player hand cards
  //     └── Pending event reports
  // 5. Broadcast PlayerReconnectedUpdate to other clients
```

The client's `ClientModel` is fully replaced from the snapshot — no incremental sync needed.

---

## 2. Server Crash / Game Suspension

### 2.1 Single Player Remaining

When enough players disconnect that only **1 active player** remains:

```
Controller.suspendGame()
  ├── suspended = true
  ├── broadcast GameSuspendedUpdate
  └── start 60s timeout
        │
        └── timeout expires:
              ├── winner = findSoloActivePlayer()
              ├── setState(EndGameState)
              ├── save score to database
              ├── broadcast EndGameUpdate
              └── clear backup
```

Other clients see: *"Gioco sospeso: in attesa di riconnessioni..."*  
All game actions are blocked while `suspended = true`.

### 2.2 All Players Disconnected

If `activePlayerCount() == 0`:

```
  ├── Clear backup file
  ├── Clear all state (players, observers, views)
  └── Reset game to SetUpGameState (back to lobby)
```

### 2.3 Reconnection During Suspension

If a player reconnects while the game is suspended:
```
handleReconnect()
  ├── Restore player's VirtualView
  ├── suspended = false           (if ≥2 players active)
  ├── cancel timeout
  └── broadcast GameResumedUpdate
```

---

## 3. Persistence (JSON Backup)

### 3.1 Purpose

The `PersistenceManager` saves the complete `Game` state to a JSON file after every action. This enables:

- **Full state recovery** on player reconnect
- **Server restart survival** — if the server crashes, the game can resume from the last saved state

### 3.2 How It Works

```java
// After every game action:
Controller.saveState() {
    if (game.getCurrentState().isGameOver()) {
        persistenceManager.clearBackup();
        return;
    }
    persistenceManager.saveGame(game);
}
```

The save uses **Gson** with `RuntimeTypeAdapterFactory` for polymorphic serialization of:
- `GameState` subclasses (SetUpGameState, StartTurnState, etc.)
- `IEffect` implementations (BuildingEventCostModifier, CharacterHunter, etc.)

### 3.3 Backup File

Saved to `backup.json.tmp` in the working directory. The file is **cleared** when:
- The game ends normally
- All players disconnect
- The game is explicitly cancelled

### 3.4 Recovery on Server Start

```java
Server.start():
    persistenceManager = new PersistenceManager("backup.json.tmp")
    game = persistenceManager.restoreGame();
    if (game == null) {
        game = new Game();  // fresh game
    } else {
        // recovered from backup — resume from saved state
    }
```

---

## 4. Leaderboard (MySQL Database)

### 4.1 Database Schema

```sql
CREATE TABLE IF NOT EXISTS match_results (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nickname    VARCHAR(50)  NOT NULL,
    score       INTEGER      NOT NULL,
    match_date  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    num_players INTEGER      NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.2 Score Saving

At the end of each game:

```java
// Controller.checkAndResolveAutomaticPhases()
if (game.getCurrentState().isGameOver() && !scoresSavedToDatabase) {
    new Thread(() -> {
        for (Player p : game.getPlayers()) {
            int finalScore = game.calculatePlayerScore(p);
            dbManager.saveMatchResult(p.getNickname(), finalScore, numPlayers);
        }
    }).start();
}
```

Scores are saved **asynchronously** — the game does not wait for the database.

### 4.3 Leaderboard Query

Any player can request the leaderboard:

```
Client → GetLeaderboardRequest(numPlayers)
Server → LeaderboardUpdate(playerPosition, entries[], numPlayers)
```

The `DatabaseManager` queries:
```java
public List<LeaderboardEntry> getLeaderboard(int numPlayers) {
    SELECT nickname, MAX(score) as best_score, MAX(match_date) as last_match
    FROM match_results
    WHERE num_players = ?
    GROUP BY nickname
    ORDER BY best_score DESC
    LIMIT 20
}
```

### 4.4 Graceful Degradation

If MySQL is not available (no `.env` file, wrong credentials, network error):

```
DatabaseManager.init():
  └── SQLException → LOGGER.warning("Database MySQL non raggiungibile – leaderboard disabilitata")
  └── available = false
```

The `ServerHandler` and `VirtualView` check `dbManager.isAvailable()` before querying:

```java
if (dbManager != null && dbManager.isAvailable()) {
    // query leaderboard
} else {
    view.showError("Leaderboard non disponibile (database offline)");
}
```

### 4.5 Configuration

Copy `.env.example` to `.env`:

```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=codex_naturalis
```

The `.env` is loaded via a `DatabaseConfig` utility class. Without it, defaults point to `localhost:3306` — the leaderboard is gracefully disabled if MySQL is unreachable.

---

## 5. Polymorphic State Pattern (Client-Side)

This refactoring replaced all if-else chains and switches on state names with a clean `ClientStateHandler` strategy pattern.

### 5.1 What Changed

**Before (CLIApp.render):**
```java
if (state.equals("Lobby") || state.equals("SetUpGameState")) {
    renderSetup();
} else if (state.equals("StartTurnState")) {
    renderStartTurn();
} else if (state.equals("PickingState")) {
    renderPicking();
} else if (state.equals("EventState")) {
    System.out.println("Fase Eventi in corso...");
} else if (state.equals("EndGameState")) {
    renderEndGame();
} else {
    System.out.println("Stato sconosciuto");
}
```

**After:**
```java
ClientStateHandler handler = ClientStateRegistry.getHandler(currentState);
handler.renderCLI(this);
```

### 5.2 Files Created

| File | Role |
|------|------|
| `gui/states/ClientStateHandler.java` | Interface with default methods |
| `gui/states/ClientStateRegistry.java` | State → Handler mapping |
| `gui/states/CLIAbstract.java` | CLI rendering contract |

### 5.3 Handlers

| Handler | `renderCLI()` | `shouldBlockInput()` | `showsEndTurnButton()` | `canInteractWithTiles()` |
|---------|--------------|---------------------|----------------------|------------------------|
| StartTurnHandler | `cli.renderStartTurn()` | `!isMyTurn` | `false` | `true` |
| PickingHandler | `cli.renderPicking()` | `!isMyTurn` | `true` | `false` |
| EventHandler | `cli.renderEvent()` | `true` | `false` | `false` |
| EndGameHandler | `cli.renderEndGame()` | `true` | `false` | `false` |
| SetUpHandler | `cli.renderSetup()` | `false` | `false` | `false` |
| DefaultHandler | `cli.renderDefault()` | `false` | `false` | `false` |

### 5.4 Bug Fix

During the refactoring, a subtle concurrency bug was fixed: system alerts (network notifications) were incorrectly nulling the active CLI input handler. When a notification arrived while the user was picking a card, the next keystroke would be ignored. The fix separated "input should be blocked" from "there is a notification to show":

```java
// Before (broken):
if (hasSystemAlert || handler.shouldBlockInput(isMyTurn)) {
    currentInputHandler = null;  // alert destroys handler!
}

// After (fixed):
if (handler.shouldBlockInput(isMyTurn)) {
    currentInputHandler = null;  // only state determines blocking
}
```

---

## 6. CLI Interface

The CLI (`CLIApp.java`) provides a full terminal-based interface:

### Commands

During gameplay:
- **1** — Pick from upper row (during PickingState)
- **2** — Pick from lower row
- **3** — End turn
- **0..N** — Select offering tile index (during StartTurnState)
- **START** — Start game (host only, during setup)
- **RED / YELLOW / WHITE / BLACK** — Choose totem color

### Rendering

```
=== MESOS - BENVENUTO NICKNAME ===

📊 STATISTICHE GLOBALI
Era: 2 | Round: 5
Turno di: NICKNAME

▲ --- FILA SUPERIORE --- ▲
Eventi:     [📜 FESTIVAL] [📜 GOOD SEASON]
Personaggi: [SHAMAN ★] [FARMER 🌾+1] [INVENTOR ⚙️ Liv.2]
Edifici:    [🏛️ Tamburo (Cost:🍖1 Aura:✨2)]

=================================================================
🔹 NICKNAME | 🍖 Cibo: 5 | ✨ Aura: 3
   Tribù:   [SHAMAN]
   Edifici: [ VUOTA ]

▼ --- FILA INFERIORE --- ▼
...

--- FASE DI PESCA ---
Tocca a: NICKNAME
E' IL TUO TURNO!
Opzioni:
1) Pesca dalla fila SUPERIORE
2) Pesca dalla fila INFERIORE
3) Termina Turno
```
