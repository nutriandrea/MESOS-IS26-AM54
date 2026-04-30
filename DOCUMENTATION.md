# IS26-AM54 - Codex Naturalis
## Software Engineering Final Project

**Politecnico di Milano - Anno Accademico 2025-2026**

**Professor**: Alessandro Margara

---

## 1. Team

| Member | Role |
|--------|------|
| Andrea Carbone | Development |
| Andrea Cacioppo | Development |
| Gabriele Buca | Development |
| Francesco Botton | Development |

---

## 2. Introduzione

Il progetto consiste nello sviluppo di una versione software distribuita del gioco da tavolo **Codex Naturalis** (noto anche come Mesos), sviluppato da Cranio Creations.

L'applicazione è un sistema distribuito client-server che permette a 2-5 giocatori di partecipare a partite multiplayer in rete.

**Vincitore**: Chi totalizza più Punti Prestigio (PP) alla fine delle 6 Ere.

---

## 3. Technology Stack

| Technology | Usage |
|------------|-------|
| **Java 25** | Programming language |
| **Maven** | Build and dependency management |
| **JavaFX** | GUI framework (non ancora implementato) |
| **Jackson** | JSON parsing per setup carte |
| **JUnit 5** | Unit testing |

---

## 4. Regole del Gioco

### 4.1 Panoramica

Codex Naturalis è un gioco di gestione risorse dove i giocatori:
- Vestono i panni di capi tribali
- Aggiungono membri alla tribù (carte Personaggio)
- Costruiscono edifici
- Affrontano eventi
- Competono per accumulare Punti Prestigio

### 4.2 Componenti

- **94 carte Tribù**: 84 Personaggi + 10 Eventi (divisi in Era I, II, III)
- **21 carte Edificio** (6 Era I, 7 Era II, 8 Era III)
- **7 tessere Offerta** (A-G)
- **1 tessera Ordine di Turno**
- **48 segnalini Cibo**
- **75 segnalini Punti Prestigio**
- **5 Totem** (RED, YELLOW, WHITE, BLACK, +1可选)

### 4.3 Setup Iniziale

1. Formare tracciato Offerte (A→G in ordine alfabetico)
2. Posizionare tessera Ordine di Turno con lato corretto (numero giocatori)
3. Creare mazzo Tribù: in fondo Eventi Finali, poi Era III → II → I
4. Pescare `giocatori + 1` carte per fila inferiore
5. Pescare `giocatori + 4` carte per fila superiore
6. Posizionare edifici Era I nella fila superiore
7. Distribuire cibo iniziale: 1°=2, 2°-3°=3, 4°-5°=4

### 4.4 Struttura del Round

Ogni round consiste in **2 fasi**:

**Fase 1: Piazzamento Totem**
- Secondo l'ordine sulla tessera Ordine di Turno (alto→basso), ogni giocatore sposta il proprio Totem su una tessera Offerta **libera**.

**Fase 2: Risoluzione Azioni**
- Dal giocatore con Totem più a sinistra verso destra:
1. Il giocatore esegue l'azione sulla propria tessera Offerta
2. Dopo l'azione, il Totem torna sulla tessera Ordine di Turno nel primo spazio libero
3. Se lo spazio ha bonus Cibo, lo ottiene immediatamente

### 4.5 Tessere Offerta

Ogni tessera mostra:
- Numero di carte da prendere dalla **fila superiore** e/o **inferiore**
- Bonus Cibo iniziale (solo tessera A per 5 giocatori)

**Regole di pescata:**
- Non si possono mai prendere carte Evento
- Se non ci sono carte disponibili, non si ottiene nulla
- Se ci sono carte Personaggio disponibili, **devono** essere prese

### 4.6 Carte Personaggio

Prendere Personaggi è **gratuito**.

| Tipo | Effetto |
|------|---------|
| **Cacciatore** | Durante Caccia: +1 Cibo per ogni Cacciatore nella tribù + PP indicati |
| **Costruttore** | Riduce il costo degli Edifici (somma sconti, non può portare sotto 0) |
| **Raccoglitore** | Durante Sostentamento: -3 Cibo dal totale da pagare |
| **Inventore** | A fine partita: (num Inventori) × (icone invenzione diverse) PP |
| **Artista** | Durante Pitture Rupestri: guadagna/perde PP |
| **Sciamano** | Durante Rituale Sciamanico: determinano maggioranza/minoranza |

### 4.7 Carte Edificio

