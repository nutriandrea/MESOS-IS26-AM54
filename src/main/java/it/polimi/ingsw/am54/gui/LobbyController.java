package it.polimi.ingsw.am54.gui;

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
import javafx.scene.image.Image;
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

    @FXML private StackPane rulesOverlay;
    @FXML private ImageView leftPageImage;
    @FXML private ImageView rightPageImage;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageLabel;

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
            try {
                rulePages.add(new javafx.scene.image.Image(getClass().getResourceAsStream(path)));
            } catch (Exception e) {
                System.err.println("Immagine regola non trovata: " + path);
            }
        }

        dotAnimation = new Timeline(new KeyFrame(Duration.millis(750), e -> {
            dotCount = (dotCount + 1) % 4; // Cicla tra 0, 1, 2, 3
            subtitleLabel.setText(baseWaitingText + dotStates[dotCount]);
        }));
        dotAnimation.setCycleCount(Animation.INDEFINITE);

        updateLobbyUI();
    }

    private void loadImage(ImageView imageView, String string) {
        try {
            URL url = getClass().getResource(string);
            if (url != null) {
                imageView.setImage(new Image(url.toExternalForm()));
            } else {
                System.err.println("Impossibile trovare l'immagine: " + string);
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento di " + string + ": " + e.getMessage());
        }
    }

    private void updateLobbyUI() {
        ClientModel model = Client.getInstance().getModel();
        String myNickname = Client.getInstance().getNickname();

        for (String colorKey : labelMap.keySet()) {
            labelMap.get(colorKey).setText("");
        }

        for (ImageView imageView : totemMap.values()) {
            imageView.getStyleClass().removeAll(
                    "totem-selected-red",
                    "totem-selected-yellow",
                    "totem-selected-blue",
                    "totem-selected-black",
                    "totem-selected-white"
            );
            imageView.setOpacity(1.0);

            imageView.setDisable(false);
        }

        // Ora la mappa utilizza TotemColors come valore
        Map<String, TotemColors> playerColors = model.getPlayerColors();
        List<String> players = model.getPlayers();

        boolean hasChosenTotem = playerColors != null && playerColors.containsKey(myNickname);
        boolean isHost = players != null && !players.isEmpty() && players.get(0).equals(myNickname);
        int playerCount = players != null ? players.size() : 0;

        if (!hasChosenTotem) {
            setSubtitleText("SCEGLI IL TUO TOTEM!", false);
            startButton.setVisible(false);
            startButton.setManaged(false);
        } else {
            if (isHost) {
                if (playerCount > 1) {
                    setSubtitleText("Premi Start quando vuoi iniziare la partita!", false);
                    startButton.setVisible(true);
                    startButton.setManaged(true);
                } else {
                    setSubtitleText("In attesa di altri giocatori", true);
                    startButton.setVisible(false);
                    startButton.setManaged(false);
                }
            } else {
                setSubtitleText("In attesa che l'host avvii la partita", true);
                startButton.setVisible(false);
                startButton.setManaged(false);
            }
        }

        if (playerColors == null) return;

        for (Map.Entry<String, TotemColors> entry : playerColors.entrySet()) {
            String player = entry.getKey();
            // Convertiamo l'enum in stringa minuscola per usarla con le nostre labelMap e totemMap
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
    }

    @FXML
    private void handleTotemSelection(MouseEvent event) {
        ImageView clicked = (ImageView) event.getSource();

        // Estraiamo il colore come stringa maiuscola dall'ID (es: "totemRed" -> "RED")
        String colorString = clicked.getId().replace("totem", "").toUpperCase();

        TotemColors chosenColor;
        try {
            chosenColor = TotemColors.valueOf(colorString);
        } catch (IllegalArgumentException e) {
            System.err.println("Colore del totem non riconosciuto: " + colorString);
            return;
        }

        ClientModel model = Client.getInstance().getModel();
        String myNickname = Client.getInstance().getNickname();
        Map<String, TotemColors> playerColors = model.getPlayerColors();

        // Controlliamo se qualcuno (diverso da noi) ha già scelto questo specifico Enum
        if (playerColors != null) {
            for (Map.Entry<String, TotemColors> entry : playerColors.entrySet()) {
                if (entry.getValue() == chosenColor && !entry.getKey().equals(myNickname)) {
                    System.out.println("Colore occupato da: " + entry.getKey());
                    return;
                }
            }
        }

        try {
            Client.getInstance().getConnection().chooseTotem(chosenColor);
        } catch (Exception e) {
            System.err.println("Errore invio totem: " + e.getMessage());
        }
    }

    @FXML
    private void handleStartGame(ActionEvent event) {
        try {

            Client.getInstance().getConnection().setupGame();
            setSubtitleText("Avvio partita in corso", true);
        } catch (Exception e) {
            System.err.println("Errore durante l'avvio della partita: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenRules() {
        rulesOverlay.setVisible(true);
        rulesOverlay.setManaged(true);
        updateRulesUI();
    }

    @FXML
    private void handleCloseRules() {
        rulesOverlay.setVisible(false);
        rulesOverlay.setManaged(false);
    }

    @FXML
    private void handleNextPage() {
        if (currentLeftPageIndex + 2 < TOTAL_PAGES) {
            currentLeftPageIndex += 2;
            updateRulesUI();
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentLeftPageIndex - 2 >= 0) {
            currentLeftPageIndex -= 2;
            updateRulesUI();
        }
    }

    private void updateRulesUI() {
        if (rulePages.isEmpty()) return;

        if (currentLeftPageIndex < rulePages.size()) {
            leftPageImage.setImage(rulePages.get(currentLeftPageIndex));
        }

        if (currentLeftPageIndex + 1 < rulePages.size()) {
            rightPageImage.setImage(rulePages.get(currentLeftPageIndex + 1));
        }

        prevPageButton.setDisable(currentLeftPageIndex == 0);
        nextPageButton.setDisable(currentLeftPageIndex + 2 >= TOTAL_PAGES);

        int displayLeft = currentLeftPageIndex + 1;
        int displayRight = Math.min(currentLeftPageIndex + 2, TOTAL_PAGES);
        pageLabel.setText(displayLeft + " - " + displayRight + " / " + TOTAL_PAGES);
    }

    private void setSubtitleText(String text, boolean animate) {
        if (animate) {
            baseWaitingText = text;

            if (dotAnimation.getStatus() != Animation.Status.RUNNING) {
                dotCount = 0;
                dotAnimation.playFromStart();
            }
            subtitleLabel.setText(baseWaitingText + dotStates[dotCount]);

        } else {
            dotAnimation.stop();
            subtitleLabel.setText(text);
        }
    }
}