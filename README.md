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
| Andrea Cacioppo | Development |
| Andrea Carbone | Development |
| Gabriele Buca | Development |
| Francesco Botton | Development |

**Professor**: Alessandro Margara
**Honorable Mention**: Carlo Arnone


---

## Project Progress

| Feature                | Status |
|------------------------|--------|
| Basic rules            | ✅ Complete |
| Complete rules         | ✅ Complete |
| Socket communication   | ✅ Implemented |
| RMI communication      | ✅ Implemented |
| GUI (JavaFX)           | ✅ Implemented |
| CLI                    | ✅ Implemented |
| Leaderboard (mySQL)    | ✅ Implemented |
| Client handling        | ✅ Implemented |
| Server handling        | ✅ Implemented |
| ML Bots                | 🔄 To implement |


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

### Thread Safety Design

Il server usa un thread per ogni client connesso: più thread possono chiamare metodi del Controller contemporaneamente.

La sincronizzazione è **minimale** perché il gioco è turn-based — il Model (`Game`) valida il turno e rifiuta mosse di chi non è di turno. Solo 3 punti richiedono `synchronized`:

| Metodo | Lock | Perché |
|---|---|---|
| `handleJoinGame` | `synchronized (game.getPlayers())` | Impedisce a due client di fare join nello stesso istante (race su `Game.observers`) |
| `handleReconnect` | `synchronized (game.getPlayers())` | Protegge `addObserver`/`removeObserver` durante la riconnessione |
| `removeClient` | `synchronized (game.getPlayers())` | Protegge `removeObserver` alla chiusura del socket |

Le strutture dati di supporto (`virtualViews`, `disconnectedPlayers`) usano **`ConcurrentHashMap`** invece di `HashMap`/`HashSet`, eliminando ogni bisogno di sincronizzazione manuale su di esse.

Tutte le azioni di gioco (`pickCard`, `endTurn`, ecc.) passano da `executeCommand()` **senza sincronizzazione**: il turn-check nel Model è sufficiente a prevenire conflitti.

---

## Technology Stack

| Technology         | Usage                                      |
|--------------------|--------------------------------------------|
| **Java 25**        | Programming language                       |
| **Maven**          | Build and dependency management            |
| **JavaFX**         | GUI framework                              |
| **Jackson**        | JSON parsing for card setup                |
| **MySql Database** | MySQL database for leaderboard persistence |
| **JUnit 5**        | Unit testing                               |
| **IntelliJ IDEA**  | Development IDE                            |

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


Per usare un nome file personalizzato:
```bash
./mvnw compile exec:java -Dexec.mainClass="..." -Ddb.file=codex_naturalis
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
| `GetLeaderboardRequest` | `numPlayers` |

### Update Messages (Server → Client)

| Message | Parameters |
|---------|------------|
| `PlayerJoinedUpdate` | `nickname` |
| `TotemColorUpdate` | `nickname, color` |
| `TotemPlacedUpdate` | `nickname, offerPosition` |
| `CardPickedUpdate` | `nickname, row, cardIndex, food, offering` |
| `TurnChangedUpdate` | `newCurrentPlayer` |
| `StateChangedUpdate` | `full state snapshot` |
| `EndGameUpdate` | `winners, allScores, numPlayers` |
| `LeaderboardUpdate` | `playerPosition, entries[], numPlayers` |
| `ErrorMessage` | `message` |

---

## Project Structure

```
src/
├── main/
│   └── java/it/polimi/ingsw/am54/
│       ├── controller/
│       │   └── Controller.java
│       ├── database/
│       │   └── DatabaseManager.java    ← JDBC persistence layer
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
│       │   │   ├── RMIClientHandler.java
│       │   │   ├── VirtualView.java
│       │   │   └── ClientConnection.java
│       │   ├── client/
│       │   │   ├── Client.java
│       │   │   ├── ClientModel.java
│       │   │   ├── ServerConnection.java
│       │   │   ├── VirtualServer.java
│       │   │   ├── ServerHandler.java
│       │   │   ├── SocketServerConnection.java
│       │   │   ├── RMIServerConnection.java
│       │   │   └── CLIApp.java
│       │   └── messages/
│       │       ├── *Request.java
│       │       ├── *Update.java
│       │       ├── LeaderboardEntry.java
│       │       └── LeaderboardUpdate.java
│       └── gui/
│           └── BoardController.java
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
   └── Send LeaderboardUpdate to all clients
```

---

## Leaderboard (Classifica)

Alla fine di ogni partita, il server salva su un database **MySQL** i risultati di tutti i giocatori:
- nickname, punteggio finale, data, numero di giocatori

Subito dopo il calcolo dei vincitori, ogni client riceve un `LeaderboardUpdate` contenente:
- **posizione del giocatore** nella classifica per il numero di giocatori della partita appena conclusa
- **classifica completa** (tutti i risultati ordinati per punteggio decrescente)

### Visualizzazione

- **CLI**: la classifica viene stampata subito dopo i vincitori. Si può richiedere manualmente digitando `CLASSIFICA N` (es. `CLASSIFICA 4` per vedere la classifica delle partite da 4 giocatori).
- **GUI**: la classifica appare in un pannello laterale (riquadro blu "CLASSIFICA") dopo la fine della partita.

### Database

Il database è **MySQL**:
- I dati persistono tra riavvii del server

### Schema

```sql
CREATE TABLE IF NOT EXISTS match_results (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nickname    VARCHAR(50)  NOT NULL,
    score       INTEGER      NOT NULL,
    match_date  TIMESTAMP    NOT NULL DEFAULT NOW(),
    num_players INTEGER      NOT NULL
);
```

Le query di classifica usano `RANK() OVER (ORDER BY score DESC)` per calcolare la posizione.

---

## License

Mesos is property of **Cranio Creations** and all of the copyrighted graphical assets used in this project were supplied by **Politecnico di Milano** in collaboration with their rights' holders.

---

## Documentation

- [Game Rules](./RULES.md)
- [Network Architecture](./NETWORK_README.md)
- [Test Coverage Summary](./TEST_COVERAGE_SUMMARY.md)
