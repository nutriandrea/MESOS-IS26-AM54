# IS26-AM54 - Codex Naturalis
## Software Engineering Final Project

**Grade: x/30**

---

## Introduction

The project involves the development of a distributed software version of the board game **Mesos**. The game rules are described in the [RULES.md](./RULES.md) file.

This is a multiplayer board game where 2-5 players compete to collect the most prestige points by strategically placing totems on offering tiles and collecting character/building cards.

---

## Team

| Member | Role |
|--------|------|
| Andrea Carbone | Development |
| Andrea Cacioppo | Development |
| Gabriele Buca | Development |
| Francesco Botton | Development |

**Professor**: Alessandro Margara

---

## Project Progress

| Feature                | Status |
|------------------------|--------|
| Basic rules            | ✅ Complete |
| Complete rules         | ✅ Complete |
| Socket communication   | ✅ Implemented |
| RMI communication      | 🔄 To implement |
| GUI (JavaFX)           | 🔄 To implement |
| CLI                    | 🔄 To implement |
| Multiple games support | 🔄 To implement |
| ML Bots                | 🔄 To implement |
| Persistence            | 🔄 To implement |
| Disconnection handling | 🔄 To implement |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                  │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────────┐   │
│  │    View     │───▶│ ClientNetwork│───▶│ ObjectInputStream │   │
│  └─────────────┘    └──────────────┘    └───────────────────┘   │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Socket TCP
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         SERVER                                  │
│                                                                 │
│  ┌─────────────────┐         ┌───────────────────────────────┐  │
│  │  ServerSocket   │────────▶│    SocketClientHandler        │  │
│  │  (port 1234)    │         │    (Thread per client)        │  │
│  └─────────────────┘         └───────────────┬───────────────┘  │
│                                              │                  │
│                                              ▼                  │
│                              ┌───────────────────────────────┐  │
│                              │       Controller              │  │
│                              │  (synchronized access)        │  │
│                              └───────────────┬───────────────┘  │
│                                              │                  │
│                                              ▼                  │
│  ┌──────────────────┐           ┌───────────────────────────┐   │
│  │  VirtualView     │◀───────── ┤       Game (Model)        │   │
│  │  (Observer)      │           │    (State Machine)        │   │
│  └──────────────────┘           └───────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Components

| Layer | Classes |
|-------|---------|
| **Model** | `Game`, `Player`, `Board`, `Card`, `Deck`, `OfferingTile`, States |
| **Controller** | `Controller` |
| **Network** | `Server`, `SocketClientHandler`, `VirtualView`, `ClientHandler` |
| **Messages** | `*Request`, `*Update`, `ErrorMessage` |

---

## Technology Stack

| Technology | Usage |
|------------|-------|
| **Java 25** | Programming language |
| **Maven** | Build and dependency management |
| **JavaFX** | GUI framework |
| **Jackson** | JSON parsing for card setup |
| **JUnit 5** | Unit testing |
| **IntelliJ IDEA** | Development IDE |

---

## Setup

### Prerequisites

- Java 25 or higher
- Maven 3.9+

### Build

```bash
mvn clean compile
```

### Run Tests

```bash
mvn test
```

### Run Server

```bash
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server"
```

Porta default: `12345`. Per usare altra porta:
```bash
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server" -Dexec.args="8080"
```

### Run Client

```bash
java -jar target/Client.jar
```

---

## Network Protocol

### Request Messages (Client → Server)

| Message | Parameters |
|---------|------------|
| `JoinGameRequest` | `nickname` |
| `ChooseTotemRequest` | `nickname, color` |
| `SetupGameRequest` | `nickname` |
| `PlaceTotemRequest` | `nickname, offerPosition` |
| `PickCardRequest` | `nickname, row, cardIndex` |

### Update Messages (Server → Client)

| Message | Parameters |
|---------|------------|
| `PlayerJoinedUpdate` | `nickname` |
| `TotemColorUpdate` | `nickname, color` |
| `TotemPlacedUpdate` | `nickname, offerPosition` |
| `CardPickedUpdate` | `nickname, row, cardIndex` |
| `TurnChangedUpdate` | `newCurrentPlayer` |
| `StateChangedUpdate` | `newState` |
| `EndGameUpdate` | `winners` |
| `ErrorMessage` | `message` |

---

## Project Structure

```
src/
├── main/
│   └── java/it/polimi/ingsw/am54/
│       ├── controller/
│       │   └── Controller.java
│       ├── model/
│       │   ├── Game.java
│       │   ├── Player.java
│       │   ├── Board.java
│       │   ├── states/
│       │   ├── effects/
│       │   └── cards/
│       ├── network/
│       │   ├── server/
│       │   │   ├── Server.java
│       │   │   ├── SocketClientHandler.java
│       │   │   ├── VirtualView.java
│       │   │   └── ClientHandler.java
│       │   └── messages/
│       │       ├── *Request.java
│       │       ├── *Update.java
│       │       └── ErrorMessage.java
│       └── Main.java
└── test/
    └── java/...
```

---

## Game Flow

```
1. Lobby Phase
   └── Players join with nickname

2. Setup Phase
   └── Players choose totem color (RED, YELLOW, WHITE, BLACK)
   └── One player calls setupGame()

3. Turn Loop (3 Eras)
   ├── Start Turn State
   │   └── Place totem on offering tile
   ├── Picking State
   │   └── Pick cards from upper/lower rows
   ├── Action Phase
   │   └── Play cards, collect resources
   ├── Event Phase
   │   └── Resolve event cards
   └── End Turn
       └── Next player

4. End Game
   └── Calculate scores
   └── Declare winner(s)
```

---

## License

Mesos is property of **Cranio Creations** and all of the copyrighted graphical assets used in this project were supplied by **Politecnico di Milano** in collaboration with their rights' holders.

---

## Documentation

- [Game Rules](./RULES.md)
- [Network Architecture](./NETWORK_README.md)
- [Test Coverage Summary](./TEST_COVERAGE_SUMMARY.md)