Per prendere un Edificio si deve **pagare il costo in Cibo** (meno eventuali sconti dei Costruttori).

Se non si ha abbastanza Cibo, non si può prendere l'Edificio.

**Effetti EndGame:**
- **AURA_PER_JOB**: PP in base al numero di Personaggi di un tipo
- **DOUBLE_CONSTRUCTOR_AURA**: Doppio PP dai Costruttori
- **SIX_DIFFERENT_AURA**: 6 PP per ogni set completo di 6 tipi diversi
- **FLAT_AURA_BONUS**: PP fissi

### 4.8 Eventi

Gli eventi vengono risolti **alla fine di ogni round** dalla fila inferiore.

**Ordine di risoluzione:**
1. Tutti gli eventi **tranne Sostentamento** (in ordine di Era)
2. **Sostentamento** per ultimo

**Tipi di Evento:**

**Caccia**
- Prendi 1 Cibo per ogni Cacciatore nella tribù
- Guadagna PP indicati per ogni Cacciatore

**Sostentamento**
- Paga 1 Cibo per ogni Personaggio nella tribù (Edifici non contano)
- Ogni Raccoglitore dà -3 Cibo sul totale
- Se non hai abbastanza Cibo, perdi PP indicati per ogni Personaggio non sfamato

**Pitture Rupestri**
- Se hai ≤threshold Artisti: perdi PP indicati
- Se hai >threshold Artisti: guadagna PP × (Artisti - threshold)

**Rituale Sciamanico**
- Chi ha **più** icone Sciamano: guadagna PP indicati
- Chi ha **meno** icone Sciamano: perde PP indicati
- In caso di parità, tutti in parità ricevono/perdono PP

### 4.9 Fine del Round

1. **Risolvi Eventi** dalla fila inferiore (Sostentamento per ultimo)
2. **Scarta** tutte le carte dalla fila inferiore (Personaggi ed Eventi, **non** Edifici)
3. **Sposta** Personaggi ed Eventi dalla fila superiore alla fila inferiore
4. **Riempi** la fila superiore (`giocatori + 4` carte)

### 4.10 Transizione Era

Quando si rivela una carta Tribù dell'Era successiva:
1. Se transizione a **Era III**: scarta eventuali Edifici dalla fila inferiore
2. Sposta gli Edifici dalla fila superiore alla fila inferiore
3. Piazza i nuovi Edifici dell'Era nella fila superiore

### 4.11 Fine della Partita

La partita termina quando il mazzo Tribù è esaurito (round 10).

**Risoluzione finale:**
1. Risolvi **TUTTI** gli eventi visibili (fila superiore + inferiore)
2. Sostentamento per ultimo

**Calcolo Punteggio Finale:**

| Componente | PP |
|------------|-----|
| PP dei **Costruttori** nella tribù | Somma dei valori indicati sulle carte |
| PP degli **Inventori** | (num Inventori) × (icone invenzione DISTINTE) |
| PP degli **Artisti** | (num Artisti / 2) × 10 |
| PP degli **Edifici** | Somma di finalAura di ogni Edificio + effetti endgame |

**Vincitore:**
1. Maggior PP totale vince
2. In caso di parità: più Cibo vince
3. Ulteriore parità: vittoria condivisa

### 4.12 Bonus TurnOrderSlot

Quando un giocatore torna sulla tessera Ordine di Turno:
- Se lo spazio ha bonus positivo: **ottiene subito quel Cibo**
- Se lo spazio ha bonus negativo (solo ultimo spazio): **paga quel Cibo** (o perde 2 PP se non ha Cibo)

### 4.13 Variante Esperti

Nella variante esperti, tutti gli Edifici (Era I, II, III) sono **scoperti** fin dall'inizio della partita.

---

## 5. Architettura del Sistema

