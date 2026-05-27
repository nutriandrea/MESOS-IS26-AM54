# IS26-AM54 — Codex Naturalis (Mesos)

[![Java](https://img.shields.io/badge/Java-25-%23ED8B00)](https://openjdk.org/projects/jdk/25/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36)](https://maven.apache.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-23-007396)](https://openjfx.io/)

Distributed multiplayer implementation of the board game **Mesos** (Codice Naturalis, Cranio Creations). 2–5 players, real-time network, Socket/RMI, dual interface (GUI + CLI).

**Software Engineering Final Project** — Politecnico di Milano, AY 2025–2026  
**Professor**: Alessandro Margara

---

## Quick Start

```bash
# Build
mvn clean compile

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

| Feature | Status |
|---------|--------|
| Complete game rules (3 Eras, all cards) | ✅ |
| Turn-based state machine | ✅ |
| Socket TCP communication | ✅ |
| RMI communication | ✅ |
| GUI (JavaFX) — board, cards, totems, events | ✅ |
| CLI — full terminal interface | ✅ |
| Client disconnection + auto-reconnect | ✅ |
| Server crash handling + suspended game | ✅ |
| Persistence (JSON backup) for reconnect | ✅ |
| Leaderboard (MySQL database) | ✅ |
| Polymorphic State pattern (refactored) | ✅ |
| Heartbeat / ping-pong disconnection detection | ✅ |
| AI / ML bots | 🔄 |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT                                      │
│  ┌─────────────┐   ┌──────────────┐   ┌────────────────────────┐   │
│  │  GUI/CLI    │──▶│ ClientModel  │──▶│ ServerConnection       │   │
│  │  (View)     │   │  (State)     │   │  (TCP or RMI)          │   │
│  └──────┬──────┘   └──────────────┘   └───────────┬────────────┘   │
│         │                                          │                 │
│         ▼                                          ▼                 │
│  ┌──────────────┐                        ┌───────────────────┐      │
│  │ClientState   │                        │ ServerHandler     │      │
│  │Registry      │                        │ RMIListener       │      │
│  └──────────────┘                        └───────────────────┘      │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │ TCP / RMI
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         SERVER                                      │
│  ┌──────────────────┐    ┌──────────────┐    ┌───────────────────┐  │
│  │ ServerSocket     │───▶│ ClientHandler│───▶│   VirtualView     │  │
│  │ RMI Registry     │    │ (per client) │    │   (Observer)      │  │
│  └──────────────────┘    └──────────────┘    └─────────┬─────────┘  │
│                                                         │            │
│  ┌──────────────────────────────────────────────────────┘            │
│  │  ┌──────────────────┐    ┌────────────────────────────────┐       │
│  │  │   Controller     │───▶│   Game (Model)                │       │
│  │  │   (synchronized) │    │   ┌────────────────────────┐   │       │
│  │  └──────────────────┘    │   │  GameState (State P.)  │   │       │
│  │                          │   │  ├─ SetUpGameState     │   │       │
│  │  ┌──────────────────┐    │   │  ├─ StartTurnState     │   │       │
│  │  │ DatabaseManager  │    │   │  ├─ PickingState       │   │       │
│  │  │ PersistenceMgr   │    │   │  ├─ EventState         │   │       │
│  │  └──────────────────┘    │   │  └─ EndGameState       │   │       │
│  │                          │   └────────────────────────┘   │       │
│  │                          └────────────────────────────────┘       │
│  └───────────────────────────────────────────────────────────────────┘
└─────────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

**State Pattern** — The game flow is modelled as a finite state machine. Each state (`GameState` subclass) implements only the methods it allows and throws `IllegalStateException` for everything else. This eliminates if-else chains and makes it impossible to call invalid actions.

**Polymorphic Client Dispatch** — The client uses `ClientStateRegistry` to map server-side state names to `ClientStateHandler` implementations. This replaces switch/if-else chains on the client for:
- CLI rendering (`CLIAbstract` interface)
- GUI action banners and button visibility
- Input blocking (whose turn is it)
- Tile interaction permission

**Dual Transport** — Socket TCP and RMI share the same message protocol (`*Request` / `*Update` objects) and the same `VirtualView` observer interface. The transport layer is abstracted behind `ClientConnection`.

**Thread Safety** — Server uses per-client threads. Synchronization is minimal thanks to turn-based validation in the Model. Three critical sections use `synchronized(game.getPlayers())`: `handleJoinGame`, `handleReconnect`, `removeClient`.

---

## Protocol

### Client → Server (Requests)

| Message | Payload |
|---------|---------|
| `JoinGameRequest` | `nickname` |
| `ChooseTotemRequest` | `nickname, color` |
| `SetupGameRequest` | `nickname` |
| `PlaceTotemRequest` | `nickname, offerPosition` |
| `PickCardRequest` | `nickname, row, cardIndex` |
| `EndTurnRequest` | `nickname` |
| `GetLeaderboardRequest` | `numPlayers` |
| `PingRequest` | — |

### Server → Client (Updates)

| Message | Payload |
|---------|---------|
| `PlayerJoinedUpdate` | `nickname` |
| `TotemColorUpdate` | `nickname, color` |
| `StateChangedUpdate` | `full state snapshot` |
| `TurnChangedUpdate` | `newCurrentPlayer` |
| `EndGameUpdate` | `winners, allScores, numPlayers` |
| `LeaderboardUpdate` | `playerPosition, entries[], numPlayers` |
| `ErrorMessage` | `message` |
| `PlayerDisconnectedUpdate` / `PlayerReconnectedUpdate` | `nickname` |

---

## Game Flow

```
Lobby ──▶ SetUpGameState ──▶ [StartTurnState ⇄ PickingState]ⁿ ──▶ EndGameState
                                        │
                                        ▼
                                   EventState
                                (auto-resolved)
```

Each round:
1. **StartTurn** — Players place totems on offering tiles (in turn order)
2. **Picking** — Active player picks cards from upper/lower row
3. **Event** — Event cards resolve automatically (no player input)
4. Repeat until 3 Eras complete → **EndGame** scoring

---

## Project Structure

```
src/main/java/it/polimi/ingsw/am54/
├── controller/Controller.java       # Server-side coordinator
├── database/                         # MySQL leaderboard + JSON persistence
├── gui/
│   ├── BoardController.java         # Main game board (JavaFX)
│   ├── LoginController.java
│   ├── LobbyController.java
│   ├── managers/                    # Market, Totem, Dashboard, Music
│   └── states/                      # ★ Polymorphic client-side handlers
│       ├── ClientStateHandler.java  #   Interface
│       ├── ClientStateRegistry.java #   Registry (Strategy Pattern)
│       └── CLIAbstract.java         #   CLI rendering contract
├── model/
│   ├── Game.java / Board.java / Player.java
│   ├── Card.java + subclasses
│   ├── GameState.java              # State pattern base
│   ├── states/                     # Concrete states
│   │   ├── SetUpGameState.java
│   │   ├── StartTurnState.java
│   │   ├── PickingState.java
│   │   ├── EventState.java
│   │   └── EndGameState.java
│   └── effects/                    # Card effect system
└── network/
    ├── server/                      # Server, handlers, VirtualView
    ├── client/                      # Client, ClientModel, CLIApp
    └── messages/                    # Request / Update DTOs
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [`docs/architecture.md`](./docs/architecture.md) | Polymorphic state pattern, client dispatch |
| [`docs/network-architecture.md`](./docs/network-architecture.md) | TCP/RMI protocol, message flow, heartbeat |
| [`docs/advanced-features.md`](./docs/advanced-features.md) | Disconnection, reconnect, leaderboard, persistence |
| [`RULES.md`](./RULES.md) | Complete game rules |
| [`SPEC.md`](./SPEC.md) | Original Italian specification |
| [`DOCUMENTATION.md`](./DOCUMENTATION.md) | Comprehensive Italian documentation |

---

## Configuration

Copy `.env.example` to `.env` and configure:

```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=codex_naturalis
```

Without a `.env` file, the leaderboard is gracefully disabled (MySQL not required).

---

## Build Artifacts

```bash
# Build JARs
mvn package

# Run server JAR
java -jar target/Server.jar

# Run client JAR (GUI)
java -jar target/Client.jar

# Run client JAR (CLI)
java -jar target/Client.jar CLI
```
