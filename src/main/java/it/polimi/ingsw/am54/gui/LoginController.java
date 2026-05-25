package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.database.DatabaseConfig;
import it.polimi.ingsw.am54.database.DatabaseManager;
import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import java.util.List;

public class LoginController {

    @FXML private StackPane rootPane;

    @FXML private VBox logoBox;
    @FXML private ImageView logoMesos;
    @FXML private VBox playStateBox;
    @FXML private VBox nicknameStateBox;

    @FXML private TextField serverIpField;
    @FXML private RadioButton socketRadio;
    @FXML private RadioButton rmiRadio;
    @FXML private ToggleGroup protocolGroup;
    @FXML private TextField nicknameField;
    @FXML private Label errorLabel;
    @FXML private Label clickToEnterLabel;

    @FXML private Button leaderboardButton;
    @FXML private StackPane leaderboardOverlay;
    @FXML private VBox leaderboardEntriesContainer;
    @FXML private Label leaderboardFooter;

    private boolean waitingForClick = false;
    private boolean isConnecting = false;

    private List<LeaderboardEntry> loadedEntries = null;
    private boolean leaderboardLoaded = false;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (rootPane != null) {
                logoMesos.fitWidthProperty().bind(rootPane.widthProperty().multiply(0.5));
                logoBox.translateYProperty().bind(rootPane.heightProperty().multiply(-0.33));
                playStateBox.translateYProperty().bind(rootPane.heightProperty().multiply(0.33));
            }
        });

        Client.getInstance().getModel().setOnModelUpdatedCallback(() -> {
            Platform.runLater(this::checkServerResponse);
        });

        loadLeaderboardInBackground();
    }

    private void loadLeaderboardInBackground() {
        new Thread(() -> {
            try {
                DatabaseConfig config = new DatabaseConfig();
                DatabaseManager db = new DatabaseManager(config);
                db.init();

                List<LeaderboardEntry> allEntries = new java.util.ArrayList<>();
                for (int np = 2; np <= 5; np++) {
                    allEntries.addAll(db.getLeaderboard(np));
                }

                allEntries.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

                Platform.runLater(() -> {
                    loadedEntries = allEntries;
                    leaderboardLoaded = true;
                });
            } catch (Exception e) {
                // Silently fail
            }
        }).start();
    }

    @FXML
    private void handleLeaderboardButton() {
        if (leaderboardOverlay == null) return;

        if (!leaderboardLoaded || loadedEntries == null || loadedEntries.isEmpty()) {
            leaderboardFooter.setText("CLASSIFICA IN CARICAMENTO O NON DISPONIBILE");
        } else {
            buildLeaderboardUI();
        }
        leaderboardOverlay.setVisible(true);
        leaderboardOverlay.setManaged(true);
    }

    @FXML
    private void handleOverlayClose() {
        if (leaderboardOverlay != null) {
            leaderboardOverlay.setVisible(false);
            leaderboardOverlay.setManaged(false);
        }
    }

    @FXML
    private void handlePanelClick(MouseEvent event) {
        event.consume();
    }

    private void buildLeaderboardUI() {
        if (leaderboardEntriesContainer == null || loadedEntries == null) return;

        leaderboardEntriesContainer.getChildren().clear();
        leaderboardFooter.setText("TOTALE PARTITE ARCHIVIATE: " + loadedEntries.size());

        int shown = Math.min(loadedEntries.size(), 50); // Mostriamo top 50
        for (int i = 0; i < shown; i++) {
            LeaderboardEntry e = loadedEntries.get(i);
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("leaderboard-entry-row");

            Label rankLabel = new Label("#" + (i + 1));
            rankLabel.getStyleClass().add("leaderboard-entry-rank");
            rankLabel.setPrefWidth(50);

            Label nameLabel = new Label(e.getNickname().toUpperCase());
            nameLabel.getStyleClass().add("leaderboard-entry-name");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            nameLabel.setMaxWidth(Double.MAX_VALUE);

            Label scoreLabel = new Label(e.getScore() + " ✨");
            scoreLabel.getStyleClass().add("leaderboard-entry-score");
            scoreLabel.setPrefWidth(80);

            Label modeLabel = new Label(e.getNumPlayers() + "P");
            modeLabel.getStyleClass().add("leaderboard-entry-mode");
            modeLabel.setPrefWidth(40);

            row.getChildren().addAll(rankLabel, nameLabel, scoreLabel, modeLabel);
            leaderboardEntriesContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handlePlayAction() {
        logoBox.setVisible(false);
        logoBox.setManaged(false);
        playStateBox.setVisible(false);
        playStateBox.setManaged(false);

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

        Thread connectionThread = new Thread(() -> {
            try {
                ServerConnection connection;
                ClientModel model = Client.getInstance().getModel();
                Platform.runLater(model::clearError);

                if (socketRadio.isSelected()) {
                    connection = new SocketServerConnection(ip, 12345, model);
                } else {
                    try (java.net.DatagramSocket s = new java.net.DatagramSocket()) {
                        s.connect(java.net.InetAddress.getByName(ip), 1099);
                        String myClientIp = s.getLocalAddress().getHostAddress();
                        System.setProperty("java.rmi.server.hostname", myClientIp);
                    } catch (Exception ex) {}

                    java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.getRegistry(ip, 1099);
                    RMIGameServer serverStub = (RMIGameServer) registry.lookup("AM54-Server");
                    connection = new RMIServerConnection(serverStub, model);
                }

                Client.getInstance().setConnection(connection);
                Client.getInstance().setNickname(nick);
                connection.joinGame(nick);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorLabel.setText("ERRORE: " + e.getMessage());
                    isConnecting = false;
                });
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void checkServerResponse() {
        if (!isConnecting) return;

        ClientModel model = Client.getInstance().getModel();
        String myNick = Client.getInstance().getNickname();

        if (model.getLastErrorMessage() != null) {
            errorLabel.setText(model.getLastErrorMessage().toUpperCase());
            model.clearError();
            Client.getInstance().setConnection(null);
            Client.getInstance().setNickname(null);
            isConnecting = false;
            nicknameField.selectAll();
            nicknameField.requestFocus();
        } else if (model.getPlayers().contains(myNick)) {
            errorLabel.setText("");
            isConnecting = false;

            // FONDAMENTALE: Nasconde il pulsante classifica quando si è collegati!
            if (leaderboardButton != null) {
                leaderboardButton.setVisible(false);
                leaderboardButton.setManaged(false);
            }

            nicknameStateBox.setVisible(false);
            nicknameStateBox.setManaged(false);
            logoBox.setVisible(true);
            logoBox.setManaged(true);
            playStateBox.setVisible(false);
            playStateBox.setManaged(false);

            clickToEnterLabel.setVisible(true);
            clickToEnterLabel.setManaged(true);
            waitingForClick = true;
        }
    }

    @FXML
    private void handleRootClick() {
        if (waitingForClick) {
            waitingForClick = false;
            logoBox.setVisible(false);
            logoBox.setManaged(false);
            clickToEnterLabel.setVisible(false);
            clickToEnterLabel.setManaged(false);
            startCinematicTransition();
        }
    }

    private void startCinematicTransition() {
        double screenW = rootPane.getScene().getWidth();
        double screenH = rootPane.getScene().getHeight();
        double imageW = 2752.0;
        double imageH = 1536.0;
        double caveX_img = 1441.0;
        double caveY_img = 985.0;

        double coverScale = Math.max(screenW / imageW, screenH / imageH);
        double caveX_screen = caveX_img * coverScale;
        double caveY_screen = caveY_img * coverScale;
        double centerX = screenW / 2.0;
        double centerY = screenH / 2.0;

        double dx = caveX_screen - centerX;
        double dy = caveY_screen - centerY;
        double zoomTarget = 6.0;
        double panX = -dx * zoomTarget;
        double panY = -dy * zoomTarget;

        GUIApp.playEntranceAnimation(panX, panY, () -> {
            try {
                String currentState = Client.getInstance().getModel().getCurrentState();
                boolean isReconnectingMidGame = currentState != null &&
                        (currentState.toUpperCase().contains("STARTTURN") ||
                                currentState.toUpperCase().contains("PICKING") ||
                                currentState.toUpperCase().contains("EVENT"));

                if (isReconnectingMidGame) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Board.fxml"));
                    Parent boardRoot = loader.load();
                    GUIApp.changeBackground("board-background");
                    GUIApp.changeSceneContent(boardRoot);
                } else {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Lobby.fxml"));
                    Parent lobbyRoot = loader.load();
                    GUIApp.changeBackground("lobby-background");
                    GUIApp.changeSceneContent(lobbyRoot);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}