### 5.1 Schema Alto Livello

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                   │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────────┐   │
│  │    View     │───▶│    Client    │───▶│ ObjectOutputStream│   │
│  └─────────────┘    └──────┬───────┘    └───────────────────┘   │
│                           │              ▲                      │
│                    ┌──────▼───────┐        │                      │
│                    │ServerHandler │────────┘                      │
│                    │  (thread)    │   ObjectInputStream          │
│                    └─────────────┘                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Socket TCP / RMI
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         SERVER                                   │
│                                                                 │
│  ┌─────────────────┐         ┌───────────────────────────────┐  │
│  │  ServerSocket   │────────▶│    SocketClientHandler        │  │
│  │  (port 12345)   │         │    (Thread per client)         │  │
│  └─────────────────┘         └───────────────┬───────────────┘  │
│  ┌─────────────────┐                           │                  │
│  │  RMIServer      │◀──────────────────────────┘                  │
│  │  (port 1099)    │                                                │
│  └─────────────────┘                           │                  │
│                                              ▼                  │
│                              ┌───────────────────────────────┐   │
│                              │       Controller              │   │
│                              │  (synchronized access)        │   │
│                              └───────────────┬───────────────┘   │
│                                              │                  │
│  ┌──────────────────┐           ┌───────────────────────────┐   │
│  │  VirtualView     │◀─────────┤       Game (Model)         │   │
│  │  (Observer)      │          │    (State Machine)         │   │
│  └──────────────────┘           └───────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │         PersistenceManager (game_state.json)           │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Pattern Utilizzati

| Pattern | Utilizzo |
|---------|----------|
| **MVC** | Model (Game), View (VirtualView/GUI), Controller |
| **Observer** | VirtualView osserva Game |
| **Command** | Controller.executeCommand() con lambda |
| **State Machine** | Game delega agli State (SetUp → StartTurn → Picking → Event → EndGame) |
| **Thread per Client** | SocketClientHandler in thread separato |
| **Virtual View** | Traduce eventi Model in messaggi di rete |

### 5.3 Package Structure

```
it.polimi.ingsw.am54/
├── model/
│   ├── Game.java                 # Core game engine, state machine
│   ├── Player.java               # Player entity
│   ├── Board.java                # Game board (offering tiles, rows)
│   ├── Card.java                 # Base card class
│   ├── CharacterCard.java        # Tribe cards (Cacciatore, Costruttore, etc.)
│   ├── BuildingCard.java         # Building cards with costs and effects
│   ├── EventCard.java            # Event cards (Caccia, Sostentamento, etc.)
│   ├── TribeCard.java            # Abstract for Character + Event
│   ├── OfferingTile.java         # Offer tiles A-G
│   ├── TurnOrderSlot.java        # Turn order slots with food bonuses
│   ├── TotemColors.java          # Enum: RED, YELLOW, WHITE, BLACK
│   ├── Deck.java                 # Card deck (Stack<T>)
│   ├── DeckFactory.java          # Deck creation from JSON
│   ├── states/                   # State machine implementation
│   │   ├── GameState.java        # Abstract base state
│   │   ├── SetUpGameState.java   # Players join and choose colors
│   │   ├── StartTurnState.java   # Place totems
│   │   ├── PickingState.java     # Pick cards from rows
│   │   ├── EventState.java       # Resolve events
│   │   └── EndGameState.java     # Calculate scores
│   └── effects/                  # Card effect implementations
├── controller/
│   └── Controller.java           # Command handler (1 per partita)
├── network/
│   ├── server/
│   │   ├── Server.java           # Socket + RMI server entry point
│   │   ├── SocketClientHandler.java  # TCP client handler (Thread per client)
│   │   ├── RMIServerImpl.java    # RMI server implementation
│   │   ├── RMIClientHandler.java # RMI client callback handler
│   │   ├── VirtualView.java      # Observer, translates Game events to network messages
│   │   ├── ClientConnection.java # Interface for sending updates
│   │   └── PersistenceManager.java  # Crash recovery, game_state.json
│   ├── client/
│   │   ├── Client.java           # Client entry point
│   │   ├── ClientModel.java      # Local client state
│   │   ├── ServerHandler.java    # Thread for receiving updates
│   │   ├── SocketServerConnection.java
│   │   └── RMIServerConnection.java
│   ├── messages/
│   │   ├── Request.java          # Base interface (Client → Server)
│   │   ├── Update.java           # Base interface (Server → Client)
│   │   ├── JoinGameRequest.java  # nickname
│   │   ├── SetupGameRequest.java # Start game
│   │   ├── ChooseTotemRequest.java # color (RED/YELLOW/WHITE/BLACK)
│   │   ├── PlaceTotemRequest.java # offerPosition (1-6)
│   │   ├── PickCardRequest.java  # row (UPPER/LOWER), cardIndex
│   │   ├── PingRequest.java      # Heartbeat
│   │   ├── PongResponse.java     # Heartbeat response
│   │   ├── PlayerJoinedUpdate.java
│   │   ├── PlayerDisconnectedUpdate.java
│   │   ├── PlayerReconnectedUpdate.java
│   │   ├── TotemColorUpdate.java
│   │   ├── TotemPlacedUpdate.java
│   │   ├── CardPickedUpdate.java
│   │   ├── TurnChangedUpdate.java
│   │   ├── StateChangedUpdate.java
│   │   ├── EndGameUpdate.java
│   │   └── ErrorMessageUpdate.java
│   └── rmi/
│       ├── RMIGameServer.java    # RMI service interface
│       ├── RMIClientInterface.java
│       └── RMIClientHandlerInterface.java
```

