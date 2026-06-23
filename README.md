# IS26-AM54 — MESOS

[![Java](https://img.shields.io/badge/Java-25-%23ED8B00)](https://openjdk.org/projects/jdk/25/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36)](https://maven.apache.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-23-007396)](https://openjfx.io/)

Distributed multiplayer implementation of the board game **Mesos** (Cranio Creations). 2–5 players, real-time network, Socket/RMI, dual interface (GUI + CLI).

**Software Engineering Final Project** — Politecnico di Milano, AA 2025–2026  
**Professor**: Alessandro Margara

---

## Quick Start

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
| Leaderboard (PostgreSQL database) | ✅ |
| Polymorphic State pattern (refactored) | ✅ |
| Heartbeat / ping-pong disconnection detection | ✅ |
| AI / ML bots | 🔄 |

---

## Architecture

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

The full specification for messages, heartbeat, auto-reconnect, TCP stream layout, and RMI interfaces is in
[`docs/network-protocol.pdf`](./docs/network-protocol.pdf) (LaTeX source:
[`docs/network-protocol.tex`](./docs/network-protocol.tex) with TikZ diagrams).

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
├── database/                         # PostgreSQL leaderboard + JSON persistence
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

## Javadoc

```bash
./mvnw javadoc:javadoc
```

Generates documentation in `target/reports/apidocs/`. Open `target/reports/apidocs/index.html` in your browser.

## Documentation

| Document | Description |
|-----------|-------------|
| **Network protocol** | |
| [`docs/network-protocol.pdf`](./docs/network-protocol.pdf) | TCP/RMI protocol: messages, heartbeat, streams, RMI, auto-reconnect |
| [`docs/network-protocol.tex`](./docs/network-protocol.tex) | LaTeX source with TikZ diagrams |
| **UML diagrams** | |
| [`docs/model.png`](./docs/model.png) | Model class diagram |
| [`docs/network.png`](./docs/network.png) | Network package class diagram |
| [`docs/uml_full.png`](./docs/uml_full.png) | Full application UML diagram |
| [`model.puml`](docs/UML/model.puml) | Model PlantUML source |
| [`network.puml`](docs/UML/network.puml) | Network PlantUML source |
| [`uml_full.puml`](docs/UML/uml_full.puml) | Full PlantUML source |
| **API docs** | |
| `target/reports/apidocs/index.html` | Javadoc (`./mvnw javadoc:javadoc`) |


---

## Configuration

Copy `.env.example` to `.env` and configure (PostgreSQL — e.g. a free Aiven service):

```env
DB_HOST=your-host.aivencloud.com
DB_PORT=5432
DB_USER=avnadmin
DB_PASSWORD=your_password
DB_NAME=defaultdb
DB_SSL_MODE=require
```

Without a `.env` file (or if the database is unreachable), the leaderboard is gracefully disabled — the rest of the game works normally.

---

## Build Artifacts

```bash
# Build the runnable JARs (in target/)
./mvnw clean package -DskipTests
```

This produces two fat JARs:
- `target/IS26-AM54-1.0-SNAPSHOT-Server.jar`
- `target/IS26-AM54-1.0-SNAPSHOT-Client.jar`

```bash
# Run the server (TCP 12345 + RMI 1099)
java -jar target/IS26-AM54-1.0-SNAPSHOT-Server.jar

# Run the client — opens a launcher to choose GUI or CLI
java -jar target/IS26-AM54-1.0-SNAPSHOT-Client.jar

# Run the client straight in CLI mode
java -jar target/IS26-AM54-1.0-SNAPSHOT-Client.jar --cli
```

> **Windows + CLI:** for correct emoji/accent rendering, set the console to UTF-8
> first with `chcp 65001`. The GUI launcher's "CLI" button does this automatically.
