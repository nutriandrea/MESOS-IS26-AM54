# Network Architecture — Codex Naturalis (AM54)

## 1. Dual Transport Layer

The system supports **two transport protocols** — Socket TCP and RMI — sharing the same message protocol and observer pattern.

```
CLIENT                          SERVER
──────                          ──────
Client.java                     Server.java
  ├─ SocketServerConnection ───▶  SocketClientHandler
  │     (Object streams)    ◀──  (per-client thread)
  │
  └─ RMIServerConnection ──────▶ RMIClientHandler
        (RMI stubs)         ◀──  (per-client thread)
                                         │
                                         ▼
                                   Controller
                                   (synchronized)
                                         │
                                         ▼
                                   VirtualView (Observer)
                                   ┌──────────────────┐
                                   │ onStateChanged() │
                                   │ onPlayerJoined() │
                                   │ onEndGame()      │
                                   │ showError()      │
                                   └──────────────────┘
```

---

## 2. Socket TCP

### Connection Setup

```
Server.main()
  └── Server.start(port)
        └── new ServerSocket(port)
              └── accept() loop
                    └── SocketClientHandler(socket, controller)
                          └── Controller.handleJoinGame(nickname, connection)
```

### Per-Client Thread

Each connected client gets a dedicated `SocketClientHandler` thread:

```
SocketClientHandler.run()
  ├── ObjectOutputStream (→ client)
  ├── ObjectInputStream  (← client)
  ├── handleRequest(Request)       // incoming from client
  │    └── controller.handleXxx()  // process on server
  └── sendUpdate(Update)           // outgoing to client
```

### Message Serialization

All `Request` and `Update` objects implement `Serializable`. The client sends `Request` subclasses; the server sends `Update` subclasses and `ErrorMessage`.

```java
// Client → Server
ObjectOutputStream.writeObject(new PlaceTotemRequest(nickname, position));

// Server → Client
ObjectOutputStream.writeObject(new StateChangedUpdate(stateName, ...));
ObjectOutputStream.writeObject(new ErrorMessage("Invalid move"));
```

### Client Reception Thread

```
Client.start()
  └── ServerHandler(connection, clientModel)
        └── thread loop:
              ObjectInputStream.readObject()
                └── if PingRequest → reply Pong
                └── if Update → message.applyTo(clientModel)
                                → notifyUI()
```

---

## 3. RMI

### Server

```java
LocateRegistry.createRegistry(port);
RMIServerImpl remoteServer = new RMIServerImpl(controller);
Naming.rebind("rmi://localhost:" + port + "/AM54", remoteServer);
```

The `RMIServerImpl` exposes remote methods:

```java
public interface RmiServerInterface extends Remote {
    void joinGame(String nickname, RMIClientInterface client) throws RemoteException;
    void chooseTotemColor(String nickname, String color) throws RemoteException;
    // ...
}
```

### Client

```java
RmiServerInterface server = (RmiServerInterface) Naming.lookup("rmi://localhost:" + port + "/AM54");
server.joinGame(nickname, new RMIClientImpl(clientModel));
```

### Callback Pattern

The client registers an `RMIClientInterface` callback with the server. The server calls back to send updates:

```java
public interface RMIClientInterface extends Remote {
    void onStateChanged(String stateName, int era, int round, ...) throws RemoteException;
    void onEndGame(EndGameUpdate update) throws RemoteException;
    void showError(String message) throws RemoteException;
    void ping() throws RemoteException;
}
```

---

## 4. Message Protocol

All protocol messages live in `it.polimi.ingsw.am54.network.messages`.

### Request Hierarchy

```
Request (abstract, Serializable)
├── JoinGameRequest
├── ChooseTotemRequest
├── SetupGameRequest
├── PlaceTotemRequest
├── PickCardRequest
├── EndTurnRequest
├── GetLeaderboardRequest
└── PingRequest
```

### Update Hierarchy

```
Update (abstract, Serializable)
├── PlayerJoinedUpdate
├── TotemColorUpdate
├── TotemPlacedUpdate
├── CardPickedUpdate
├── TurnChangedUpdate
├── StateChangedUpdate      ← full state snapshot
├── EndGameUpdate           ← winners, scores
├── LeaderboardUpdate       ← leaderboard entries
├── PlayerDisconnectedUpdate
├── PlayerReconnectedUpdate
├── GameSuspendedUpdate
├── GameResumedUpdate
├── EventResolvedUpdate
└── ErrorMessage
```

