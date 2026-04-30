# Prova Finale di Ingegneria del Software - Anno Accademico 2025-2026

## Specifica Tecnica del Progetto "Mesos - AM54"

---

## 1. Introduzione

Il progetto consiste nello sviluppo di una versione software del gioco da tavolo **mesos**. L'applicazione è un sistema distribuito client-server che permette a 2-5 giocatori di partecipare a partite multiplayer in rete.

### Obiettivi

- Implementazione completa delle regole del gioco
- Comunicazione via Socket TCP
- Comunicazione via RMI
- Sistema di heartbeat per rilevamento disconnessioni
- Gestione riconnessioni con sincronizzazione stato
- Supporto multi-partita (max 5 partite simultanee)
- Pattern MVC

---

## 2. Architettura del Sistema

### 2.1 Schema Alto Livello

```
┌─────────────────────────────────────────────────────────────────┐
│                          SERVER                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │   ServerSocket   │ SocketClientHandler (TCP)            │   │
│   │   RMIServerImpl  │ RMIClientHandler (RMI)                │   │
│   │   (port 12345)  │   (Thread per client)                   │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                  Server                                  │   │
│   │   (singola partita: Game + Controller)                  │   │
│   └─────────────────────────────────────────────────────────┘   │
│                         │                                       │
│                         ▼                                       │
│   ┌───────────────────┐              ┌─────────────────────┐   │
│   │      Controller   │──────────────│  Game (Model)        │   │
│   │ (synchronized access)  │         │  (State Machine)    │   │
│   └───────────────────┘              └─────────────────────┘   │
│                                                                 │
│   ┌───────────────┐          ┌────────────────────────────────┐   │
│   │ VirtualView  │─────────▶│      Game (Model)              │   │
│   │ (Observer)  │          │     (Observer pattern)          │   │
│   └───────────────┘          └────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Diagramma Package

```
it.polimi.ingsw.am54/
├── Main.java                    # Entry point test (4 giocatori)
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
│   └── Controller.java          # Command handler (1 per partita)
├── network/
│   ├── server/
│   │   ├── Server.java          # Socket + RMI server
│   │   ├── SocketClientHandler.java
│   │   ├── RMIServerImpl.java   # RMI implementation
│   │   ├── RMIClientHandler.java
│   │   ├── VirtualView.java
│   │   └── ClientConnection.java
│   ├── client/
│   │   ├── Client.java          # Client base
│   │   ├── ClientModel.java     # Stato locale client
│   │   ├── ServerHandler.java   # Thread ascolto TCP
│   │   ├── SocketServerConnection.java
│   │   └── RMIServerConnection.java
│   ├── rmi/
│   │   ├── RMIGameServer.java
│   │   ├── RMIClientInterface.java
│   │   └── RMIClientHandlerInterface.java
│   └── messages/
│       ├── Request.java         # Interfaccia base
│       ├── Update.java          # Interfaccia base
│       ├── JoinGameRequest.java    # nickname del giocatore
│       ├── SetupGameRequest.java
│       ├── ChooseTotemRequest.java
│       ├── PlaceTotemRequest.java
│       ├── PickCardRequest.java
│       ├── PingRequest.java
│       ├── PongResponse.java
│       ├── PlayerJoinedUpdate.java
│       ├── PlayerDisconnectedUpdate.java
│       ├── PlayerReconnectedUpdate.java
│       ├── TotemColorUpdate.java
│       ├── TotemPlacedUpdate.java
│       ├── CardPickedUpdate.java
│       ├── TurnChangedUpdate.java
│       ├── StateChangedUpdate.java
│       ├── EndGameUpdate.java
│       └── ErrorMessageUpdate.java
```

---

## 3. Protocollo di Comunicazione

### 3.1 Connessione Socket

```
CLIENT                                    SERVER

new Socket(ip, port)
      │
      ▼
ObjectOutputStream.flush()
      │                    TCP                   ▼
      ├───────────────────────▶  ServerSocket.accept()
      │                        new SocketClientHandler()
      │                        CONNECTION_ESTABLISHED
      │◀───────────────────── (ACK)
      ▼
ObjectInputStream + ServerHandler thread starts
```

### 3.2 Richieste (Client → Server)

| Messaggio | Parametri | Descrizione |
|:----------|:----------|:-------------|
| `JoinGameRequest` | nickname | Primo contatto / Riconnessione |
| `ChooseTotemRequest` | color (RED/YELLOW/WHITE/BLACK) | Scelta colore |
| `SetupGameRequest` | - | Avvia partita |
| `PlaceTotemRequest` | offerPosition (1-6) | Piazza totem |
| `PickCardRequest` | row (UPPER/LOWER), cardIndex | Pesca carta |
| `PongResponse` | timestamp | Risposta automatica a ping |

### 3.3 Aggiornamenti (Server → Client)

| Messaggio | Parametri | Descrizione |
|:----------|:----------|:-------------|
| `PlayerJoinedUpdate` | nickname | Nuovo giocatore |
| `PlayerDisconnectedUpdate` | nickname | Giocatore disconnesso |
| `PlayerReconnectedUpdate` | nickname | Giocatore riconnesso |
| `TotemColorUpdate` | nickname, color | Colore scelto |
| `TotemPlacedUpdate` | nickname, position | Totem piazzato |
| `CardPickedUpdate` | nickname, row, index | Carta pescata |
| `TurnChangedUpdate` | newCurrentPlayer | Cambio turno |
| `StateChangedUpdate` | stateName | Cambio stato |
| `EndGameUpdate` | winners[] | Fine partita |
| `ErrorMessageUpdate` | message | Errore |
| `PingRequest` | timestamp | Heartbeat (ogni 5s) |
| `StateChangedUpdate` | stateName | Sync stato completo (reconnect) |

### 3.4 Heartbeat

```
SERVER                                    CLIENT
PingRequest(timestamp) ─────────────────────────▶
                     ◀────────────────── PongResponse(timestamp)

