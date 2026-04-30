# Specifiche GUI e TUI - Prova Finale AM54

## 1. Panoramica Framework

### 1.1 Libreria vs Framework

| Libreria | Framework |
|----------|-----------|
| Riutilizzo di funzioni/componenti pre-scritti | Fornisce struttura e regole da seguire |
| Voi decidete quando/come usarla | Inversione di controllo |
| Espande funzionalità di codebase esistente | Velocizza sviluppo con strumenti pronti |
| DRY principle nel codice | DRY principle nell'architettura |

### 1.2 Scelta: JavaFX

Per questo progetto si utilizza **JavaFX** perché:
- Framework moderno per GUI in Java
- Supporto per animazioni, multimedia, CSS, FXML
- Architettura MVC nativa
- SceneBuilder per design visuale

---

## 2. Architettura JavaFX

### 2.1 Componenti Principali

```
Stage (Finestra)
    └── Scene (Contenuto)
        └── Node (Elementi grafici)
            ├── Text, Button, Shape
            ├── ImageView
            ├── Layout (VBox, HBox, GridPane...)
            └── ...
```

### 2.2 Approcci di Costruzione

**Approccio Imperativo (Java puro):**
```java
public class HelloApp extends Application {
    private Parent createContent() {
        return new StackPane(new Text("Hello World"));
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setScene(new Scene(createContent(), 300, 300));
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
```

**Approccio Dichiarativo (FXML):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.StackPane?>
<?import javafx.scene.text.Text?>
<StackPane xmlns:fx="http://javafx.com/fxml" 
           prefWidth="300.0" prefHeight="300.0">
    <children>
        <Text text="Hello World"/>
    </children>
</StackPane>
```

**Confronto:**

| Aspetto | Imperativo | Dichiarativo |
|---------|-----------|--------------|
| Errori | Compile time | Runtime |
| Parsing | Nessuno | Richiesto |
| Controllo | Massimo | Limitato |
| Produttività | Minore | Maggiore (WYSIWYG) |

---

## 3. Layout Manager

### 3.1 Layout Base

| Layout | Descrizione |
|--------|-------------|
| **VBox** | Organizza nodi verticalmente |
| **HBox** | Organizza nodi orizzontalmente |
| **StackPane** | Elementi sovrapposti |
| **GridPane** | Griglia (righe x colonne) |
| **TilePane** | Griglia regolare |
| **AnchorPane** | Layout dinamico relativo ai bordi |
| **BorderLayout** | 5 aree (North, South, East, West, Center) |
| **FlowLayout** | Disposizione in linea |
| **GridLayout** | Griglia con dimensioni regolari |

### 3.2 Esempi

**VBox + HBox:**
```java
VBox vbox = new VBox();
Button btn = new Button("Refresh");
Label label = new Label("Test");
HBox topControls = new HBox();
topControls.getChildren().addAll(btn, label);
vbox.getChildren().addAll(topControls, table);
```

**GridPane:**
```java
GridPane grid = new GridPane();
grid.add(new Label("Nome:"), 0, 0);
grid.add(textField, 1, 0);
grid.add(new Label("Cognome:"), 0, 1);
grid.add(textField2, 1, 1);
```

**AnchorPane:**
```java
AnchorPane ap = new AnchorPane();
Hyperlink link = new Hyperlink("Sign Out");
ap.getChildren().add(link);
AnchorPane.setTopAnchor(link, 10.0);
AnchorPane.setRightAnchor(link, 10.0);
```

---

## 4. Event Handling

### 4.1 Tipi di Eventi

| Categoria | Evento | Oggetto |
|-----------|--------|---------|
| Azione | Button click, Menu selection | ActionEvent |
| Mouse | Click, Double-click, Drag | MouseEvent |
| Keyboard | Tasti premuti | KeyEvent |
| Focus | Gain/Lose focus | FocusEvent |
| Selezione | ComboBox, List selection | ItemEvent |

### 4.2 Registrazione Listener

**Button con Action:**
```java
Button button = new Button("Clicca");
button.setOnAction(event -> {
    performAction();
});
button.fire(); // programmatic click
```

**Mouse Events:**
```java
button.setOnMouseEntered(e -> { /* mouse entrato */ });
button.setOnMouseExited(e -> { /* mouse uscito */ });
button.setOnMousePressed(e -> { /* mouse premuto */ });
button.setOnMouseReleased(e -> { /* mouse rilasciato */ });
```

**Altre registrazioni:**
```java
node.setOnKeyPressed(e -> { /* tastiera */ });
node.setOnScroll(e -> { /* scroll mouse */ });
```

---

## 5. Threading

### 5.1 Modello JavaFX

```
┌─────────────────────────────────────┐
│         UI Thread (singolo)         │
│                                     │
│  Event Queue                        │
│  ┌─────┐ ┌─────┐ ┌─────┐            │
│  │ E1  │ │ E2  │ │ E3  │  ...       │
│  └─────┘ └─────┘ └─────┘            │
│       ↓         ↓                   │
│   Event Listeners                   │
└─────────────────────────────────────┘
         ↑
  Worker Threads (background)
