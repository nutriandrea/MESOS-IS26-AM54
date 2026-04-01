# Architettura di Rete - AM54

## 1. Diagramma dei Componenti

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
│  └────────┬─────────┘           └───────────────────────────┘   │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                            │
│  │ ClientHandler  │ ◀── send updates to specific client         │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Flusso Dati Completo

### 2.1 Connessione di un Client

```
1. Server.start() → ServerSocket.accept()
2. Crea SocketClientHandler(socket, controller)
3. Crea VirtualView(handler)
4. game.addObserver(vView)
5. new Thread(handler).start()
```

### 2.2 Ciclo di Vita di un Comando

```
CLIENT                                    SERVER
──────                                    ──────
View.composeCommand()
     │
     ▼ (serializza oggetto)
ObjectOutputStream.writeObject()
     │
     │──── TCP Socket ────│
     ▼                    ▼
ServerSocket          SocketClientHandler.run()
                      ObjectInputStream.readObject()
                             │
                             ▼
                      SocketClientHandler.parseRequest()
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
                      ClientHandler.sendMessage()
                             │
                             │──── TCP Socket ────│
                             ▼                    ▼
                      ObjectOutputStream    Client receives
```

---

## 3. Descrizione delle Classi Network

### 3.1 Server (`it.polimi.ingsw.am54.network.server.Server`)

**Responsabilità**: Entry point del server, gestisce le connessioni in ingresso.

**Funzionamento**:
- Crea un'istanza di `Game` (Model) e `Controller`
- Apre un `ServerSocket` sulla porta specificata
- Loop infinito: `accept()` per ogni nuovo client
- Per ogni client crea:
  - `SocketClientHandler` - gestisce la comunicazione
  - `VirtualView` - osserva il Model per inviare update
  - Thread dedicato per gestire il client

```java
// Pseudocodice
public void start() {
    while (true) {
        Socket socket = serverSocket.accept();
        SocketClientHandler handler = new SocketClientHandler(socket, controller);
        VirtualView vView = new VirtualView(handler);
        game.addObserver(vView);
        new Thread(handler).start();
    }
}
```

---

### 3.2 SocketClientHandler (`it.polimi.ingsw.am54.network.server.SocketClientHandler`)

**Responsabilità**: Thread dedicato per ogni client, gestisce I/O socket.

**Funzionamento**:
- Implementa `Runnable` - eseguito in un Thread separato
- Inizializza `ObjectInputStream` e `ObjectOutputStream`
- Loop di ricezione comandi: `in.readObject()`
- Chiama metodi del Controller per ogni comando ricevuto
- Gestisce errori di connessione e cleanup

**Nota**: Il loop di ricezione (riga 28-32) è attualmente **vuoto** - deve essere implementato.

```java
public void run() {
    out = new ObjectOutputStream(socket.getOutputStream());
    in = new ObjectInputStream(socket.getInputStream());
    
    while (connected) {
        Object input = in.readObject();
        // TODO: parse e chiama controller
    }
}
```

---

### 3.3 ClientHandler (`it.polimi.ingsw.am54.network.server.ClientHandler`)

**Responsabilità**: Interfaccia per l'invio di messaggi al client.

**Metodi**:
- `sendMessage(Serializable message)` - invia un messaggio
- `sendError(String errorMessage)` - invia un errore

```java
public interface ClientHandler {
    void sendMessage(Serializable message);
    void sendError(String errorMessage);
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

### 3.5 Messaggi (Package `it.polimi.ingsw.am54.network.messages`)

Tutti i messaggi implementano `Serializable` per la trasmissione su socket.

#### Request (Client → Server)

| Classe | Contenuto | Interfaccia |
|--------|-----------|-------------|
| `JoinGameRequest` | `String nickname` | `ClientMessage` |
| `ChooseTotemRequest` | `String nickname, String color` | `ClientMessage` |
| `SetupGameRequest` | `String nickname` | `ClientMessage` |
| `PlaceTotemRequest` | `String nickname, int offerPosition` | `ClientMessage` |
| `PickCardRequest` | `String nickname, String row, int cardIndex` | `ClientMessage` |

#### Update Messages (Server → Client)

| Classe | Contenuto |
|--------|-----------|
| `PlayerJoinedUpdate` | `String nickname` |
| `TotemColorUpdate` | `String nickname, String color` |
| `TotemPlacedUpdate` | `String nickname, int offerPosition` |
| `CardPickedUpdate` | `String nickname, String row, int cardIndex` |
| `TurnChangedUpdate` | `String newCurrentPlayer` |
| `StateChangedUpdate` | `String newState` (nome classe) |
| `EndGameUpdate` | `List<String> winners` |

#### Error Messages (Server → Client)

| Classe | Contenuto |
|--------|-----------|
| `ErrorMessage` | `String message` |

---

## 4. Problemi e Limiti Attuali

### 4.1 Controller.signalError() Incompleto

**Posizione**: `Controller.java:76-79`

**Problema**: Il metodo `signalError()` non ha accesso al `ClientHandler` per inviare l'errore al client corretto.

```java
// Codice attuale - non funziona
private void signalError(String nickname, String message) {
    LOGGER.warning("Segnalazione errore a " + nickname + ": " + message);
    // TODO: Qui serve accesso al ClientHandler
}
```

**Soluzione**: Passare una mappa `nickname → ClientHandler` al Controller.

---

### 4.2 Loop di Ricezione Vuoto

**Posizione**: `SocketClientHandler.java:28-32`

**Problema**: Il loop che legge i comandi dal client è vuoto.

```java
// Codice attuale
while (connected) {
    Object input = in.readObject();
    // MANCA: parsing del comando e chiamata al controller
}
```

**Soluzione**: Creare classi Request (es. `PickCardRequest`) e implementare il parsing.

---

### 4.3 Request Create

**Stato**: ✅ Implementate

Classi create nel package `network.messages`:
- `ClientMessage` (interfaccia base)
- `JoinGameRequest`
- `ChooseTotemRequest`
- `SetupGameRequest`
- `PlaceTotemRequest`
- `PickCardRequest`

---

## 5. Flusso Sincronizzazione

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

---

## 7. Avvio del Sistema

```bash
# Server
Server server = new Server(1234);
server.start();

# Client (da implementare)
Socket socket = new Socket("localhost", 1234);
ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
out.writeObject(new JoinGameRequest("Player1"));
```
