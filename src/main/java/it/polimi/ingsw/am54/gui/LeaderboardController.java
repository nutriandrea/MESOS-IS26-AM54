package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
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
import javafx.scene.text.Font;

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
        if (model == null || model.getPlayers() == null) return;

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
            if (model.getLastMatchScores() != null && model.getLastMatchScores().containsKey(pName)) {
                ps.aura = model.getLastMatchScores().get(pName);
            } else {
                ps.aura = model.getPlayerAura().getOrDefault(pName, 0); // Fallback di sicurezza
            }

            ps.food = model.getPlayerFood().getOrDefault(pName, 0);

            // Punti magia (gestiti con sicurezza)
            ps.magic = 0;
            try {
                if (model.getPlayerMagic() != null) ps.magic = model.getPlayerMagic().getOrDefault(pName, 0);
            } catch (Exception e) {}

            // Conteggio carte
            List<String> tribe = model.getPlayerTribe().get(pName);
            ps.tribeCards = (tribe != null) ? tribe.size() : 0;

            List<String> builds = model.getPlayerBuildings().get(pName);
            ps.buildCards = (builds != null) ? builds.size() : 0;

            statsList.add(ps);
        }

        // ==============================================================
        // ORDINAMENTO LOGICO:
        // 1° Criterio: Aura (Maggiore)
        // 2° Criterio: Cibo (Maggiore)
        // 3° Criterio: Ordine Alfabetico del Nome
        // ==============================================================
        statsList.sort((p1, p2) -> {
            if (p1.aura != p2.aura) return Integer.compare(p2.aura, p1.aura);
            if (p1.food != p2.food) return Integer.compare(p2.food, p1.food);
            return p1.name.compareToIgnoreCase(p2.name);
        });

        // Popola il Podio
        if (statsList.size() > 0) setupPodiumPlace(podium1, totem1, name1, statsList.get(0));
        if (statsList.size() > 1) setupPodiumPlace(podium2, totem2, name2, statsList.get(1));
        if (statsList.size() > 2) setupPodiumPlace(podium3, totem3, name3, statsList.get(2));

        // Popola la Tabella Generale
        for (int i = 0; i < statsList.size(); i++) {
            PlayerStats p = statsList.get(i);
            HBox row = createRankingRow(i + 1, p);
            rankingTable.getChildren().add(row);
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
                // Carichiamo l'immagine totem_front_COLORE.png
                InputStream is = getClass().getResourceAsStream("/images/totem_front_" + cName + ".png");
                if (is != null) {
                    totemView.setImage(new Image(is));
                    // Aggiungiamo la classe CSS della lobby per l'illuminazione (glow effect)
                    totemView.getStyleClass().add("totem-selected-" + cName);
                }
            } catch (Exception e) {
                System.err.println("Errore caricamento totem front per podio: " + cName);
            }
        }
    }

    private HBox createRankingRow(int position, PlayerStats p) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: transparent transparent rgba(255,255,255,0.2) transparent;");

        // POS
        Label lblPos = new Label(position + "°");
        lblPos.setPrefWidth(60);
        lblPos.setTextFill(Color.WHITE);
        lblPos.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // TOTEM ICON (Piccolo)
        ImageView iconView = new ImageView();
        iconView.setFitHeight(40);
        iconView.setFitWidth(40);
        iconView.setPreserveRatio(true);
        if (p.color != null) {
            String cName = p.color.name().toLowerCase();
            try {
                InputStream is = getClass().getResourceAsStream("/images/totem_front_" + cName + ".png");
                if (is != null) iconView.setImage(new Image(is));
            } catch (Exception e) {}
        }
        HBox iconBox = new HBox(iconView);
        iconBox.setPrefWidth(80);
        iconBox.setAlignment(Pos.CENTER_LEFT);

        // NICKNAME
        Label lblName = new Label(p.name.toUpperCase());
        lblName.setPrefWidth(300);
        lblName.setTextFill(Color.WHITE);
        lblName.setStyle("-fx-font-size: 20px;");

        // STATS (Larghezza 120px per combaciare con l'header)
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
        l.setPrefWidth(120); // Questo deve essere identico al prefWidth dell'FXML (120)
        l.setAlignment(Pos.CENTER); // <-- AGGIUNGI QUESTA RIGA per centrare il testo
        l.getStyleClass().add("ranking-table-value"); // Applica il CSS per il font Herculanum e colore
        return l;
    }

    @FXML
    private void handleReturnToMenu() {
        try {
            // Reset completo e profondo del client, cancellando vecchi player e stati
            Client.resetClient();

            // Ritorno alla schermata di login
            GUIApp.loadLoginScene();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}