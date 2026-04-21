# Prova Finale di Ingegneria del Software - Anno Accademico 2025-2026

## Specifica Tecnica del Progetto "Mesos - AM54"

---

## 1. Introduzione

Il progetto consiste nello sviluppo di una versione software del gioco da tavolo **mesos**. L'applicazione è un sistema distribuito client-server che permette a 2-5 giocatori di partecipare a partite multiplayer in rete.

### Obiettivi Raggiunti

- Implementazione completa delle regole del gioco
- Comunicazione via Socket TCP
- Comunicazione via RMI
- Interfaccia Grafica (GUI) JavaFX
- Interfaccia Testuale (TUI) a riga di comando
- Sistema di heartbeat per rilevamento disconnessioni
- Gestione riconnessioni
- Pattern MVC

---

## 2. Architettura del Sistema

### 2.1 Schema Alto Livello

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                  │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────────┐   │
│  │    GUI      │───▶│    Client    │───▶│ ObjectOutputStream│   │
│  │  (JavaFX)   │    └──────┬───────┘    └───────────────────┘   │
│  └─────────────┘    ┌──────▼───────┐                            │
│               TUI  │ServerHandler │◀── ObjectInputStream      │
│  (ClientMain)       └─────────────┘                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Socket TCP / RMI
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         SERVER                                  │
│  ┌─────────────────┐         ┌───────────────────────────────┐  │
│  │  ServerSocket   │────────▶│    SocketClientHandler        │  │
│  │  (port 1234)    │         │    (Thread per client)          │  │
│  └─────────────────┘         └───────────────┬───────────────┘  │
│                                              │                  │
│                                              ▼                  │
│                              ┌───────────────────────────────   │
│                              │       Controller              │  │
│                              │  (synchronized access)        │  │
│                              └───────────────┬───────────────┘  │
│                                              │                  │
│                                              ▼                  │
│  ┌──────────────────┐           ┌───────────────────────────┐   │
│  │  VirtualView     │◀─────────┤       Game (Model)         │   │
│  │  (Observer)      │          │    (State Machine)         │   │
│  └────────┬─────────┘           └───────────────────────────┘   │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                            │
│  │ ClientHandler  │ ◀── send updates to specific client       │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Diagramma Package

```
it.polimi.ingsw.am54/
├── Main.java                    # Entry point per test
├── model/
│   ├── Game.java               # Core game engine
│   ├── Player.java              # Player entity
│   ├── Board.java               # Game board
│   ├── Card.java, BuildingCard.java, CharacterCard.java, EventCard.java
│   ├── Deck.java, DeckFactory.java
│   ├── OfferingTile.java
│   ├── TotemColors.java
│   ├── states/                  # Game state machine
│   │   ├── SetUpGameState.java
│   │   ├── StartTurnState.java
│   │   ├── PickingState.java
│   │   ├── EventState.java
│   │   └── EndGameState.java
│   └── effects/                 # Card effects
├── controller/
│   └── Controller.java          # Command handler
├── network/
│   ├── server/
│   │   ├── Server.java          # Socket server
│   │   ├── SocketClientHandler.java
│   │   ├── VirtualView.java
│   │   └── ClientHandler.java
│   ├── client/
│   │   ├── Client.java
│   │   ├── ClientMain.java      # TUI entry
│   │   └── ServerHandler.java
│   ├── rmi/
│   │   ├── RMIServer.java
│   │   ├── RMIServerInterface.java
│   │   ├── RMIServerImplementation.java
│   │   ├── RMIClientConnector.java
│   │   └── RMIClientHandler.java
│   └── messages/
│       ├── ClientMessage.java   # Base interface
│       ├── Request.java          # Client → Server
│       ├── Update.java           # Server → Client
│       └── *Request.java, *Update.java
└── view/
    ├── GameApplication.java      # GUI entry
    ├── GameController.java
    └── game.fxml
```

---

## 3. Protocollo di Comunicazione

### 3.1 Connessione Socket

```
CLIENT                                    SERVER
─────                                     ──────
new Socket(ip, port)
     │
     ▼
ObjectOutputStream.flush()
     │──── TCP ────▶  ServerSocket.accept()
     │               new SocketClientHandler()
     │
     │               CONNECTION_ESTABLISHED
     ◀───────────────┘ (ACK)
     │
     ▼
ObjectInputStream + ServerHandler thread starts
```

