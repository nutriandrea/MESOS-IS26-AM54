# Architettura di Rete - AM54

## 1. Diagramma dei Componenti

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                  │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────────┐   │
│  │    View     │───▶│    Client    │───▶│ ObjectOutputStream│   │
│  └─────────────┘    └──────┬───────┘    └───────────────────┘   │
│                           │              ▲                      │
│                    ┌──────▼───────┐        │                      │
│                    │ServerHandler │────────┘                      │
│                    │  (thread)    │   ObjectInputStream          │
│                    └─────────────┘                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Socket TCP
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         SERVER                                  │
│                                                                 │
│  ┌─────────────────┐         ┌───────────────────────────────┐  │
│  │  ServerSocket   │────────▶│    SocketClientHandler        │  │
│  │  (port 1234)    │         │    (Runnable - Thread per     │  │
│  └─────────────────┘         │     client)                   │  │
│                              └───────────────┬───────────────┘  │
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
│  └────────┬─────────┘           └───────────────────────────┘  │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                            │
│  │ ClientHandler  │ ◀── send updates to specific client        │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Flusso Dati Completo

### 2.1 Connessione di un Client

```
1. Server.main() / Server.start() → ServerSocket.accept()
2. Crea SocketClientHandler(socket, controller)
3. Crea VirtualView(handler)
4. game.addObserver(vView)
5. new Thread(handler).start()
```

### 2.2 Ciclo di Vita di un Comando

```
CLIENT                                    SERVER
─────                                    ──────
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

---

## 3. Descrizione delle Classi Network

### 3.1 Server (`it.polimi.ingsw.am54.network.server.Server`)

**Responsabilità**: Entry point del server, gestisce le connessioni in ingresso.

**Funzionamento**:
- Entry point: `main(String[] args)` - porta default 12345
- Crea un'istanza di `Game` (Model) e `Controller`
- Apre un `ServerSocket` sulla porta specificata
- Loop infinito: `accept()` per ogni nuovo client
- Per ogni client crea:
  - `SocketClientHandler` - gestisce la comunicazione
  - `VirtualView` - osserva il Model per inviare update
  - Thread dedicato per gestire il client
- Limita a massimo 4 giocatori

**Avvio**:
```bash
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server"
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server" -Dexec.args="8080"
```

---

### 3.2 SocketClientHandler (`it.polimi.ingsw.am54.network.server.SocketClientHandler`)

**Responsabilità**: Thread dedicato per ogni client, gestisce I/O socket.

**Funzionamento**:
- Implementa `Runnable` - eseguito in un Thread separato
- Inizializza `ObjectInputStream` e `ObjectOutputStream`
- Loop di ricezione comandi: `in.readObject()`
- Parsa il tipo di Request e invoca metodi del Controller
- Tiene traccia del `playerNickname` per autenticazione

**Request gestite**:
- `JoinGameRequest` → `controller.handleJoinGame(nickname, this)`
- `SetupGameRequest` → `controller.handleSetupGame(nickname)`
- `ChooseTotemRequest` → `controller.handleChooseTotemColor(nickname, color)`
- `PlaceTotemRequest` → `controller.handlePlaceTotem(nickname, position)`
- `PickCardRequest` → `controller.handlePickCard(nickname, row, index)`

---

### 3.3 ClientHandler (`it.polimi.ingsw.am54.network.server.ClientHandler`)

**Interfaccia** per l'invio di messaggi al client.

```java
public interface ClientHandler {
    void sendMessage(Update message);
    void sendError(String errorMessage);
    void disconnect();
}
```

---

### 3.4 VirtualView (`it.polimi.ingsw.am54.network.server.VirtualView`)

**Responsabilità**: Observer del Model, traduce gli eventi di gioco in messaggi di rete.

**Funzionamento**:
- Implementa `IGameObserver`
- Per ogni evento del gioco, crea il corrispondente `Update` message
- Invia il messaggio al client tramite `ClientHandler`

| Evento Model | Metodo VirtualView | Message Inviata |
|--------------|--------------------|-----------------|
| player joined | `onPlayerJoined()` | `PlayerJoinedUpdate` |
| totem color chosen | `onTotemColorChosen()` | `TotemColorUpdate` |
| totem placed | `onTotemPlaced()` | `TotemPlacedUpdate` |
| card picked | `onCardPicked()` | `CardPickedUpdate` |
| turn changed | `onTurnChanged()` | `TurnChangedUpdate` |
| state changed | `onStateChanged()` | `StateChangedUpdate` |
| game ended | `onEndGame()` | `EndGameUpdate` |

---

### 3.5 Controller (`it.polimi.ingsw.am54.controller.Controller`)

**Responsabilità**: Gestisce i comandi e sincronizza l'accesso al Model.

**Funzionamento**:
- Mantiene una mappa `nickname → ClientHandler` per inviare errori
- Ogni handler `handleXxx()` chiama il metodo corrispondente sul Game
- uso di `synchronized(game)` per thread safety
- In caso di errore, `signalError()` invia `ErrorMessageUpdate` al client

---

### 3.6 Client (`it.polimi.ingsw.am54.network.client.Client`)

**Responsabilità**: Punto di ingresso lato client per la connessione di rete.

**Funzionamento**:
- `connect(ip, port)` - stabilisce connessione TCP, aspetta ACK, avvia ServerHandler
- `sendMessage(ClientMessage)` - serializza e invia al server
- `disconnect()` - chiude socket e ferma ServerHandler

---

### 3.7 ServerHandler (`it.polimi.ingsw.am54.network.client.ServerHandler`)

**Responsabilità**: Thread parallelo per ricevere Update dal server.

**Funzionamento**:
- Costruttore riceve `ObjectInputStream` già aperto
- Loop infinito: `in.readObject()` bloccante
- `handleUpdate(Update)` - dispatch del messaggio (da collegare a GUI)

---

## 4. Messaggi (Package `it.polimi.ingsw.am54.network.messages`)

### Gerarchia delle Interfacce

```
ClientMessage extends Serializable
    ├── Request (Client → Server)
    │    ├── JoinGameRequest
    │    ├── ChooseTotemRequest
    │    ├── SetupGameRequest
    │    ├── PlaceTotemRequest
    │    └── PickCardRequest
    │
    └── Update (Server → Client)
         ├── ErrorMessageUpdate
         ├── PlayerJoinedUpdate
         ├── TotemColorUpdate
         ├── TotemPlacedUpdate
         ├── CardPickedUpdate
         ├── TurnChangedUpdate
         ├── StateChangedUpdate
         └── EndGameUpdate