All updates implement `applyTo(ClientModel)` for polymorphism:

```java
// Dispatcher
void handleUpdate(Update message) {
    message.applyTo(clientModel);  // polymorphic update
    clientModel.notifyUI();
}
```

---

## 5. Lifecycle of a Player Action

```
CLIENT                              SERVER
──────                              ──────
1. User action triggers
   Client.sendMessage(Request)
         │
    ╔════╧══════════════════════════╗
    ║       NETWORK TRANSPORT       ║
    ║  (TCP ObjectStreams / RMI)   ║
    ╚════╤══════════════════════════╝
         │
2.       SocketClientHandler.handleRequest()
         └── controller.executeCommand(nickname, () -> {
                 game.getCurrentState().someAction(game, nickname, params);
             })
              │
              ▼
3.        Game executes action
         GameState.someAction()
              │
              ▼
4.        Game.notifyObservers()
              │
              ▼
5.        VirtualView.onXxxEvent()
         └── connection.sendUpdate(Update)
              │
         ╔════╧══════════════════════════╗
         ║       NETWORK TRANSPORT       ║
         ╚════╤══════════════════════════╝
              │
6.       Client receives Update
         ServerHandler / RMI callback
         └── message.applyTo(clientModel)
         └── clientModel.notifyUI()
              │
              ▼
7.       CLI: render() / GUI: refreshBoard()
```

---

## 6. Heartbeat / Ping-Pong

The server detects disconnections via a **heartbeat mechanism**:

```
Server (Controller)
  ├── ScheduledExecutorService (1 thread)
  │    └── every 5 seconds:
  │         └── for each client:
  │               └── clientConnection.sendPing()
  │                     └── Timer(10s timeout)
  │                           └── if no Pong → markDisconnected()
  │
  └── Client side:
        └── On PingRequest → immediately reply with pong
        └── No user-visible effect
```

When a client is marked disconnected:
1. `Controller.markDisconnected(nickname)` is called
2. The player is added to `disconnectedPlayers` set
3. Other clients receive `PlayerDisconnectedUpdate`
4. The game state handles the disconnection via `state.handleDisconnection()`
5. A 60-second reconnect window begins

---

## 7. StateChangedUpdate — Full Snapshot

The most important message is `StateChangedUpdate`, which contains the **complete game state**:

```java
public class StateChangedUpdate extends Update {
    private final String stateName;              // e.g. "PickingState"
    private final int currentEra;
    private final int currentRound;
    private final Map<String, PlayerSnapshot> playersStats;
    private final List<String> turnOrderTrack;
    private final Map<Integer, String> offeringTrack;  // tile id → occupant
    private final List<String> characterUpperRow;
    private final List<String> characterLowerRow;
    private final List<String> eventUpperRow;
    private final List<String> eventLowerRow;
    private final List<String> buildUpperRow;
    private final List<String> buildLowerRow;
}
```

The server sends this **on every state change** — the client completely replaces its `ClientModel` state with the new snapshot. This avoids incremental update bugs and simplifies reconnect.

---

## 8. Reconnect Flow

When a disconnected player reconnects:

```
1. Client sends JoinGameRequest(nickname)
2. Controller detects nickname in disconnectedPlayers
3. handleReconnect():
   a. Removes old VirtualView
   b. Creates new VirtualView with new connection
   c. Adds as observer to Game
   d. Calls sendFullStateOnReconnect(game)
       └── sends current StateChangedUpdate snapshot
       └── sends player hand cards
       └── sends pending events
4. Client model fully restored from snapshot
5. Other clients receive PlayerReconnectedUpdate
```

If the player does not reconnect within 60 seconds, the game removes them permanently via `state.handleDisconnection(game, player, isCurrentPlayer)`. The behaviour depends on the game state:
- **Lobby/Setup**: Player is removed; game continues if enough players remain
- **Mid-game**: Current player's totem is removed, turn advances automatically
- **EndGame**: No-op (already finished)