```

### 5.2 Regole Fondamentali

1. **Non bloccare l'UI Thread**: operazioni pesanti devono essere eseguite in thread separati
2. **Delegare all'UI Thread**: per aggiornare la GUI da altri thread, usare `Platform.runLater()`

### 5.3 Esempi

**Operazione lenta in background:**
```java
void slowOperation() {
    // NON fare direttamente sull'UI thread
}

new Thread(() -> {
    slowOperation();
}).start();

// Oppure con Executor
ExecutorService executor = Executors.newCachedThreadPool();
executor.submit(() -> {
    slowOperation();
});
```

**Aggiornare UI da background thread:**
```java
new Thread(() -> {
    String result = fetchData();
    Platform.runLater(() -> {
        label.setText(result);
    });
}).start();
```

### 5.4 Timer

```java
Timer timer = new Timer();
TimerTask task = new TimerTask() {
    @Override
    public void run() {
        Platform.runLater(() -> {
            updateUI();
        });
    }
};
// schedule(task, delay, interval)
timer.schedule(task, 1000, 2000);
timer.cancel();
```

---

## 6. Properties e Binding

### 6.1 Property

Una **Property** è un oggetto che rappresenta un valore modificabile e osservabile.

```java
StringProperty text = new SimpleStringProperty("Hello");
text.addListener((obs, oldVal, newVal) -> {
    System.out.println("Cambiato: " + oldVal + " -> " + newVal);
});
text.set("World"); // Trigger listener
```

### 6.2 Binding

**Unidirezionale (A segue B):**
```java
label.textProperty().bind(textField.textProperty());
// Se textField cambia, label si aggiorna automaticamente
```

**Bidirezionale:**
```java
textField1.textProperty().bindBidirectional(textField2.textProperty());
// Cambia uno, cambia l'altro
```

**Con trasformazioni:**
```java
slider.valueProperty().addListener((obs, oldVal, newVal) -> {
    label.setText(newVal.intValue() + "");
});

// Oppure con binding
label.textProperty().bind(slider.valueProperty().asString("%.0f"));
```

### 6.3 Binding con Observable

```java
// ObservableList per TableView
ObservableList<Person> data = FXCollections.observableArrayList(
    new Person("Mario", "Rossi"),
    new Person("Luigi", "Verdi")
);
table.setItems(data);

// ObservableMap
ObservableMap<String, Integer> scores = FXCollections.observableHashMap();
scores.addListener((MapChangeListener) change -> {
    updateUI();
});
```

---

## 7. Immagini

```java
// Caricamento immagine
Image image = new Image("file:resources/cards/hunter.png", 100, 100, true, true);

// ImageView per visualizzazione
ImageView imageView = new ImageView(image);
imageView.setPreserveRatio(true);
imageView.setFitWidth(100);
imageView.setFitHeight(100);

// Proprietà interattive
imageView.setOnMouseClicked(e -> handleCardClick());
```

---

## 8. Trasformazioni

```java
Rectangle box = new Rectangle(100, 50, Color.BLUE);

// Traslazione
box.setTranslateX(100);
box.setTranslateY(200);

// Rotazione
box.setRotate(45);

// Scaling
box.setScaleX(1.5);
box.setScaleY(0.8);
```

---

## 9. Struttura MVC per il Progetto

### 9.1 Model

```java
public class ClientModel {
    private final StringProperty currentState = new SimpleStringProperty();
    private final ObservableList<String> players = FXCollections.observableArrayList();
    private final ObservableMap<String, Integer> playerFood = FXCollections.observableHashMap();
    
    // Getters per Properties
    public StringProperty currentStateProperty() { return currentState; }
    public ObservableList<String> getPlayers() { return players; }
    