---

## 6. Comunicazione di Rete

### 6.1 Connessione Socket

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

### 6.2 Ciclo di Vita di un Comando

```
CLIENT                                    SERVER
──────                                    ──────
Client.connect()
     │
     ▼ (TCP handshake)
ObjectOutputStream + ObjectInputStream
     │
     ▼
Client.sendMessage(Request)
     │──── TCP Socket ────│
     ▼                    ▼
ServerSocket          SocketClientHandler.run()
                     ObjectInputStream.readObject()
                            │
                            ▼
                     handleRequest(Request)
                            │
                            ▼
                     Controller.handleXxx()
                            │
                            ▼
                     synchronized(game) {
                         game.executeAction()
                     }
                            │
                            ▼
                     Game.notifyObservers()
                            │
                            ▼
                     VirtualView.onXxxEvent()
                            │
                            ▼
                     ClientHandler.sendMessage(Update)
                            │
                            │──── TCP Socket ────│
                            ▼                    ▼
                     ObjectOutputStream    ServerHandler.run()
                                          handleUpdate()
```

### 6.3 Richieste (Client → Server)

| Messaggio | Parametri | Descrizione |
|:----------|:----------|:-------------|
| `JoinGameRequest` | nickname | Primo contatto |
| `ChooseTotemRequest` | color (RED/YELLOW/WHITE/BLACK) | Scelta colore totem |
| `SetupGameRequest` | - | Avvia partita |
| `PlaceTotemRequest` | offerPosition (1-6) | Piazza totem |
| `PickCardRequest` | row (UPPER/LOWER), cardIndex | Pesca carta |
| `PongResponse` | timestamp | Risposta a ping |

### 6.4 Aggiornamenti (Server → Client)

| Messaggio | Parametri | Descrizione |
|:----------|:----------|:-------------|
| `PlayerJoinedUpdate` | nickname | Nuovo giocatore |
| `PlayerDisconnectedUpdate` | nickname | Giocatore disconnesso |
| `PlayerReconnectedUpdate` | nickname | Giocatore riconnesso |
| `TotemColorUpdate` | nickname, color | Colore scelto |
| `TotemPlacedUpdate` | nickname, position | Totem piazzato |
| `CardPickedUpdate` | nickname, row, index | Carta pescata |
| `TurnChangedUpdate` | newCurrentPlayer | Cambio turno |
| `StateChangedUpdate` | stateName | Cambio stato / sync completo |
| `EndGameUpdate` | winners[] | Fine partita |
| `ErrorMessageUpdate` | message | Errore |
| `PingRequest` | timestamp | Heartbeat (ogni 5s) |

### 6.5 Heartbeat e Disconnessione

```
SERVER                                    CLIENT
PingRequest(timestamp) ─────────────────────────▶
                      ◀────────────────── PongResponse(timestamp)

Se nessun Pong in 15 secondi:
         → disconnect() → markDisconnected()
```

**Comportamento:**
- Giocatori disconnessi sono **mantenuti** nel gioco
- Non vengono **saltati** nei turni
- Riconnessione: JoinGameRequest con nickname esistente → handleReconnect()
- Sync stato: StateChangedUpdate completo inviato al reconnect

### 6.6 Socket + RMI

| Protocol | Port | Implementation |
|----------|------|----------------|
| Socket TCP | 12345 | SocketClientHandler (Thread per client) |
| RMI | 1099 | RMIServerImpl + RMIClientHandler |

Entrambi supportano heartbeat e riconnessione.

---

## 7. Thread Safety

