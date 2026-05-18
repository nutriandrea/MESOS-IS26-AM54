package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.database.DatabaseConfig;
import it.polimi.ingsw.am54.database.DatabaseManager;
import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Comparator;
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

    // --- Leaderboard (login screen) ---
    @FXML private VBox leaderboardBox;
    @FXML private ScrollPane leaderboardScroll;
    @FXML private VBox leaderboardContent;
    @FXML private Label leaderboardFooter;

    // NOTA: Abbiamo rimosso la MediaView perché non usiamo più i video!

    private boolean waitingForClick = false;
    private boolean isConnecting = false;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            logoMesos.fitWidthProperty().bind(rootPane.widthProperty().multiply(0.5));
            logoBox.translateYProperty().bind(rootPane.heightProperty().multiply(-0.33));
            playStateBox.translateYProperty().bind(rootPane.heightProperty().multiply(0.33));
        });

        Client.getInstance().getModel().setOnModelUpdatedCallback(() -> {
            Platform.runLater(this::checkServerResponse);
        });

        // Carica la classifica dal database in background
        loadLeaderboardInBackground();
    }

    /**
     * Carica la classifica dal database MySQL in un thread separato.
     */
    private void loadLeaderboardInBackground() {
        leaderboardFooter.setText("Caricamento classifica...");
        new Thread(() -> {
            try {
                DatabaseConfig config = new DatabaseConfig();
                DatabaseManager db = new DatabaseManager(config);
                db.init();

                List<LeaderboardEntry> allEntries = new ArrayList<>();
                for (int np = 2; np <= 5; np++) {
                    allEntries.addAll(db.getLeaderboard(np));
                }

                if (allEntries.isEmpty()) {
                    Platform.runLater(() -> {
                        leaderboardContent.getChildren().clear();
                        leaderboardContent.getChildren().add(
                            new Label("(Nessuna partita registrata)")
                        );
                        leaderboardFooter.setText("Nessun dato disponibile");
                    });
                    return;
                }

                // Ordina per num_players ASC, score DESC
                allEntries.sort(Comparator
                    .comparingInt(LeaderboardEntry::getNumPlayers)
                    .thenComparingInt((LeaderboardEntry e) -> -e.getScore()));

                int total = allEntries.size();
                Platform.runLater(() -> buildLeaderboardUI(allEntries, total));

            } catch (Exception e) {
                System.err.println("[Login] Leaderboard DB error: " + e.getMessage());
                Platform.runLater(() -> {
                    leaderboardContent.getChildren().clear();
                    Label fallback = new Label("Connettiti al server\nper la classifica");
                    fallback.getStyleClass().add("leaderboard-empty");
                    leaderboardContent.getChildren().add(fallback);
                    leaderboardFooter.setText("Database non raggiungibile");
                });
            }
        }).start();
    }

    /**
     * Costruisce l'interfaccia della classifica con lo stile del gioco.
     */
    private void buildLeaderboardUI(List<LeaderboardEntry> entries, int total) {
        leaderboardContent.getChildren().clear();

        for (int np = 2; np <= 5; np++) {
            List<LeaderboardEntry> group = new ArrayList<>();
            for (LeaderboardEntry e : entries) {
                if (e.getNumPlayers() == np) group.add(e);
            }
            if (group.isEmpty()) continue;

            // Titolo del gruppo
            Label groupTitle = new Label("— " + np + " giocatori —");
            groupTitle.getStyleClass().add("leaderboard-group-title");
            leaderboardContent.getChildren().add(groupTitle);

            // Intestazione tabella
            Label header = new Label(String.format("%-4s %-16s %s", "Pos", "Nickname", "Punti"));
            header.getStyleClass().add("leaderboard-entry-header");
            leaderboardContent.getChildren().add(header);

            // Righe della classifica
            for (LeaderboardEntry e : group) {
                String nick = e.getNickname();
                if (nick.length() > 16) nick = nick.substring(0, 14) + "..";
                Label row = new Label(String.format(" %-3d %-16s %3d",
                    e.getPosition(), nick, e.getScore()));
                row.getStyleClass().add("leaderboard-entry-row");
                leaderboardContent.getChildren().add(row);
            }
        }

        leaderboardFooter.setText("Dati storici (" + total + " record)");
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

        try {
            ServerConnection connection;
            ClientModel model = Client.getInstance().getModel();

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