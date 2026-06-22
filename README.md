# IS26-AM54 — MESOS

[![Java](https://img.shields.io/badge/Java-25-%23ED8B00)](https://openjdk.org/projects/jdk/25/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36)](https://maven.apache.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-23-007396)](https://openjfx.io/)
![Tests](https://img.shields.io/badge/tests-28-blue)
![Coverage](https://img.shields.io/badge/coverage-80%25-success)

Distributed multiplayer implementation of the board game **Mesos** (Codice Naturalis, Cranio Creations). 2–5 players, real-time network with dual-transport (Socket TCP + RMI), dual interface (JavaFX GUI + CLI).

**Software Engineering Final Project** — Politecnico di Milano, AY 2025–2026  
**Professor**: Alessandro Margara

---

## Quick Start

**Prerequisites**: Java 25+, Maven 3.9+

```bash
# Build
./mvnw clean compile

# Run server (port 12345)
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server"

# Run client (TCP)
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.client.Client" -Dexec.args="TCP"

# Run client (RMI)
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.client.Client" -Dexec.args="RMI"
```

---

## Team

| Member | Role |
|--------|------|
| Andrea Cacioppo | Development |
| Andrea Carbone | Development |
| Gabriele Buca | Development |
| Francesco Botton | Development |

---

## Features

| Area | Feature | Status |
|------|---------|--------|
| **Game** | Full rules (3 Eras, all cards, all effects) | ✅ |
| | Turn-based state machine (6 states) | ✅ |
| | Polymorphic State pattern | ✅ |
| | Sealed message hierarchy (28 message types) | ✅ |
| **Network** | Socket TCP (port 12345) | ✅ |
| | Java RMI (port 1099) | ✅ |
| | Real-time heartbeat / ping-pong (5s interval, 15s timeout) | ✅ |
| **Reliability** | Client auto-reconnect (retry every 3s) | ✅ |
| | Server crash recovery (atomic JSON persistence) | ✅ |
| | Suspended game → default victory after 60s | ✅ |
| | `synchronized` critical sections (3 total) | ✅ |
| **UI** | JavaFX GUI (board, cards, totems, events, music) | ✅ |
| | CLI (full terminal interface) | ✅ |
| **Data** | MySQL leaderboard (graceful fallback if absent) | ✅ |

---

## Architecture

The system follows a client-server model. A single `Server` process manages one game instance; multiple `Client` processes connect over TCP or RMI.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT                                      │
│  ┌─────────────┐   ┌──────────────┐   ┌────────────────────────┐    │
│  │  GUI/CLI    │──▶│ ClientModel  │──▶│ ServerConnection       │    │
│  │  (View)     │   │  (State)     │   │  (TCP or RMI)          │    │
│  └──────┬──────┘   └──────────────┘   └───────────┬────────────┘    │
│         │                                         │                 │
│         ▼                                         ▼                 │
│  ┌──────────────┐                        ┌───────────────────┐      │
│  │ClientState   │                        │ ServerHandler     │      │
│  │Registry      │                        │ RMIListener       │      │
│  └──────────────┘                        └───────────────────┘      │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ TCP / RMI
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         SERVER                                      │
│  ┌──────────────────┐    ┌──────────────┐    ┌───────────────────┐  │
│  │ ServerSocket     │───▶│ ClientHandler│───▶│   VirtualView     │  │
│  │ RMI Registry     │    │ (per client) │    │   (Observer)      │  │
│  └──────────────────┘    └──────────────┘    └─────────┬─────────┘  │
│                                                        │            │
│  ┌─────────────────────────────────────────────────────┘            │
│  │  ┌──────────────────┐    ┌────────────────────────────────┐      │
│  │  │   Controller     │───▶│   Game (Model)                 │      │
│  │  │   (synchronized) │    │   ┌────────────────────────┐   │      │
│  │  └──────────────────┘    │   │  GameState (State P.)  │   │      │
│  │                          │   │  ├─ SetUpGameState     │   │      │
│  │  ┌──────────────────┐    │   │  ├─ StartTurnState     │   │      │
│  │  │ DatabaseManager  │    │   │  ├─ PickingState       │   │      │
│  │  │ PersistenceMgr   │    │   │  ├─ EventState         │   │      │
│  │  └──────────────────┘    │   │  └─ EndGameState       │   │      │
│  │                          │   └────────────────────────┘   │      │
│  │                          └────────────────────────────────┘      │
│  └────────────────────────────────────────────┘                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Design Patterns

| Pattern | Where | Benefit |
|---------|-------|---------|
| **State** | `GameState` → 5 concrete states | Eliminates if-else chains; invalid operations throw at compile-time |
| **Strategy / Registry** | `ClientStateHandler` + `ClientStateRegistry` | Maps server state → client UI behavior (CLI render, GUI banners, input blocking) |
| **Observer** | `VirtualView` ← `Game` (model) | Decouples game model from network broadcast; each `VirtualView` fanouts to all connected clients |
| **Command** | `Request.execute(controller, ...)` | Each message encapsulates its own server-side logic; zero dispatch boilerplate |
| **Facade** | `VirtualServer` | Single-client view facade hiding network transport details from the UI |
| **Singleton** | `Client` (hub), `Controller` | Single points of coordination for client state and server-side turn serialization |

### Transport Comparison

| Aspect | Socket TCP (port 12345) | Java RMI (port 1099) |
|--------|------------------------|----------------------|
| Serialization | Java Object Streams | Built-in RMI protocol |
| Client entry | `Socket(host, 12345)` | `LocateRegistry.getRegistry(host, 1099)` |
| Update delivery | Background thread reads `ObjectInputStream` | Server invokes client stub directly |
| Disconnection detection | `IOException` on socket read/write | `RemoteException` |
| Heartbeat mechanism | Server-side scheduled ping/pong | Server-side ping/pong + client-side watcher |

---

## Protocol

Full specification (messages, heartbeat, auto-reconnect, TCP stream layout, RMI interfaces) in
[`docs/network-protocol.pdf`](./docs/network-protocol.pdf) — LaTeX source with TikZ diagrams at
[`docs/network-protocol.tex`](./docs/network-protocol.tex).

### Client → Server (Requests)

| Message | Payload |
|---------|---------|
| `JoinGameRequest` | `nickname` |
| `SetupGameRequest` | `nickname` |
| `ChooseTotemRequest` | `nickname, color` |
| `PlaceTotemRequest` | `nickname, offerPosition` |
| `PickCardRequest` | `nickname, row, cardIndex` |
| `EndTurnRequest` | `nickname` |
| `GetLeaderboardRequest` | `numPlayers` |
| `PingRequest` | `timestamp` |

### Server → Client (Updates)

| Message | Payload |
|---------|---------|
| `PlayerJoinedUpdate` | `nickname` |
| `TotemColorUpdate` | `nickname, color` |
| `StateChangedUpdate` | `full state snapshot` |
| `TurnChangedUpdate` | `newCurrentPlayer` |
| `EndGameUpdate` | `winners, scores, numPlayers` |
| `LeaderboardUpdate` | `playerPosition, entries[], numPlayers` |
| `ErrorMessage` | `message` |
| `PlayerDisconnectedUpdate` | `nickname` |
| `PlayerReconnectedUpdate` | `nickname` |
| `GameSuspendedUpdate` | — |
| `GameResumedUpdate` | — |

---

## Game Flow

### State Machine

```
Lobby ──▶ SetUpGameState ──▶ [StartTurnState ⇄ PickingState]ⁿ ──▶ EndGameState
                                        │
                                        ▼
                                   EventState
                                (auto-resolved)
```

### Round Structure

The game spans **3 Eras**. Each Era consists of:

1. **StartTurn** — Players place totems on offering tiles (in turn order). The active player chooses an offering position.
2. **Picking** — Active player picks a card from the upper or lower row of the board.
3. **Event** — Event cards resolve automatically (no player input needed).
4. Repeat until all 3 Eras are complete → **EndGame** final scoring.

---

## Project Structure

```
src/main/java/it/polimi/ingsw/am54/
├── MainLauncher.java                 # Entry point (CLI/GUI selector)
├── controller/Controller.java        # Server-side coordinator
├── database/                          # MySQL leaderboard + JSON persistence
├── gui/
│   ├── BoardController.java          # Main game board (JavaFX)
│   ├── LoginController.java
│   ├── LobbyController.java
│   ├── managers/                     # Market, Totem, Dashboard, Audio
│   └── states/                       # ★ Polymorphic client handlers
│       ├── ClientStateHandler.java   #   Interface
│       ├── ClientStateRegistry.java  #   Registry (Strategy)
│       └── CLIAbstract.java          #   CLI rendering contract
├── model/
│   ├── Game.java / Board.java / Player.java
│   ├── Card.java + subclasses
│   ├── GameState.java                # State pattern base
│   ├── states/                       # Concrete FSM states
│   │   ├── SetUpGameState.java
│   │   ├── StartTurnState.java
│   │   ├── PickingState.java
│   │   ├── EventState.java
│   │   └── EndGameState.java
│   └── effects/                      # Card effect system
└── network/
    ├── server/                       # Server, handlers, VirtualView
    ├── client/                       # Client, ClientModel, ServerHandler
    ├── rmi/                          # RMI interfaces & stubs
    └── messages/                     # Request / Update sealed hierarchy

src/test/java/it/polimi/ingsw/am54/
├── model/                            # 14 test classes
├── controller/                       # 1 test class
├── network/                          # 5 test classes
└── integration/                      # 5 test classes (TCP + RMI)
```

---

## Javadoc

```bash
./mvnw javadoc:javadoc     # → target/site/apidocs/index.html
```

---

## Testing

```bash
# Run all tests
./mvnw test

# Filter by package
./mvnw test -Dtest="model.*"
./mvnw test -Dtest="network.*"
./mvnw test -Dtest="integration.*"
```

28 test classes across 4 categories:

| Package | Count | Scope |
|---------|-------|-------|
| `model/` | 14 | Game logic, state machine, card effects, board, validation, disconnection |
| `controller/` | 1 | Server-side turn coordination |
| `network/` | 5 | Message serialization, client state, disconnection handling |
| `integration/` | 5 | End-to-end Socket TCP + RMI flows |

---

## Documentation

| Document | Description |
|-----------|-------------|
| **Network protocol** | |
| [`docs/network-protocol.pdf`](./docs/network-protocol.pdf) | TCP/RMI protocol specification with TikZ diagrams |
| [`docs/network-protocol.tex`](./docs/network-protocol.tex) | LaTeX source |
| **UML diagrams** | |
| [`docs/model.png`](./docs/model.png) | Model class diagram |
| [`docs/network.png`](./docs/network.png) | Network package class diagram |
| [`docs/uml_full.png`](./docs/uml_full.png) | Full application UML diagram |
| [`docs/UML/model.puml`](./docs/UML/model.puml) | Model PlantUML source |
| [`docs/UML/network.puml`](./docs/UML/network.puml) | Network PlantUML source |
| [`docs/UML/uml_full.puml`](./docs/UML/uml_full.puml) | Full PlantUML source |
| **API** | |
| `target/site/apidocs/index.html` | Javadoc (generate with `./mvnw javadoc:javadoc`) |

---

## Configuration

Copy `.env.example` → `.env`:

```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=codex_naturalis
```

Without `.env`, the leaderboard is gracefully disabled (MySQL not required for gameplay).

---

## Build Artifacts

```bash
# Build executable JARs
./mvnw package

# Run
java -jar target/Server.jar
java -jar target/Client.jar       # GUI mode
java -jar target/Client.jar CLI   # CLI mode
```
