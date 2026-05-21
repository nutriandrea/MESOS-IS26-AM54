package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.gui.managers.MusicManager;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class LobbyController {

    @FXML private StackPane lobbyRoot;

    @FXML private VBox redBox;
    @FXML private VBox yellowBox;
    @FXML private VBox blueBox;
    @FXML private VBox blackBox;
    @FXML private VBox whiteBox;

    @FXML private ImageView totemRed;
    @FXML private ImageView totemYellow;
    @FXML private ImageView totemBlue;
    @FXML private ImageView totemBlack;
    @FXML private ImageView totemWhite;

    @FXML private Label nameRed;
    @FXML private Label nameYellow;
    @FXML private Label nameBlue;
    @FXML private Label nameBlack;
    @FXML private Label nameWhite;

    @FXML private Label subtitleLabel;
    @FXML private Button startButton;

    @FXML private Label welcomeLabel;

    @FXML private StackPane rulesOverlay;
    @FXML private ImageView leftPageImage;
    @FXML private ImageView rightPageImage;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageLabel;
    @FXML private Label infoLabel;
    @FXML private Button infoButton;
    @FXML private Button musicButton;
    @FXML private Label lobbyTitle;


    private Map<String, ImageView> totemMap;
    private Map<String, Label> labelMap;

    private int currentLeftPageIndex = 0;
    private final int TOTAL_PAGES = 8;
    private List<Image> rulePages = new ArrayList<>();

    private Timeline dotAnimation;
    private int dotCount = 0;
    private String baseWaitingText = "";
    private final String[] dotStates = {"", ".", "..", "..."};

    @FXML
    public void initialize() {
        double totemSizeScale = 0.23;
        totemRed.fitHeightProperty().bind(lobbyRoot.heightProperty().multiply(totemSizeScale));
        totemYellow.fitHeightProperty().bind(lobbyRoot.heightProperty().multiply(0.21));
        totemBlue.fitHeightProperty().bind(lobbyRoot.heightProperty().multiply(totemSizeScale));
        totemBlack.fitHeightProperty().bind(lobbyRoot.heightProperty().multiply(totemSizeScale));
        totemWhite.fitHeightProperty().bind(lobbyRoot.heightProperty().multiply(totemSizeScale));

        List<VBox> allBoxes = List.of(redBox, yellowBox, blueBox, blackBox, whiteBox);

        for (VBox box : allBoxes) {
            box.setSpacing(30);
            box.setOnMouseEntered(e -> box.toFront());
            box.setOnMouseExited(e -> bringMyTotemToFront());
        }

        redBox.translateXProperty().bind(lobbyRoot.widthProperty().multiply(-0.255));
        redBox.translateYProperty().bind(lobbyRoot.heightProperty().multiply(0.388).subtract(redBox.heightProperty().divide(2)));

        yellowBox.translateXProperty().bind(lobbyRoot.widthProperty().multiply(-0.127));
        yellowBox.translateYProperty().bind(lobbyRoot.heightProperty().multiply(0.292).subtract(yellowBox.heightProperty().divide(2)));

        blueBox.translateXProperty().bind(lobbyRoot.widthProperty().multiply(0.037));
        blueBox.translateYProperty().bind(lobbyRoot.heightProperty().multiply(0.240).subtract(blueBox.heightProperty().divide(2)));

        blackBox.translateXProperty().bind(lobbyRoot.widthProperty().multiply(0.180));
        blackBox.translateYProperty().bind(lobbyRoot.heightProperty().multiply(0.330).subtract(blackBox.heightProperty().divide(2)));

        whiteBox.translateXProperty().bind(lobbyRoot.widthProperty().multiply(0.279));
        whiteBox.translateYProperty().bind(lobbyRoot.heightProperty().multiply(0.385).subtract(whiteBox.heightProperty().divide(2)));

        totemMap = new HashMap<>();
        totemMap.put("red", totemRed);
        totemMap.put("yellow", totemYellow);
        totemMap.put("blue", totemBlue);
        totemMap.put("black", totemBlack);
        totemMap.put("white", totemWhite);

        labelMap = new HashMap<>();
        labelMap.put("red", nameRed);
        labelMap.put("yellow", nameYellow);
        labelMap.put("blue", nameBlue);
        labelMap.put("black", nameBlack);
        labelMap.put("white", nameWhite);

        loadImage(totemRed, "/images/totem_red_3d_160x270.png");
        loadImage(totemYellow, "/images/totem_yellow_3d_160x270.png");
        loadImage(totemBlue, "/images/totem_blue_3d_160x270.png");
        loadImage(totemBlack, "/images/totem_black_3d_160x270.png");
        loadImage(totemWhite, "/images/totem_white_3d_160x270.png");

        Client.getInstance().getModel().setOnModelUpdatedCallback(() -> {
            Platform.runLater(this::updateLobbyUI);
        });

        for (int i = 1; i <= TOTAL_PAGES; i++) {
            String path = "/images/rules/page_" + i + ".jpg";
            try { rulePages.add(new javafx.scene.image.Image(getClass().getResourceAsStream(path))); } catch (Exception e) {}
        }

        dotAnimation = new Timeline(new KeyFrame(Duration.millis(750), e -> {
            dotCount = (dotCount + 1) % 4;
            subtitleLabel.setText(baseWaitingText + dotStates[dotCount]);
        }));
        dotAnimation.setCycleCount(Animation.INDEFINITE);

        // Ridimensionamento fluido e proporzionale
        // Ridimensionamento fluido e proporzionale del blocco testuale superiore
        lobbyRoot.widthProperty().addListener((obs, oldVal, newVal) -> {
            double currentWidth = newVal.doubleValue();
            double factor = currentWidth / 1920.0;

            // 1. Welcome Label (Titolo principale)
            double targetWelcomeSize = Math.max(18.0, 52.0 * factor);
            // Inseriamo Font e Colore (da CSS) per evitare il flash Arial/Nero
            welcomeLabel.setStyle("-fx-font-size: " + targetWelcomeSize + "px; -fx-font-family: 'Herculanum'; -fx-text-fill: white;");

            // 2. Etichetta "Regole del gioco"
            double targetLabelSize = Math.max(16.0, 32.0 * factor);
            if (infoLabel != null) {
                infoLabel.setStyle("-fx-font-size: " + targetLabelSize + "px; -fx-font-family: 'Herculanum'; -fx-text-fill: white;");
            }

            // 3. Testo interno del pulsante informativo "i"
            double targetButtonSize = Math.max(16.0, 28.0 * factor);
            if (infoButton != null) {
                // Il bottone usa Arial e gold nel tuo CSS!
                infoButton.setStyle("-fx-font-size: " + targetButtonSize + "px; -fx-font-family: 'Arial'; -fx-text-fill: gold;");
            }
        });

        updateLobbyUI();
    }

    private void loadImage(ImageView imageView, String string) {
        try {
            URL url = getClass().getResource(string);
            if (url != null) imageView.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {}
    }

    private void updateLobbyUI() {
        ClientModel model = Client.getInstance().getModel();
        String myNickname = Client.getInstance().getNickname();

        for (String colorKey : labelMap.keySet()) {
            labelMap.get(colorKey).setText("");
        }

        for (ImageView imageView : totemMap.values()) {
            imageView.getStyleClass().removeAll("totem-selected-red", "totem-selected-yellow", "totem-selected-blue", "totem-selected-black", "totem-selected-white");
            imageView.setOpacity(1.0);
            imageView.setDisable(false);
        }

        Map<String, TotemColors> playerColors = model.getPlayerColors();
        List<String> players = model.getPlayers();

        boolean hasChosenTotem = playerColors != null && playerColors.containsKey(myNickname);
        boolean isHost = players != null && !players.isEmpty() && players.get(0).equals(myNickname);
        int playerCount = players != null ? players.size() : 0;

        // LA CORREZIONE DEL BUG: Verifica che TUTTI i player loggati abbiano inserito un colore
        boolean everyoneHasChosen = playerColors != null && players != null && playerColors.size() == players.size();

        if (!hasChosenTotem) {
            setSubtitleText("SCEGLI IL TUO TOTEM!", false);
            startButton.setVisible(false);
            startButton.setManaged(false);
        } else {
            if (isHost) {
                // IL PULSANTE APPARE SOLO SE CI SONO ALMENO 2 STRUTTURE E TUTTI HANNO SCELTO
                if (playerCount > 1 && everyoneHasChosen) {
                    setSubtitleText("Premi Start quando vuoi iniziare la partita!", false);
                    startButton.setVisible(true);
                    startButton.setManaged(true);
                } else if (playerCount > 1) {
                    setSubtitleText("In attesa che gli altri scelgano il totem", true);
                    startButton.setVisible(false);
                    startButton.setManaged(false);
                } else {
                    setSubtitleText("In attesa di altri giocatori", true);
                    startButton.setVisible(false);
                    startButton.setManaged(false);
                }
            } else {
                if (everyoneHasChosen) {
                    setSubtitleText("In attesa che l'host avvii la partita", true);
                } else {
                    setSubtitleText("In attesa che tutti scelgano il totem", true);
                }
                startButton.setVisible(false);
                startButton.setManaged(false);
            }
        }

        if (playerColors == null) return;

        for (Map.Entry<String, TotemColors> entry : playerColors.entrySet()) {
            String player = entry.getKey();
            String color = entry.getValue().name().toLowerCase();

            if (labelMap.containsKey(color) && totemMap.containsKey(color)) {
                Label label = labelMap.get(color);
                ImageView totem = totemMap.get(color);
                label.setText(player.toUpperCase());

                if (player.equals(myNickname)) {
                    totem.getStyleClass().add("totem-selected-" + color);
                } else {
                    totem.setOpacity(0.7);
                    totem.setDisable(true);
                }
            }
        }
        bringMyTotemToFront();
    }

    @FXML
    private void handleTotemSelection(MouseEvent event) {
        ImageView clicked = (ImageView) event.getSource();
        String colorString = clicked.getId().replace("totem", "").toUpperCase();
        TotemColors chosenColor;
        try { chosenColor = TotemColors.valueOf(colorString); } catch (IllegalArgumentException e) { return; }

        ClientModel model = Client.getInstance().getModel();
        String myNickname = Client.getInstance().getNickname();
        Map<String, TotemColors> playerColors = model.getPlayerColors();

        if (playerColors != null) {
            for (Map.Entry<String, TotemColors> entry : playerColors.entrySet()) {
                if (entry.getValue() == chosenColor && !entry.getKey().equals(myNickname)) return;
            }
        }
        try { Client.getInstance().getConnection().chooseTotem(chosenColor); } catch (Exception e) {}
    }

    @FXML
    private void handleStartGame(ActionEvent event) {
        try {
            Client.getInstance().getConnection().setupGame();
            setSubtitleText("Avvio partita in corso", true);
        } catch (Exception e) {}
    }

    @FXML private void handleOpenRules() { rulesOverlay.toFront(); rulesOverlay.setVisible(true); rulesOverlay.setManaged(true); updateRulesUI(); }
    @FXML private void handleCloseRules() { rulesOverlay.setVisible(false); rulesOverlay.setManaged(false); }

    @FXML
    private void handleMusicToggle() {
        MusicManager.getInstance().toggle("lobby", musicButton);
    }

    @FXML
    private void handleNextPage() {
        if (currentLeftPageIndex + 2 < TOTAL_PAGES) { currentLeftPageIndex += 2; updateRulesUI(); }
    }

    @FXML
    private void handlePrevPage() {
        if (currentLeftPageIndex - 2 >= 0) { currentLeftPageIndex -= 2; updateRulesUI(); }
    }

    private void updateRulesUI() {
        if (rulePages.isEmpty()) return;
        if (currentLeftPageIndex < rulePages.size()) leftPageImage.setImage(rulePages.get(currentLeftPageIndex));
        if (currentLeftPageIndex + 1 < rulePages.size()) rightPageImage.setImage(rulePages.get(currentLeftPageIndex + 1));

        prevPageButton.setDisable(currentLeftPageIndex == 0);
        nextPageButton.setDisable(currentLeftPageIndex + 2 >= TOTAL_PAGES);

        int displayLeft = currentLeftPageIndex + 1;
        int displayRight = Math.min(currentLeftPageIndex + 2, TOTAL_PAGES);
        pageLabel.setText(displayLeft + " - " + displayRight + " / " + TOTAL_PAGES);
    }

    private void setSubtitleText(String text, boolean animate) {
        if (animate) {
            baseWaitingText = text;
            if (dotAnimation.getStatus() != Animation.Status.RUNNING) { dotCount = 0; dotAnimation.playFromStart(); }
            subtitleLabel.setText(baseWaitingText + dotStates[dotCount]);
        } else {
            dotAnimation.stop();
            subtitleLabel.setText(text);
        }
    }

    private void bringMyTotemToFront() {
        ClientModel model = Client.getInstance().getModel();
        String myNickname = Client.getInstance().getNickname();
        if (model != null && model.getPlayerColors() != null && myNickname != null) {
            TotemColors myColor = model.getPlayerColors().get(myNickname);
            if (myColor != null) {
                switch (myColor.name().toLowerCase()) {
                    case "red": redBox.toFront(); break;
                    case "yellow": yellowBox.toFront(); break;
                    case "blue": blueBox.toFront(); break;
                    case "black": blackBox.toFront(); break;
                    case "white": whiteBox.toFront(); break;
                }
            }
        }
    }
}