```

### Request (Client → Server)

| Classe | Contenuto |
|--------|-----------|
| `JoinGameRequest` | `String nickname` |
| `ChooseTotemRequest` | `String nickname, String color` |
| `SetupGameRequest` | `String nickname` |
| `PlaceTotemRequest` | `String nickname, int offerPosition` |
| `PickCardRequest` | `String nickname, String row, int cardIndex` |

### Update (Server → Client)

| Classe | Contenuto |
|--------|-----------|
| `PlayerJoinedUpdate` | `String nickname` |
| `TotemColorUpdate` | `String nickname, String color` |
| `TotemPlacedUpdate` | `String nickname, int offerPosition` |
| `CardPickedUpdate` | `String nickname, String row, int cardIndex` |
| `TurnChangedUpdate` | `String newCurrentPlayer` |
| `StateChangedUpdate` | `String newState` (nome classe) |
| `EndGameUpdate` | `List<String> winners` |
| `ErrorMessageUpdate` | `String message` |

---

## 5. Flusso di Sincronizzazione

```
┌─────────────────────────────────────────────────────────────┐
│                     THREAD SAFETY                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Thread 1 (Client A)     Thread 2 (Client B)    Thread 3    │
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

## 6. Pattern Utilizzati

| Pattern | Utilizzo |
|---------|----------|
| **Observer** | `VirtualView` observer del `Game` (Model) |
| **State Machine** | `Game` delega agli `State` per validazione |
| **MVC** | Controller interpola View e Model |
| **Thread per Client** | Ogni socket in un Thread separato |
| **Command** | Azioni incapsulate in `GameAction` (lambda) |
| **Virtual View** | Traduce eventi Model in messaggi di rete |

---

## 7. Avvio del Sistema

### Server
```bash
# Porta default 12345
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server"

# Porta personalizzata
./mvnw compile exec:java -Dexec.mainClass="it.polimi.ingsw.am54.network.server.Server" -Dexec.args="8080"
```

### Client (da GUI/TUI)
```java
Client client = new Client();
client.connect("localhost", 12345);
client.sendMessage(new JoinGameRequest("Player1"));
```

---

## 8. Limitazioni e Prossimi Passi

| Funzionalità | Stato |
|--------------|-------|
| Socket communication | ✅ Completo |
| Multiple games support | 🔄 Da implementare |
| RMI communication | 🔄 Da implementare |
| Reconnection handling | 🔄 Da implementare |
| Client GUI/TUI integration | 🔄 Da implementare |