```
┌─────────────────────────────────────────────────────────────┐
│                     THREAD SAFETY                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Thread 1 (Client A)     Thread 2 (Client B)    Thread 3   │
│       │                        │                  │         │
│       ▼                        ▼                  ▼         │
│  Controller.handleXxx()  Controller.handleYyy()  ...        │
│       │                        │                            │
│       └────────┬───────────────┘                            │
│                ▼                                            │
│         synchronized(game) {                                │
│             game.executeAction();  ← UN SOLO thread         │
│         }                    ← alla volta può accedere      │
│                │                                            │
│                ▼                                            │
│         Game notifies Observers                             │
│                │                                            │
│                ▼                                            │
│         VirtualView.onEvent()                               │
│                │                                            │
│                ▼                                            │
│         ClientHandler.sendUpdate()                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. Persistenza (Crash Recovery)

### 8.1 Meccanismo

Il server salva periodicamente lo stato completo della partita su `game_state.json`.

**Trigger di salvataggio:**
- Ogni 30 secondi (automatico, ScheduledExecutorService)
- Dopo ogni azione di gioco: setupGame, chooseTotemColor, placeTotem, pickCard
- Su shutdown del server (ShutdownHook JVM)

### 8.2 Cosa viene salvato

**Game:**
- currentEra, roundCount
- currentStateName (SetUpGameState, StartTurnState, PickingState, EventState, EndGameState)
- currentPlayerNickname

**Players:**
- nickname, totemColor
- food, aura, magicPoints
- extraPick, hasPlacedTotem
- eventCostFood, eventCostAura
- buildingDiscount, foodDiscount
- currentTileId
- myTribe[] (carte personaggio in mano)
- myBuildings[] (carte edificio in mano)

**Board:**
- characterUpperRow, characterLowerRow
- eventUpperRow, eventLowerRow
- buildingUpperRow, buildingLowerRow
- offeringTrack (id, cardsFromUpper, cardsFromLower, foodBonus)
- turnOrderTile (id, occupiedByNickname, occupied)

**Decks:**
- deckTribuCards[] (nome, tipo, costo)
- deckBuildingsCards[] (nome, tipo, costo)

### 8.3 Recovery

1. All'avvio, il server verifica l'esistenza di game_state.json
2. Se presente, carica lo stato e ripristina:
   - Giocatori (joinGame + chooseTotemColor)
   - Board (setupGame)
   - Era, round, stato
3. Giocatori che si riconnettono con stesso nickname ricevono StateChangedUpdate per sync completo

---

## 9. Game Flow (State Machine)

```
SetUpGameState
    ↓ (tutti i giocatori hanno scelto colore)
StartTurnState
    ↓ (tutti hanno piazzato totem)
PickingState
    ↓ (ultimo giocatore finisce di pickare)
EventState
    ↓ (deck vuoto)
EndGameState
```

---

## 10. Avvio del Sistema

### 10.1 Build e Test

```bash
# Compilazione
./mvnw clean compile

# Esecuzione test
./mvnw test
```

### 10.2 Server

```bash
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server"
```

Il server avvia:
- Socket TCP sulla porta 12345
- RMI sulla porta 1099
- Una singola partita (Game + Controller)
- Salvataggio automatico ogni 30s

### 10.3 Client (programmatico)

```java
Client client = new Client();
client.connect("localhost", 12345);
client.sendMessage(new JoinGameRequest("Player1"));
```

Vedi `it.polimi.ingsw.am54.network.client.Client` come punto di partenza.

---

## 11. Feature Status

| Feature | Status |
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

## 12. UML Diagrams

Vedi file separati:
- `model.puml` - Diagramma del Model
- `network.puml` - Diagramma di rete con PersistenceManager
- `uml_full.puml` - Diagramma completo del sistema

---

## 13. License

Mesos/Codex Naturalis è proprietà di **Cranio Creations** e tutti i diritti sui materiali grafici appartengono ai rispettivi proprietari. Questo progetto è stato sviluppato per **Politecnico di Milano** in collaborazione con i detentori dei diritti.

---

## 14. Riferimenti

- [RULES.md](./RULES.md) - Regole complete del gioco
- [NETWORK_README.md](./NETWORK_README.md) - Architettura di rete dettagliata
- [SPEC.md](./SPEC.md) - Specifica tecnica completa
- [TEST_COVERAGE_SUMMARY.md](./TEST_COVERAGE_SUMMARY.md) - Copertura test
- [GUI_TUI_SPEC.md](./GUI_TUI_SPEC.md) - Specifica GUI/TUI (non implementato)
- [TUI_IMPLEMENTATION.md](./TUI_IMPLEMENTATION.md) - Piano implementazione TUI (non implementato)