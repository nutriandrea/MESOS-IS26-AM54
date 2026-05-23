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
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
    @FXML private VBox leaderboardOverlay;
    @FXML private VBox leaderboardEntriesContainer;
    @FXML private Label leaderboardFooter;

    // NOTA: Abbiamo rimosso la MediaView perché non usiamo più i video!

    private boolean waitingForClick = false;
    private boolean isConnecting = false;

    private List<LeaderboardEntry> loadedEntries = null;
    private boolean leaderboardLoaded = false;

    @FXML
    public void initialize() {
        // Risolve il bug del layout al rientro eseguendo i calcoli solo a dimensioni consolidate
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

        // Carica la classifica dal database cloud in background
        loadLeaderboardInBackground();
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

        // BEST PRACTICE JAVAFX: Spostare il lavoro di rete su un thread demone
        Thread connectionThread = new Thread(() -> {
            try {
                ServerConnection connection;
                ClientModel model = Client.getInstance().getModel();

                Platform.runLater(model::clearError);

                if (socketRadio.isSelected()) {
                    connection = new SocketServerConnection(ip, 12345, model);
                } else {
                    // 1. Il Client usa l'IP digitato nella GUI per autoconfigurare RMI
                    try (java.net.DatagramSocket s = new java.net.DatagramSocket()) {
                        s.connect(java.net.InetAddress.getByName(ip), 1099);
                        String myClientIp = s.getLocalAddress().getHostAddress();
                        System.setProperty("java.rmi.server.hostname", myClientIp);
                        System.out.println("[Client RMI] Hostname impostato a: " + myClientIp);
                    } catch (Exception ex) {
                        System.err.println("Impossibile determinare l'IP del client. Fallback.");
                    }

                    // 2. Connessione effettiva al Server
                    java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.getRegistry(ip, 1099);
                    RMIGameServer serverStub = (RMIGameServer) registry.lookup("AM54-Server");
                    connection = new RMIServerConnection(serverStub, model);
                }

                Client.getInstance().setConnection(connection);
                Client.getInstance().setNickname(nick);

                // Questa chiamata RMI ora bloccherà solo QUESTO thread, lasciando libera la grafica!
                connection.joinGame(nick);

            } catch (Exception e) {
                // Se fallisce, torniamo sul Thread della grafica per mostrare l'errore
                Platform.runLater(() -> {
                    errorLabel.setText("ERRORE: " + e.getMessage());
                    isConnecting = false;
                });
            }
        });

        connectionThread.setDaemon(true); // Se chiudi la finestra, il thread muore con essa
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

            // Nascondiamo l'interfaccia
            logoBox.setVisible(false);
            logoBox.setManaged(false);
            clickToEnterLabel.setVisible(false);
            clickToEnterLabel.setManaged(false);

            // Facciamo partire la nuova transizione nativa!
            startCinematicTransition();
        }
    }

    /**
     * Carica la classifica dal database MySQL remoto in un thread separato.
     * I dati vengono memorizzati e mostrati solo quando l'utente clicca il bottone.
     */
    private void loadLeaderboardInBackground() {
        new Thread(() -> {
            try {
                DatabaseConfig config = new DatabaseConfig();
                DatabaseManager db = new DatabaseManager(config);
                db.init();

                List<LeaderboardEntry> allEntries = new java.util.ArrayList<>();
                for (int np = 2; np <= 5; np++)
                    allEntries.addAll(db.getLeaderboard(np));

                allEntries.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

                Platform.runLater(() -> {
                    loadedEntries = allEntries;
                    leaderboardLoaded = true;
                });
            } catch (Exception e) {
                // Silently fail — l'utente vedrà "Classifica non disponibile" se clicca
            }
        }).start();
    }

    @FXML
    private void handleLeaderboardButton() {
        if (leaderboardOverlay == null) return;

        if (!leaderboardLoaded || loadedEntries == null || loadedEntries.isEmpty()) {
            leaderboardFooter.setText("CLASSIFICA NON DISPONIBILE");
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

    /**
     * Costruisce l'UI della classifica all'interno dell'overlay.
     */
    private void buildLeaderboardUI() {
        if (leaderboardEntriesContainer == null || loadedEntries == null) return;

        leaderboardEntriesContainer.getChildren().clear();
        leaderboardFooter.setText("TOTALE: " + loadedEntries.size() + " PARTITE");

        int shown = Math.min(loadedEntries.size(), 20);
        for (int i = 0; i < shown; i++) {
            LeaderboardEntry e = loadedEntries.get(i);
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);

            String rowClass = (i % 2 == 0) ? "leaderboard-row-even" : "leaderboard-row-odd";
            row.getStyleClass().addAll("leaderboard-row", rowClass);

            // Rank badge
            Label rankLabel = new Label("#" + e.getPosition());
            rankLabel.getStyleClass().add("leaderboard-rank");
            if (e.getPosition() == 1) rankLabel.getStyleClass().add("leaderboard-rank-top1");
            else if (e.getPosition() == 2) rankLabel.getStyleClass().add("leaderboard-rank-top2");
            else if (e.getPosition() == 3) rankLabel.getStyleClass().add("leaderboard-rank-top3");

            // Player name (takes remaining space)
            Label nameLabel = new Label(e.getNickname());
            nameLabel.getStyleClass().add("leaderboard-name");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            // Score
            Label scoreLabel = new Label(e.getScore() + " pts");
            scoreLabel.getStyleClass().add("leaderboard-score");

            // Player count badge
            Label modeLabel = new Label(e.getNumPlayers() + "p");
            modeLabel.getStyleClass().add("leaderboard-mode");

            row.getChildren().addAll(rankLabel, nameLabel, scoreLabel, modeLabel);
            leaderboardEntriesContainer.getChildren().add(row);
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Lobby.fxml"));
                Parent lobbyRoot = loader.load();

                GUIApp.changeBackground("lobby-background");
                GUIApp.changeSceneContent(lobbyRoot);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}