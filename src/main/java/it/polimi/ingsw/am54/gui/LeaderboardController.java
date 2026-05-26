package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import it.polimi.ingsw.am54.model.TotemColors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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

    // --- VARIABILI PER L'OVERLAY DELLA CLASSIFICA GLOBALE (Copia dal Login) ---
    @FXML private StackPane leaderboardOverlay;
    @FXML private VBox leaderboardEntriesContainer;
    @FXML private Label leaderboardFooter;

    private final double NATIVE_WIDTH = 1672.0;
    private final double NATIVE_HEIGHT = 941.0;

    // Classe locale per gestire l'ordinamento facilmente
    private class PlayerStats {
        String name; TotemColors color;
        int aura, food, magic, tribeCards, buildCards;
    }

    @FXML
    public void initialize() {
        setupScaling();

        // 1. NON DISCONNETTIAMO LA RETE SUBITO! Ci serve per richiedere la classifica.
        // Se c'è una connessione, la manteniamo aperta per il pulsante globale.

        // 2. Registriamo il Listener per quando il Server ci manda i dati della Classifica Globale
        if (Client.getInstance().getModel() != null) {
            Client.getInstance().getModel().setOnModelUpdatedCallback(() -> {
                Platform.runLater(() -> {
                    // Costruiamo la tabella della partita corrente
                    buildLeaderboard();
                    // Proviamo ad aggiornare l'overlay globale se ci sono dati
                    updateGlobalLeaderboardUI();
                });
            });
        }

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

    /* =====================================================================
     * LOGICA PER LA CLASSIFICA DELLA PARTITA CORRENTE
     * ===================================================================== */

    private void buildLeaderboard() {
        ClientModel model = Client.getInstance().getModel();
        if (model == null || model.getPlayers() == null) return;

        // Evitiamo di ridisegnare la tabella se è già stata popolata
        if (!rankingTable.getChildren().isEmpty()) return;

        List<PlayerStats> statsList = new ArrayList<>();

        for (String pName : model.getPlayers()) {
            PlayerStats ps = new PlayerStats();
            ps.name = pName;

            ps.color = model.getPlayerColors().get(pName);
            if (ps.color == null) {
                for (Map.Entry<String, TotemColors> entry : model.getPlayerColors().entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(pName)) { ps.color = entry.getValue(); break; }
                }
            }

            if (model.getLastMatchScores() != null && model.getLastMatchScores().containsKey(pName)) {
                ps.aura = model.getLastMatchScores().get(pName);
            } else {
                ps.aura = model.getPlayerAura().getOrDefault(pName, 0);
            }

            ps.food = model.getPlayerFood().getOrDefault(pName, 0);

            ps.magic = 0;
            try {
                if (model.getPlayerMagic() != null) ps.magic = model.getPlayerMagic().getOrDefault(pName, 0);
            } catch (Exception e) {}

            List<String> tribe = model.getPlayerTribe().get(pName);
            ps.tribeCards = (tribe != null) ? tribe.size() : 0;

            List<String> builds = model.getPlayerBuildings().get(pName);
            ps.buildCards = (builds != null) ? builds.size() : 0;

            statsList.add(ps);
        }

        statsList.sort((p1, p2) -> {
            if (p1.aura != p2.aura) return Integer.compare(p2.aura, p1.aura);
            if (p1.food != p2.food) return Integer.compare(p2.food, p1.food);
            return p1.name.compareToIgnoreCase(p2.name);
        });

        if (statsList.size() > 0) setupPodiumPlace(podium1, totem1, name1, statsList.get(0));
        if (statsList.size() > 1) setupPodiumPlace(podium2, totem2, name2, statsList.get(1));
        if (statsList.size() > 2) setupPodiumPlace(podium3, totem3, name3, statsList.get(2));

        for (int i = 0; i < statsList.size(); i++) {
            PlayerStats p = statsList.get(i);
            HBox row = createRankingRow(i + 1, p);
            rankingTable.getChildren().add(row);
        }
    }

    private void setupPodiumPlace(VBox podiumBox, ImageView totemView, Label nameLabel, PlayerStats p) {
        podiumBox.setVisible(true);

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
            } catch (Exception e) {}
        }
    }

    private HBox createRankingRow(int position, PlayerStats p) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: transparent transparent rgba(255,255,255,0.2) transparent;");

        Label lblPos = new Label(position + "°");
        lblPos.setPrefWidth(60);
        lblPos.setTextFill(Color.WHITE);
        lblPos.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Herculanum';");

        ImageView iconView = new ImageView();
        iconView.setFitHeight(40);
        iconView.setFitWidth(40);
        iconView.setPreserveRatio(true);
        if (p.color != null) {
            try {
                InputStream is = getClass().getResourceAsStream("/images/totem_front_" + p.color.name().toLowerCase() + ".png");
                if (is != null) iconView.setImage(new Image(is));
            } catch (Exception e) {}
        }
        HBox iconBox = new HBox(iconView);
        iconBox.setPrefWidth(80);
        iconBox.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(p.name.toUpperCase());
        lblName.setPrefWidth(300);
        lblName.setTextFill(Color.WHITE);
        lblName.setStyle("-fx-font-size: 20px; -fx-font-family: 'Herculanum';");

        Label lblAura = createStatLabel(String.valueOf(p.aura));
        Label lblFood = createStatLabel(String.valueOf(p.food));
        Label lblMagic = createStatLabel(String.valueOf(p.magic));
        Label lblTribe = createStatLabel(String.valueOf(p.tribeCards));
        Label lblBuild = createStatLabel(String.valueOf(p.buildCards));

        row.getChildren().addAll(lblPos, iconBox, lblName, lblAura, lblFood, lblMagic, lblTribe, lblBuild);
        return row;
    }

    private Label createStatLabel(String val) {
        Label l = new Label(val);
        l.setPrefWidth(120);
        l.setAlignment(Pos.CENTER);
        l.getStyleClass().add("ranking-table-value");
        return l;
    }

    /* =====================================================================
     * LOGICA PER L'OVERLAY DELLA CLASSIFICA GLOBALE
     * ===================================================================== */

    @FXML
    private void handleGlobalLeaderboardButton() {
        if (leaderboardOverlay == null) return;

        // Mostriamo l'overlay con una scritta di attesa
        if (leaderboardFooter != null) leaderboardFooter.setText("CARICAMENTO IN CORSO...");
        leaderboardOverlay.setVisible(true);
        leaderboardOverlay.setManaged(true);

        // Richiediamo al server la classifica filtrata per il numero di giocatori attuale
        if (Client.getInstance().getConnection() != null && Client.getInstance().getModel() != null) {
            int numPlayers = Client.getInstance().getModel().getPlayers().size();
            Client.getInstance().getConnection().requestLeaderboard(numPlayers);
        } else {
            if (leaderboardFooter != null) leaderboardFooter.setText("CONNESSIONE SERVER ASSENTE");
        }
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
        // Intercetta i click sul pannello interno per non far chiudere l'overlay
        event.consume();
    }

    private void updateGlobalLeaderboardUI() {
        if (leaderboardEntriesContainer == null || leaderboardOverlay == null || !leaderboardOverlay.isVisible()) return;

        ClientModel model = Client.getInstance().getModel();

        java.util.List<LeaderboardEntry> entries = new java.util.ArrayList<>(model.getLeaderboardEntries());

        if (entries == null || entries.isEmpty()) {
            if (leaderboardFooter != null) leaderboardFooter.setText("NESSUNA PARTITA ARCHIVIATA");
            return;
        }

        entries.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        leaderboardEntriesContainer.getChildren().clear();

        if (leaderboardFooter != null) leaderboardFooter.setText("TOTALE PARTITE ARCHIVIATE: " + entries.size());

        int shown = Math.min(entries.size(), 50);
        for (int i = 0; i < shown; i++) {
            LeaderboardEntry e = entries.get(i);
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

            // ========================================================
            // NUOVO: RECUPERO E VISUALIZZAZIONE DATA DELLA PARTITA
            // ========================================================
            String fullDate = e.getMatchDate();
            String displayDate = (fullDate != null && fullDate.length() >= 10) ? fullDate.substring(0, 10) : "---";
            Label dateLabel = new Label(displayDate);
            dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.6); -fx-font-family: 'Herculanum';");
            dateLabel.setPrefWidth(120);
            dateLabel.setAlignment(Pos.CENTER_RIGHT);

            row.getChildren().addAll(rankLabel, nameLabel, scoreLabel, modeLabel, dateLabel);
            leaderboardEntriesContainer.getChildren().add(row);
        }
    }

    /* =====================================================================
     * RITORNO AL MENU E DISCONNESSIONE
     * ===================================================================== */

    @FXML
    private void handleReturnToMenu() {
        try {
            // La disconnessione della rete avviene SOLO in questo momento!
            if (Client.getInstance().getConnection() != null) {
                try {
                    Client.getInstance().getConnection().disconnect();
                } catch (Exception e) {
                    System.err.println("[CLIENT] Errore durante la disconnessione: " + e.getMessage());
                }
            }

            Client.resetClient();
            GUIApp.loadLoginScene();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}