Se nessun Pong in 15 secondi:
         → disconnect() → markDisconnected()
```

---

## 4. Pattern Utilizzati

| Pattern | Utilizzo |
|:--------|:--------|
| **MVC** | Model (Game), View (VirtualView/GUI), Controller |
| **Observer** | VirtualView osserva Game |
| **Command** | Controller.executeCommand() |
| **State Machine** | Game delega agli State |
| **Thread per Client** | SocketClientHandler in thread separato |
| **Virtual View** | Traduce eventi Model in messaggi di rete |

---

## 5. Funzionalità Avanzate Implementate

### 5.1 Resilienza alle Disconnessioni

- **Heartbeat**: PingRequest ogni 5 secondi, PongResponse automatico dal client
- **Timeout**: 15 secondi senza risposta -> markDisconnected()
- **Giocatori disconnessi**: Mantenuti nel gioco, NON saltati nei turni
- **Riconnessione**: JoinGameRequest con nickname esistente -> handleReconnect()
- **Sync stato**: StateChangedUpdate completo inviato al reconnect
- **Broadcast**: PlayerDisconnectedUpdate e PlayerReconnectedUpdate a tutti

### 5.2 Socket + RMI

- **Socket TCP**: SocketClientHandler per ogni connessione (porta 12345)
- **RMI**: RMIServerImpl + RMIClientHandler (porta 1099)
- Entrambi supportano ping-pong e riconnessione

### 5.3 Architettura Server

- **Server**: tiene una singola istanza di Game + Controller
- **JoinGame**: il primo player che si connette avvia la partita
- **Riconnessione**: StateChangedUpdate per sincronizzare stato completo

### 5.4 Persistenza (Crash Recovery)

Il server salva periodicamente lo stato della partita su disco per permettere il recovery dopo un crash.

**File di salvataggio:** `game_state.json`

**Cosa viene salvato:**
- **Game**: currentEra, roundCount, currentStateName, currentPlayerNickname
- **Players**: nickname, totemColor, food, aura, magicPoints, extraPick, hasPlacedTotem, eventCostFood, eventCostAura, buildingDiscount, foodDiscount, myTribe (carte in mano), myBuildings (edifici in mano)
- **Board**: characterUpperRow, characterLowerRow, eventUpperRow, eventLowerRow, buildingUpperRow, buildingLowerRow, offeringTrack (con bonus), turnOrderTile (ordine turni)
- **Decks**: deckTribuCards[], deckBuildingsCards[] (tutte le carte rimaste)

**Trigger di salvataggio:**
- Salvataggio automatico ogni 30 secondi (ScheduledExecutorService)
- Dopo ogni azione di gioco: setupGame, chooseTotemColor, placeTotem, pickCard
- Su shutdown del server (ShutdownHook JVM)

**Recovery:**
1. All'avvio, Server carica game_state.json se esiste
2. Ripristina i player con joinGame() + chooseTotemColor()
3. Chiama setupGame() per inizializzare board
4. Ripristina era, round e stato del gioco
5. Giocatori che si riconnettono con stesso nickname ricevono StateChangedUpdate per sync completo

**Classe:** `PersistenceManager.java` con nested classes: GameFullSnapshot, PlayerSnapshot, BoardSnapshot, CardInfo, OfferingTileInfo, TurnOrderSlotInfo

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

### 7.1 Server (Socket TCP + RMI)

```bash
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server"
```

Il server avvia:
- Socket TCP sulla porta 12345
- RMI sulla porta 1099
- Una singola partita (Game + Controller)

### 7.2 Client (programmatico)

Il client deve creare un'istanza di Client e connettersi tramite:
- `SocketServerConnection` (TCP)
- `RMIServerConnection` (RMI)

Vedi `it.polimi.ingsw.am54.network.client.Client` come punto di partenza.

---

## 8. Tabella di Valutazione

| Feature | Stato |
|:--------|:------|
| Regole complete gioco | ✅ |
| Socket TCP | ✅ |
| RMI | ✅ |
| Disconnection handling (ping/pong) | ✅ |
| Riconnessione (state sync) | ✅ |
| Persistence (crash recovery) | ✅ |
| GUI (JavaFX) | ❌ |
| TUI (CLI) | ❌ | 

---

## 9. Riferimenti

- [README.md](../README.md) - Documentazione generale
- [NETWORK_README.md](../NETWORK_README.md) - Architettura di rete dettagliata
- [RULES.md](../RULES.md) - Regole del gioco
- [CHECKLIST.md](./CHECKLIST.md) - Checklist di verifica