    // Update methods (chiamati da ServerHandler)
    public void updateState(String state) {
        Platform.runLater(() -> currentState.set(state));
    }
}
```

### 9.2 View (FXML)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.VBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.TableView?>

<VBox xmlns:fx="http://javafx.com/fxml" spacing="10">
    <children>
        <Label id="stateLabel" text="Stato: Lobby"/>
        <Button text="Setup Game" onAction="#handleSetupGame"/>
        <TableView id="playersTable"/>
    </children>
</VBox>
```

### 9.3 Controller

```java
public class GameController implements Initializable {
    @FXML private Label stateLabel;
    @FXML private Button setupButton;
    @FXML private TableView<Player> playersTable;
    
    private ClientModel model;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Bindings
        stateLabel.textProperty().bind(model.currentStateProperty());
    }
    
    @FXML
    private void handleSetupGame() {
        model.sendRequest(new SetupGameRequest());
    }
    
    public void setModel(ClientModel model) {
        this.model = model;
    }
}
```

---

## 10. TUI (Interfaccia Testuale)

### 10.1 Struttura Base

```java
public class ClientMain {
    private ClientModel model;
    private ServerConnection connection;
    private Scanner scanner;
    
    public static void main(String[] args) {
        ClientMain client = new ClientMain();
        client.run();
    }
    
    public void run() {
        scanner = new Scanner(System.in);
        printWelcome();
        
        while (running) {
            String input = scanner.nextLine();
            handleInput(input);
        }
    }
    
    private void handleInput(String input) {
        String[] parts = input.split(" ");
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "join":
                handleJoin(parts);
                break;
            case "color":
                handleColor(parts);
                break;
            case "place":
                handlePlace(parts);
                break;
            case "pick":
                handlePick(parts);
                break;
            case "quit":
                handleQuit();
                break;
            default:
                printHelp();
        }
    }
}
```

### 10.2 Comandi TUI

| Comando | Esempio | Descrizione |
|---------|---------|-------------|
| `join <nickname>` | `join Mario` | Entra nel gioco |
| `color <red\|yellow\|white\|black>` | `color red` | Scegli colore totem |
| `setup` | `setup` | Avvia partita |
| `place <1-6>` | `place 3` | Piazza totem |
| `pick <U\|L> <index>` | `pick U 0` | Pesca carta |
| `games` | `games` | Lista partite |
| `create` | `create` | Crea nuova partita |
| `help` | `help` | Mostra aiuto |
| `quit` | `quit` | Esci |

### 10.3 Visualizzazione Stato

```java
public void printGameState() {
    System.out.println("\n=== STATO GIOCO ===");
    System.out.println("Stato: " + model.getCurrentState());
    System.out.println("Turno: " + model.getCurrentPlayer());
    System.out.println("\nGiocatori:");
    for (String p : model.getPlayers()) {
        System.out.println("  - " + p + 
            " [Cibo: " + model.getPlayerFood(p) + 
            ", Aura: " + model.getPlayerAura(p) + "]");
    }
    System.out.println("\nOfferte:");
    for (var entry : model.getOfferingTrack().entrySet()) {
        System.out.println("  " + entry.getKey() + ": " + entry.getValue());
    }
    System.out.println("==================\n");
}
```

---

## 11. Checklist Implementazione

### GUI (JavaFX)

- [ ] ScenePrincipale con menu
- [ ] LoginView (nickname, gameId opzionale)
- [ ] LobbyView (lista giocatori, stato)
- [ ] GameView (board, carte, offerte)
- [ ] HandView (carte del giocatore)
- [ ] StatisticheView (punteggi)
- [ ] Binding bidirezionale Model-View
- [ ] Gestione eventi UI -> Request
- [ ] Aggiornamenti automatici da Update

### TUI (CLI)

- [ ] Loop input utente
- [ ] Parser comandi
- [ ] Stampa stato partita
- [ ] Stampa lista partite
- [ ] Visualizzazione carte offerta
- [ ] Gestione colori e formattazione

### Comuni

- [ ] Connessione Socket/RMI selezionabile
- [ ] Gestione errori e riconnessione
- [ ] Thread-safe Model updates
- [ ] Platform.runLater() per UI

---

## 12. Riferimenti

- [JavaFX Documentation](https://openjfx.io/openjfx-docs/)
- [JavaFX API Reference](https://docs.oracle.com/javase/8/javafx/api/toc.html)
- [SceneBuilder Download](https://gluonhq.com/products/scene-builder/)