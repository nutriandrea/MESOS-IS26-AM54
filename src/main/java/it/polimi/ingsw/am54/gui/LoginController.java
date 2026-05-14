package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LoginController {

    @FXML private StackPane rootPane;
    @FXML private GridPane uiLayer; // Assicurati che sia GridPane per matchare l'FXML
    @FXML private VBox logoContainer;
    @FXML private VBox playStateBox;
    @FXML private VBox nicknameStateBox;

    @FXML private TextField serverIpField;
    @FXML private RadioButton socketRadio;
    @FXML private TextField nicknameField;
    @FXML private Label errorLabel;

    @FXML private Label clickToEnterLabel;
    @FXML private MediaView mediaView;

    private boolean waitingForClick = false;
    private boolean isConnecting = false;

    @FXML
    public void initialize() {
        // Nascondiamo la mediaView all'inizio per evitare che intercetti i click
        mediaView.setVisible(false);
        mediaView.setManaged(false);

        Client.getInstance().getModel().setOnModelUpdatedCallback(() -> {
            Platform.runLater(this::checkServerResponse);
        });
    }

    @FXML
    private void handlePlayAction() {
        // Nascondi logo e tasto PLAY
        logoContainer.setVisible(false);
        logoContainer.setManaged(false);
        playStateBox.setVisible(false);
        playStateBox.setManaged(false);

        // Mostra la box del nickname
        nicknameStateBox.setVisible(true);
        nicknameStateBox.setManaged(true);

        nicknameField.requestFocus();
    }

    @FXML
    private void handleNicknameSubmit() {
        String ip = serverIpField.getText().trim();
        String nick = nicknameField.getText().trim();

        if (ip.isEmpty() || nick.isEmpty()) {
            errorLabel.setText("DATI MANCANTI!");
            return;
        }

        if (isConnecting) return;
        isConnecting = true;
        errorLabel.setText("CONNESSIONE...");

        try {
            // Chiudi eventuali vecchie connessioni fallite prima di riprovare
            if (Client.getInstance().getConnection() != null) {
                // Se la tua classe connection ha un metodo close()/disconnect(), chiamalo qui
                // Altrimenti, forziamo semplicemente la sovrascrittura
            }

            ServerConnection connection;
            ClientModel model = Client.getInstance().getModel();

            // Assicurati che l'errore precedente sia pulito
            model.clearError();

            if (socketRadio.isSelected()) {
                connection = new SocketServerConnection(ip, 12345, model);
            } else {
                Registry registry = LocateRegistry.getRegistry(ip, 1099);
                RMIGameServer serverStub = (RMIGameServer) registry.lookup("AM54-Server");
                connection = new RMIServerConnection(serverStub, model);
            }

            Client.getInstance().setConnection(connection);
            Client.getInstance().setNickname(nick);

            // Invio la richiesta
            connection.joinGame(nick);

        } catch (Exception e) {
            errorLabel.setText("ERRORE: " + e.getMessage());
            isConnecting = false;
        }
    }

    private void checkServerResponse() {
        if (!isConnecting) return;

        ClientModel model = Client.getInstance().getModel();
        String myNick = Client.getInstance().getNickname();

        // CASO 1: IL SERVER CI HA RIFIUTATO (Es. Nickname già in uso)
        if (model.getLastErrorMessage() != null) {
            // 1. Mostriamo l'errore
            errorLabel.setText(model.getLastErrorMessage().toUpperCase());

            // 2. Puliamo il modello dall'errore per poter riprovare
            model.clearError();

            // 3. Resettiamo lo stato di connessione nel client (se necessario)
            Client.getInstance().setConnection(null);
            Client.getInstance().setNickname(null);

            // 4. Sblocchiamo l'interfaccia
            isConnecting = false;

            // 5. Evidenziamo il campo nickname per farlo cambiare all'utente
            nicknameField.selectAll();
            nicknameField.requestFocus();
        }
        // CASO 2: IL SERVER CI HA ACCETTATO
        else if (model.getPlayers().contains(myNick)) {
            errorLabel.setText("");
            isConnecting = false;

            // Nascondiamo la box del nickname
            nicknameStateBox.setVisible(false);
            nicknameStateBox.setManaged(false);

            // Facciamo riapparire il logo Mesos
            logoContainer.setVisible(true);
            logoContainer.setManaged(true);

            // Mostriamo la scritta finale bianca in basso
            clickToEnterLabel.setVisible(true);
            clickToEnterLabel.setManaged(true);

            waitingForClick = true;
        }
    }

    @FXML
    private void handleRootClick() {
        if (waitingForClick) {
            waitingForClick = false;

            uiLayer.setVisible(false);
            uiLayer.setManaged(false);
            clickToEnterLabel.setVisible(false);
            clickToEnterLabel.setManaged(false);

            startTransitionVideo();
        }
    }

    private void startTransitionVideo() {
        try {
            // 1. Cerchiamo il file video
            URL videoUrl = getClass().getResource("/videos/transizione.mp4");

            // Se il video non esiste, entriamo subito nella lobby per non bloccare il gioco
            if (videoUrl == null) {
                System.err.println("File video non trovato in resources/videos/transizione.mp4");
                loadLobby();
                return;
            }

            // 2. Prepariamo il MediaPlayer
            Media media = new Media(videoUrl.toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(media);

            // Colleghiamo il player alla MediaView dell'FXML
            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.setVisible(true);
            mediaView.setManaged(true);

            // 3. ALLINEAMENTO PERFETTO: Disattiviamo il rapporto d'aspetto fisso
            // Questo serve per far sì che il video si "stiri" esattamente come lo sfondo della lobby
            mediaView.setPreserveRatio(false);

            // Bindiamo le dimensioni del video a tutto lo schermo (rootPane)
            mediaView.fitWidthProperty().bind(rootPane.widthProperty());
            mediaView.fitHeightProperty().bind(rootPane.heightProperty());

            // 4. GESTIONE FINE VIDEO (Transizione fluida)
            mediaPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    // Carichiamo la scena della Lobby (lo sfondo apparirà "sotto" il video)
                    loadLobby();

                    // Usiamo un thread separato per un brevissimo delay (100ms)
                    // Questo evita il flash nero tra la fine del video e il rendering della lobby
                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                            Platform.runLater(() -> {
                                // Solo ora nascondiamo e distruggiamo il player
                                mediaView.setVisible(false);
                                mediaPlayer.dispose();
                                System.out.println("Transizione completata con successo.");
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            loadLobby(); // Fallback estremo
                        }
                    }).start();
                });
            });

            // 5. Partenza!
            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Errore critico durante l'avvio della transizione: " + e.getMessage());
            // In caso di errore imprevisto, forziamo l'entrata nella lobby
            loadLobby();
        }
    }

    private void loadLobby() {
        try {
            Parent lobbyRoot = FXMLLoader.load(getClass().getResource("/fxml/Lobby.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();

            // Cambia la radice
            stage.getScene().setRoot(lobbyRoot);

            // Riconferma il full screen (fondamentale per evitare che torni in finestra)
            stage.setFullScreen(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}