### 3.2 Richieste (Client → Server)

| Messaggio | Parametri | Descrizione |
|----------|-----------|-------------|
| `JoinGameRequest` | nickname | Primo contatto |
| `ChooseTotemRequest` | color (RED/YELLOW/WHITE/BLACK) | Scelta colore |
| `SetupGameRequest` | - | Avvia partita |
| `PlaceTotemRequest` | offerPosition (1-6) | Piazza totem |
| `PickCardRequest` | row (UPPER/LOWER), cardIndex | Pesca carta |
| `ReconnectRequest` | nickname | Riconnessione |
| `PongResponse` | timestamp | Risposta a ping |

### 3.3 Aggiornamenti (Server → Client)

| Messaggio | Parametri | Descrizione |
|----------|-----------|-------------|
| `PlayerJoinedUpdate` | nickname | Nuovo giocatore |
| `PlayerReconnectedUpdate` | nickname | Giocatore riconnesso |
| `TotemColorUpdate` | nickname, color | Colore scelto |
| `TotemPlacedUpdate` | nickname, position | Totem piazzato |
| `CardPickedUpdate` | nickname, row, index | Carta pescata |
| `TurnChangedUpdate` | newCurrentPlayer | Cambio turno |
| `StateChangedUpdate` | stateName | Cambio stato |
| `EndGameUpdate` | winners[] | Fine partita |
| `ErrorMessageUpdate` | message | Errore |
| `PingRequest` | timestamp | Heartbeat |

### 3.4 Heartbeat

```
SERVER                                    CLIENT
──────                                    ──────
PingRequest(timestamp) ──────────────────▶
                    ◀─────────────────── PongResponse(timestamp)

Se nessun Pong in 15 secondi:
        → disconnect() → markDisconnected()
```

---

## 4. Pattern Utilizzati

| Pattern | Utilizzo |
|---------|----------|
| **MVC** | Model (Game), View (VirtualView/GUI), Controller |
| **Observer** | VirtualView osserva Game |
| **Command** | Controller.executeCommand() |
| **State Machine** | Game delega agli State |
| **Thread per Client** | SocketClientHandler in thread separato |
| **Virtual View** | Traduce eventi Model in messaggi di rete |

---

## 5. Funzionalità Avanzate Implementate

### 5.1 Resilienza alle Disconnessioni

- Sistema di heartbeat Ping/Pong ogni 5 secondi
- Timeout 15 secondi per rilevamento disconnessioni
- Giocatore mantenuto nel gioco durante disconnessione
- Possibilità di riconnessione

### 5.2 Socket + RMI

- Supporto per entrambe le tecnologie
- Client può scegliere all'avvio

### 5.3 GUI + TUI

- Interfaccia Grafica JavaFX
- Interfaccia Testuale ClientMain

---

## 6. Test

### 6.1 Test Esistenti

- Test per il Model layer
- Test per Network layer (nuovi)
- Test per Player (nuovi)

### 6.2 Nuovi Test Aggiunti

- `network/RequestTest.java` - Test classi Request
- `network/UpdateTest.java` - Test classi Update
- `model/PlayerTest.java` - Test Player

---

## 7. Avvio del Sistema

### 7.1 Server Socket

```bash
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server"
```

### 7.2 Server RMI

```bash
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.rmi.RMIServer"
```

### 7.3 Client TUI

```bash
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.client.ClientMain"
```

### 7.4 Client GUI

```bash
./mvnw javafx:run
```

---

## 8. Tabella di Valutazione

| Requisiti Soddisfatti | Voto Massimo |
|---------------------|--------------|
| Regole Complete + TUI + Socket | 20 |
| Regole Complete + TUI + Socket + RMI | 22 |
| Regole Complete + TUI + GUI + Socket + RMI | 27 |
| Regole Complete + TUI + GUI + Socket + RMI + 1 FA | 30 |

**Stato attuale**: Regole Complete + TUI + GUI + Socket + RMI + Heartbeat/Fault Tolerance = **27** (può raggiungere **30** con funzionalità avanzate aggiuntive come persistenza o partite multiple)

---

## 9. Riferimenti

- [README.md](../README.md) - Documentazione generale
- [NETWORK_README.md](../NETWORK_README.md) - Architettura di rete dettagliata
- [RULES.md](../RULES.md) - Regole del gioco
- [CHECKLIST.md](./CHECKLIST.md) - Checklist di verifica