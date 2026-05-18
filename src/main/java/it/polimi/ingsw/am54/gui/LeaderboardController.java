package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import it.polimi.ingsw.am54.model.TotemColors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaderboardController {

    @FXML private StackPane rootPane;
    @FXML private AnchorPane mainArea;

    @FXML private VBox podium1, podium2, podium3;
    @FXML private ImageView totem1, totem2, totem3;
    @FXML private Label name1, name2, name3;

    @FXML private VBox rankingTable;

    @FXML private Label playerPositionLabel;

    private final double NATIVE_WIDTH = 1920.0;
    private final double NATIVE_HEIGHT = 1080.0;

    // Classe locale per gestire i dati della partita appena conclusa
    private static class PlayerStats {
        String name; TotemColors color;
        int aura, food, magic, tribeCards, buildCards;
    }

    @FXML
    public void initialize() {
        setupScaling();

        Platform.runLater(this::buildLeaderboard);
    }

    private void setupScaling() {
        Scale scaleTransform = new Scale(1, 1, 0, 0);
        mainArea.getTransforms().add(scaleTransform);

        rootPane.widthProperty().addListener((obs, oldV, newV) -> updateScale(scaleTransform));
        rootPane.heightProperty().addListener((obs, oldV, newV) -> updateScale(scaleTransform));

        Platform.runLater(() -> updateScale(scaleTransform));
    }

    private void updateScale(Scale scaleTransform) {
        double screenW = rootPane.getWidth();
        double screenH = rootPane.getHeight();
        if (screenW <= 0 || screenH <= 0) return;

        double factor = Math.min(screenW / NATIVE_WIDTH, screenH / NATIVE_HEIGHT);
        scaleTransform.setX(factor);
        scaleTransform.setY(factor);

        double scaledW = NATIVE_WIDTH * factor;
        double scaledH = NATIVE_HEIGHT * factor;
        mainArea.setTranslateX((screenW - scaledW) / 2.0);
        mainArea.setTranslateY((screenH - scaledH) / 2.0);
    }

    private void buildLeaderboard() {
        ClientModel model = Client.getInstance().getModel();
        if (model == null) return;

        // 1. Podio: usa i dati della partita appena conclusa
        buildPodiumFromLocalData(model);

        // 2. Classifica storica dal server
        buildHistoricalLeaderboard(model);

        // 3. Posizione del giocatore
        updatePlayerPositionLabel(model);
    }

    private void buildPodiumFromLocalData(ClientModel model) {
        if (model.getPlayers() == null) return;

        List<PlayerStats> statsList = new ArrayList<>();

        for (String pName : model.getPlayers()) {
            PlayerStats ps = new PlayerStats();
            ps.name = pName;

            // Colore Totem (con fallback sicuro)
            ps.color = model.getPlayerColors().get(pName);
            if (ps.color == null) {
                for (Map.Entry<String, TotemColors> entry : model.getPlayerColors().entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(pName)) { ps.color = entry.getValue(); break; }
                }
            }

            // Punti base
            ps.aura = model.getPlayerAura().getOrDefault(pName, 0);
            ps.food = model.getPlayerFood().getOrDefault(pName, 0);

            // Punti magia
            ps.magic = 0;
            try {
                if (model.getPlayerMagic() != null) ps.magic = model.getPlayerMagic().getOrDefault(pName, 0);
            } catch (Exception ignored) {}

            // Conteggio carte
            List<String> tribe = model.getPlayerTribe().get(pName);
            ps.tribeCards = (tribe != null) ? tribe.size() : 0;

            List<String> builds = model.getPlayerBuildings().get(pName);
            ps.buildCards = (builds != null) ? builds.size() : 0;

            statsList.add(ps);
        }

        // Ordinamento: Aura > Cibo > Nome
        statsList.sort((p1, p2) -> {
            if (p1.aura != p2.aura) return Integer.compare(p2.aura, p1.aura);
            if (p1.food != p2.food) return Integer.compare(p2.food, p1.food);
            return p1.name.compareToIgnoreCase(p2.name);
        });

        // Popola il Podio
        if (statsList.size() > 0) setupPodiumPlace(podium1, totem1, name1, statsList.get(0));
        if (statsList.size() > 1) setupPodiumPlace(podium2, totem2, name2, statsList.get(1));
        if (statsList.size() > 2) setupPodiumPlace(podium3, totem3, name3, statsList.get(2));
    }

    private void buildHistoricalLeaderboard(ClientModel model) {
        // Pulisce la tabella precedente
        rankingTable.getChildren().clear();

        List<LeaderboardEntry> entries = model.getLeaderboardEntries();
        if (entries == null || entries.isEmpty()) {
            Label noData = new Label("(Nessuna classifica disponibile)");
            noData.setTextFill(Color.WHITE);
            noData.setStyle("-fx-font-size: 18px;");
            rankingTable.getChildren().add(noData);
            return;
        }

        // Intestazione tabella
        rankingTable.getChildren().add(createTableHeader());

        for (LeaderboardEntry entry : entries) {
            HBox row = createLeaderboardRow(entry);
            rankingTable.getChildren().add(row);
        }
    }

    private HBox createTableHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 8 0 8 0; -fx-border-color: transparent transparent rgba(255,255,255,0.5) transparent;");

        Label lblPos = new Label("Pos");
        lblPos.setPrefWidth(60);
        lblPos.setTextFill(Color.GOLD);
        lblPos.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label lblName = new Label("Giocatore");
        lblName.setPrefWidth(300);
        lblName.setTextFill(Color.GOLD);
        lblName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label lblScore = new Label("Punti");
        lblScore.setPrefWidth(120);
        lblScore.setTextFill(Color.GOLD);
        lblScore.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label lblDate = new Label("Data");
        lblDate.setPrefWidth(200);
        lblDate.setTextFill(Color.GOLD);
        lblDate.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label lblPlayers = new Label("Giocatori");
        lblPlayers.setPrefWidth(120);
        lblPlayers.setTextFill(Color.GOLD);
        lblPlayers.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        header.getChildren().addAll(lblPos, lblName, lblScore, lblDate, lblPlayers);
        return header;
    }

    private HBox createLeaderboardRow(LeaderboardEntry entry) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 0 8 0; -fx-border-color: transparent transparent rgba(255,255,255,0.15) transparent;");

        String myName = Client.getInstance().getNickname();
        boolean isMe = myName != null && myName.equalsIgnoreCase(entry.getNickname());

        // POS
        Label lblPos = new Label(entry.getPosition() + "°");
        lblPos.setPrefWidth(60);
        lblPos.setTextFill(isMe ? Color.GOLD : Color.WHITE);
        lblPos.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // NICKNAME
        Label lblName = new Label(entry.getNickname().toUpperCase());
        lblName.setPrefWidth(300);
        lblName.setTextFill(isMe ? Color.GOLD : Color.WHITE);
        lblName.setStyle("-fx-font-size: 18px;" + (isMe ? " -fx-font-weight: bold;" : ""));

        // SCORE
        Label lblScore = new Label(String.valueOf(entry.getScore()));
        lblScore.setPrefWidth(120);
        lblScore.setTextFill(isMe ? Color.GOLD : Color.WHITE);
        lblScore.setStyle("-fx-font-size: 18px;");

        // DATE
        String dateStr = entry.getMatchDate() != null && entry.getMatchDate().length() >= 10
                ? entry.getMatchDate().substring(0, 10) : "";
        Label lblDate = new Label(dateStr);
        lblDate.setPrefWidth(200);
        lblDate.setTextFill(Color.WHITE);
        lblDate.setStyle("-fx-font-size: 18px;");

        // NUM PLAYERS
        Label lblPlayers = new Label(String.valueOf(entry.getNumPlayers()));
        lblPlayers.setPrefWidth(120);
        lblPlayers.setTextFill(Color.WHITE);
        lblPlayers.setStyle("-fx-font-size: 18px;");

        row.getChildren().addAll(lblPos, lblName, lblScore, lblDate, lblPlayers);
        return row;
    }

    private void updatePlayerPositionLabel(ClientModel model) {
        int pos = model.getLeaderboardPosition();
        int numPlayers = model.getLeaderboardNumPlayers();
        if (pos > 0 && playerPositionLabel != null) {
            playerPositionLabel.setText("La tua posizione in classifica (partite da " + numPlayers + " giocatori): #" + pos);
            playerPositionLabel.setVisible(true);
        } else if (playerPositionLabel != null) {
            playerPositionLabel.setVisible(false);
        }
    }

    private void setupPodiumPlace(VBox podiumBox, ImageView totemView, Label nameLabel, PlayerStats p) {
        podiumBox.setVisible(true);

        // Ridimensionamento testo intelligente se il nome è troppo lungo
        String dispName = p.name.toUpperCase();
        nameLabel.setText(dispName);
        if (dispName.length() > 10) {
            nameLabel.setStyle(nameLabel.getStyle() + " -fx-font-size: " + (36 - (dispName.length() - 10)) + "px;");
        }

        if (p.color != null) {
            String cName = p.color.name().toLowerCase();
            try {
                InputStream is = getClass().getResourceAsStream("/images/totem_front_" + cName + ".png");
                if (is != null) {
                    totemView.setImage(new Image(is));
                    totemView.getStyleClass().add("totem-selected-" + cName);
                }
            } catch (Exception e) {
                System.err.println("Errore caricamento totem front per podio: " + cName);
            }
        }
    }

    @FXML
    private void handleReturnToMenu() {
        try {
            if (Client.getInstance().getConnection() != null) {
                Client.getInstance().setConnection(null);
            }
            Client.getInstance().setNickname(null);
            GUIApp.loadLoginScene();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
