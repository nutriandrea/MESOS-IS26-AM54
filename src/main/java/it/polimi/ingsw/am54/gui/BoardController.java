package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.model.TotemColors;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoardController {

    @FXML private StackPane rootPane;

    @FXML private HBox upperMarket;
    @FXML private HBox lowerMarket;
    @FXML private HBox middleRow;

    @FXML private ImageView tribeDeckView;
    @FXML private ImageView buildingsDeckView;

    @FXML private ImageView turnOrderImage;
    @FXML private AnchorPane totemArea;
    @FXML private HBox offeringTrack;

    @FXML private StackPane recapOverlay;
    @FXML private ImageView recapFrontImageView;
    @FXML private ImageView recapBackImageView;

    @FXML private VBox personalDashboard;
    @FXML private ScrollPane handScrollPane;
    @FXML private HBox handCardsContainer;
    @FXML private Label foodLabel;
    @FXML private Label auraLabel;
    @FXML private Label magicLabel;
    @FXML private Label tribeCountLabel;
    @FXML private Label buildingCountLabel;
    @FXML private Label hintLabel;

    @FXML private StackPane opponentsOverlay;
    @FXML private VBox opponentsContainer;

    @FXML private Label actionInstructionLabel;
    @FXML private StackPane turnNotificationOverlay;
    @FXML private Label turnNotificationTitle;

    @FXML private Button endTurnButton;

    private final double CARD_ASPECT_RATIO = 135.0 / 190.0;
    private final double TOTEM_FACE_SIZE = 50.0;

    private boolean isDashboardOpen = false;
    private final double DASHBOARD_OPEN_HEIGHT = 210.0;

    private String lastKnownPlayer = null;
    private Timeline dotsTimeline;
    private String currentDots = "";

    // Flag fondamentale per impedire la sovrascrittura dei messaggi d'errore
    private boolean isShowingError = false;

    @FXML
    public void initialize() {
        rootPane.widthProperty().addListener((obs, oldV, newV) -> updateDynamicLayout());
        rootPane.heightProperty().addListener((obs, oldV, newV) -> updateDynamicLayout());

        loadRecapImages();
        setupDotsAnimation();

        ClientModel model = Client.getInstance().getModel();
        if (model != null) {
            model.setOnModelUpdatedCallback(this::refreshBoard);
            refreshBoard();
        }

        Platform.runLater(this::updateDynamicLayout);
    }

    private void setupDotsAnimation() {
        dotsTimeline = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            if (currentDots.length() >= 3) {
                currentDots = "";
            } else {
                currentDots += ".";
            }
            ClientModel model = Client.getInstance().getModel();
            if (model != null) {
                updateActionBanner(model);
            }
        }));
        dotsTimeline.setCycleCount(Timeline.INDEFINITE);
        dotsTimeline.play();
    }

    /**
     * CALCOLO DINAMICO RESPONSIVO ANTI-TAGLIO (Risolto l'overflow dell'Era 2)
     */
    private void updateDynamicLayout() {
        double w = rootPane.getWidth();
        double h = rootPane.getHeight();
        if (w <= 0 || h <= 0) return;

        double targetCardHeight = h * 0.22;
        double targetMiddleHeight = h * 0.30;

        // CALCOLO REALE DEGLI SPAZI DELLA FILA SUPERIORE (Market + Deck)
        int upperCardsCount = upperMarket.getChildren().size();
        int totalUpperItems = upperCardsCount + 2; // Carte mercato + 2 mazzi coperti

        // Ricaviamo la somma esatta dei margini e spaziature dichiarati nell'FXML
        double upperSpacing = 80.0 + 40.0 + 25.0; // 40+40 padding esterni, 40 tra market e mazzi, 25 tra i mazzi
        if (upperCardsCount > 1) {
            upperSpacing += (upperCardsCount - 1) * 15.0; // Spazi interni tra le carte mercato
        }
        double maxUpperCardWidth = (w - upperSpacing) / totalUpperItems;

        // CALCOLO REALE DEGLI SPAZI DELLA FILA INFERIORE
        int lowerCardsCount = lowerMarket.getChildren().size();
        double lowerSpacing = 80.0; // 40+40 padding esterni
        if (lowerCardsCount > 1) {
            lowerSpacing += (lowerCardsCount - 1) * 15.0;
        }
        double maxLowerCardWidth = lowerCardsCount > 0 ? (w - lowerSpacing) / lowerCardsCount : w;

        // Scegliamo la larghezza di sicurezza minore per evitare qualsiasi taglio in 16:10 o 16:9
        double maxCardWidth = Math.min(maxUpperCardWidth, maxLowerCardWidth);
        double maxCardHeightFromWidth = maxCardWidth / CARD_ASPECT_RATIO;

        double finalCardHeight = Math.min(targetCardHeight, maxCardHeightFromWidth);

        if (tribeDeckView != null) tribeDeckView.setFitHeight(finalCardHeight);
        if (buildingsDeckView != null) buildingsDeckView.setFitHeight(finalCardHeight);

        setCardsHeight(upperMarket, finalCardHeight);
        setCardsHeight(lowerMarket, finalCardHeight);

        // Calcolo anti-taglio per la fila centrale
        double maxMiddleWidth = (w - 200) / 8.0;
        double maxMiddleHeightFromWidth = maxMiddleWidth / 0.622;
        double finalMiddleHeight = Math.min(targetMiddleHeight, maxMiddleHeightFromWidth);

        if (turnOrderImage != null) turnOrderImage.setFitHeight(finalMiddleHeight);
        if (totemArea != null) {
            totemArea.setPrefHeight(finalMiddleHeight);
            totemArea.setPrefWidth(180.5 * (finalMiddleHeight / 290.0));
        }

        if (offeringTrack != null) {
            for (javafx.scene.Node node : offeringTrack.getChildren()) {
                if (node instanceof Group) {
                    Group tileGroup = (Group) node;
                    for (javafx.scene.Node child : tileGroup.getChildren()) {
                        if (child instanceof ImageView && child.getStyleClass().contains("offering-tile-img")) {
                            ((ImageView) child).setFitHeight(finalMiddleHeight);
                        }
                        else if (child instanceof AnchorPane) {
                            AnchorPane anchor = (AnchorPane) child;
                            for (javafx.scene.Node totemNode : anchor.getChildren()) {
                                if (totemNode instanceof ImageView && totemNode.getStyleClass().contains("offering-totem")) {
                                    ImageView totem = (ImageView) totemNode;

                                    double targetTotemH = 220.0;
                                    double targetTotemW = targetTotemH * (160.0 / 270.0);
                                    double centerX = 183.0 + ((392.0 - 183.0) / 2.0);
                                    double bottomY = 306.0;

                                    double totemW = finalMiddleHeight * (targetTotemW / 940.0);
                                    double totemH = finalMiddleHeight * (targetTotemH / 940.0);
                                    double totemX = finalMiddleHeight * ((centerX - targetTotemW / 2.0) / 940.0);
                                    double totemY = finalMiddleHeight * ((bottomY - targetTotemH) / 940.0);

                                    totem.setFitWidth(totemW);
                                    totem.setFitHeight(totemH);
                                    totem.setLayoutX(totemX);
                                    totem.setLayoutY(totemY);
                                }
                            }
                        }
                    }
                }
            }
        }
        refreshTotemsPosition();
    }

    private void setCardsHeight(HBox market, double height) {
        if (market == null) return;
        for (javafx.scene.Node node : market.getChildren()) {
            if (node instanceof ImageView) {
                ((ImageView) node).setFitHeight(height);
            }
        }
    }

    private void refreshTotemsPosition() {
        ClientModel model = Client.getInstance().getModel();
        if (model == null || totemArea == null || totemArea.getChildren().isEmpty()) return;

        int totalPlayers = (model.getPlayers() != null && !model.getPlayers().isEmpty()) ? model.getPlayers().size() : 3;

        for (javafx.scene.Node node : totemArea.getChildren()) {
            Object userData = node.getUserData();
            if (userData instanceof Integer) {
                int actualPos = (Integer) userData;
                node.setLayoutX(getTotemX(totalPlayers, actualPos));
                node.setLayoutY(getTotemY(totalPlayers, actualPos));
            }
        }
    }

    @FXML
    private void toggleDashboard() {
        isDashboardOpen = !isDashboardOpen;
        double targetHeight = isDashboardOpen ? DASHBOARD_OPEN_HEIGHT : 0.0;
        String hintText = isDashboardOpen ? "▼ CLICCA PER NASCONDERE LA MANO ▼" : "▲ CLICCA PER APRIRE LA MANO ▲";
        hintLabel.setText(hintText);

        Timeline animation = new Timeline(
                new KeyFrame(Duration.seconds(0.3), new KeyValue(handScrollPane.prefHeightProperty(), targetHeight))
        );
        animation.play();
    }

    @FXML
    private void toggleOpponentsOverlay() {
        opponentsOverlay.setVisible(!opponentsOverlay.isVisible());
    }

    private void loadRecapImages() {
        try {
            recapFrontImageView.setImage(new Image(getClass().getResourceAsStream("/images/recap/recap_front.jpg")));
            recapBackImageView.setImage(new Image(getClass().getResourceAsStream("/images/recap/recap_back.jpg")));
        } catch (Exception e) {
            System.err.println("[GUI] Immagini Riepilogo non trovate.");
        }
    }

    @FXML
    private void toggleRecapOverlay() {
        recapOverlay.setVisible(!recapOverlay.isVisible());
    }

    @FXML
    private void handleEndTurn() {
        if (Client.getInstance().getConnection() != null) {
            try {
                Client.getInstance().getConnection().endTurn();
            } catch (Exception e) {
                showTemporaryError("Hai ancora pick disponibili!");
            }
        }
    }

    /**
     * Mostra l'errore e attiva il blocco di protezione per 3 secondi
     */
    public void showTemporaryError(String errorMessage) {
        Platform.runLater(() -> {
            isShowingError = true; // Attiva il blocco
            actionInstructionLabel.setText(errorMessage);
            actionInstructionLabel.setStyle("-fx-text-fill: #ff4444; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 5, 0, 0, 2);");

            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> {
                actionInstructionLabel.setStyle("");
                isShowingError = false; // Rilascia il blocco
                ClientModel model = Client.getInstance().getModel();
                if (model != null) {
                    updateActionBanner(model);
                }
            });
            pause.play();
        });
    }

    public void refreshBoard() {
        Platform.runLater(() -> {
            ClientModel model = Client.getInstance().getModel();
            if (model == null) return;

            updateMarkets(model);
            updateTurnOrderTrack(model);
            updateOfferingTrack(model);
            updateDecksGraphics(model);
            updatePersonalDashboard(model);
            updateOpponentsView(model);

            handleTurnChangeNotification(model);
            updateActionBanner(model);

            // SPOSTATO IN FONDO: Controlliamo e stampiamo l'errore dopo l'action banner!
            if (model.getLastErrorMessage() != null && !model.getLastErrorMessage().isEmpty()) {
                showTemporaryError(model.getLastErrorMessage());
                model.clearError();
            }

            updateDynamicLayout();
        });
    }

    private void handleTurnChangeNotification(ClientModel model) {
        String activePlayer = model.getCurrentPlayer();
        String myName = Client.getInstance().getNickname();

        if (activePlayer == null || activePlayer.isEmpty()) return;

        if (!activePlayer.equals(lastKnownPlayer)) {
            lastKnownPlayer = activePlayer;

            turnNotificationTitle.getStyleClass().removeAll("turn-title-mine", "turn-title-other");

            if (activePlayer.equals(myName)) {
                turnNotificationTitle.setText("🔮 È IL TUO TURNO! 🔮");
                turnNotificationTitle.getStyleClass().add("turn-title-mine");
            } else {
                turnNotificationTitle.setText("👥 È IL TURNO DI " + activePlayer.toUpperCase() + " 👥");
                turnNotificationTitle.getStyleClass().add("turn-title-other");
            }

            turnNotificationOverlay.setVisible(true);

            PauseTransition hideDelay = new PauseTransition(Duration.seconds(2.0));
            hideDelay.setOnFinished(e -> turnNotificationOverlay.setVisible(false));
            hideDelay.play();
        }
    }

    private void updateActionBanner(ClientModel model) {
        if (isShowingError) return; // Se c'è un errore a schermo, non sovrascrivere il testo!

        String activePlayer = model.getCurrentPlayer();
        String myName = Client.getInstance().getNickname();
        String state = model.getCurrentState();

        if (activePlayer == null || state == null) return;

        boolean isMyTurn = activePlayer.equals(myName);
        boolean isPicking = state.equalsIgnoreCase("PICKINGSTATE") || state.equalsIgnoreCase("PICKING");

        if (isMyTurn && isPicking) {
            endTurnButton.setVisible(true);
        } else {
            endTurnButton.setVisible(false);
        }

        if (!isMyTurn) {
            actionInstructionLabel.setText("In attesa della mossa di " + activePlayer.toUpperCase() + currentDots);
            return;
        }

        switch (state.toUpperCase()) {
            case "STARTTURNSTATE":
            case "STARTTURN":
            case "BIDDINGSTATE":
                actionInstructionLabel.setText("Scegli dove piazzare il tuo totem!");
                break;

            case "PICKINGSTATE":
            case "PICKING":
                int myPosition = -1;
                for (Map.Entry<Integer, String> entry : model.getOfferingTrack().entrySet()) {
                    if (entry.getValue() != null && entry.getValue().contains(myName)) {
                        myPosition = entry.getKey();
                        break;
                    }
                }
                if (myPosition >= 2) {
                    actionInstructionLabel.setText("Scegli che carte prendere!");
                } else {
                    actionInstructionLabel.setText("Scegli che carta prendere!");
                }
                break;

            case "SETUPGAMESTATE":
            case "SETUP":
                actionInstructionLabel.setText("Fase di setup: prepara la tua tribù di partenza.");
                break;

            default:
                actionInstructionLabel.setText("Tocca a te! Fai la tua mossa.");
                break;
        }
    }

    private void updatePersonalDashboard(ClientModel model) {
        String myName = Client.getInstance().getNickname();
        if (myName == null || (model.getPlayers() != null && !model.getPlayers().contains(myName))) {
            myName = model.getCurrentPlayer() != null ? model.getCurrentPlayer() : (!model.getPlayers().isEmpty() ? model.getPlayers().get(0) : "Sconosciuto");
        }

        int food = model.getPlayerFood().getOrDefault(myName, 0);
        int aura = model.getPlayerAura().getOrDefault(myName, 0);

        int magic = 0;
        try {
            if (model.getPlayerMagic() != null) {
                magic = model.getPlayerMagic().getOrDefault(myName, 0);
            }
        } catch (Exception e) {}

        foodLabel.setText("🍖 Cibo: " + food);
        auraLabel.setText("✨ Aura: " + aura);
        magicLabel.setText("🔮 Magia: " + magic);

        List<String> myCharacters = model.getPlayerTribe().containsKey(myName) ? new ArrayList<>(model.getPlayerTribe().get(myName)) : new ArrayList<>();
        List<String> myBuildings = model.getPlayerBuildings().containsKey(myName) ? new ArrayList<>(model.getPlayerBuildings().get(myName)) : new ArrayList<>();

        tribeCountLabel.setText("👥 Carte Tribù: " + myCharacters.size());
        buildingCountLabel.setText("🏛️ Edifici: " + myBuildings.size());

        handCardsContainer.getChildren().clear();
        for (String cardStr : myCharacters) {
            handCardsContainer.getChildren().add(createHandCardView(cardStr, "character", 150.0));
        }
        for (String cardStr : myBuildings) {
            handCardsContainer.getChildren().add(createHandCardView(cardStr, "building", 150.0));
        }
    }

    private void updateOpponentsView(ClientModel model) {
        opponentsContainer.getChildren().clear();
        String myName = Client.getInstance().getNickname();
        if (myName == null) myName = "";

        if (model.getPlayers() == null) return;

        for (String oppName : model.getPlayers()) {
            if (oppName.equals(myName)) continue;

            VBox oppBox = new VBox(15);
            oppBox.getStyleClass().add("opponent-box");

            int food = model.getPlayerFood().getOrDefault(oppName, 0);
            int aura = model.getPlayerAura().getOrDefault(oppName, 0);

            int magic = 0;
            try {
                if (model.getPlayerMagic() != null) {
                    magic = model.getPlayerMagic().getOrDefault(oppName, 0);
                }
            } catch (Exception e) {}

            Label nameLabel = new Label("👤 " + oppName.toUpperCase() + "   |   🍖 Cibo: " + food + "   |   ✨ Aura: " + aura + "   |   🔮 Magia: " + magic);
            nameLabel.getStyleClass().add("opponent-name-label");

            ScrollPane cardsScroll = new ScrollPane();
            cardsScroll.setFitToHeight(true);
            cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            cardsScroll.getStyleClass().add("opponent-scroll-pane");

            HBox cardsBox = new HBox(10);
            cardsBox.setAlignment(Pos.CENTER_LEFT);

            List<String> oppChars = model.getPlayerTribe().getOrDefault(oppName, new ArrayList<>());
            List<String> oppBuilds = model.getPlayerBuildings().getOrDefault(oppName, new ArrayList<>());

            for (String c : oppChars) {
                cardsBox.getChildren().add(createHandCardView(c, "character", 120.0));
            }
            for (String b : oppBuilds) {
                cardsBox.getChildren().add(createHandCardView(b, "building", 120.0));
            }

            if (cardsBox.getChildren().isEmpty()) {
                Label noCards = new Label("Nessuna carta posseduta al momento.");
                noCards.getStyleClass().add("opponent-no-cards");
                cardsBox.getChildren().add(noCards);
            }

            cardsScroll.setContent(cardsBox);
            oppBox.getChildren().addAll(nameLabel, cardsScroll);
            opponentsContainer.getChildren().add(oppBox);
        }
    }

    private ImageView createHandCardView(String cardString, String type, double desiredHeight) {
        ImageView view = new ImageView();
        if (cardString == null || cardString.equals("null") || cardString.isEmpty()) {
            view.setFitHeight(desiredHeight);
            view.setFitWidth(desiredHeight * CARD_ASPECT_RATIO);
            return view;
        }

        String[] parts = cardString.split("\\|");
        String imageId = parts[0];
        String eraStr = parts.length > 1 ? parts[1] : "1";
        String minPStr = parts.length > 2 ? parts[2] : "0";
        String cardName = parts.length > 3 ? parts[3] : "Carta";

        if (type.equals("building") && parts.length > 6) {
            cardName = parts[6];
        }

        String path = "";
        switch(type) {
            case "building":
                path = "/images/cards/buildings/era " + eraStr + "/front/" + imageId + ".jpg";
                break;
            case "character":
                int minP = 2;
                try { minP = Integer.parseInt(minPStr); } catch (Exception ignored) {}
                String folder = (minP >= 5) ? "5_players" : minP + "+_players";
                path = "/images/cards/characters/" + folder + "/era" + eraStr + "/front/" + imageId + ".jpg";
                break;
        }

        try {
            view.setImage(new Image(getClass().getResourceAsStream(path)));
        } catch (Exception e) {}

        view.setFitHeight(desiredHeight);
        view.setPreserveRatio(true);
        view.getStyleClass().add("hand-card");
        Tooltip.install(view, new Tooltip(cardName));
        return view;
    }

    private void updateMarkets(ClientModel model) {
        upperMarket.getChildren().clear();
        lowerMarket.getChildren().clear();
        int currentIndex = 0;

        for (String cardStr : model.getCharacterUpperRow()) {
            upperMarket.getChildren().add(createCardView(cardStr, "character", "upper", currentIndex++));
        }
        for (String cardStr : model.getEventUpperRow()) {
            upperMarket.getChildren().add(createCardView(cardStr, "event", "upper", currentIndex++));
        }
        for (String cardStr : model.getUpperBuildingRow()) {
            upperMarket.getChildren().add(createCardView(cardStr, "building", "upper", currentIndex++));
        }

        currentIndex = 0;
        for (String cardStr : model.getCharacterLowerRow()) {
            lowerMarket.getChildren().add(createCardView(cardStr, "character", "lower", currentIndex++));
        }
        for (String cardStr : model.getEventLowerRow()) {
            lowerMarket.getChildren().add(createCardView(cardStr, "event", "lower", currentIndex++));
        }
        for (String cardStr : model.getLowerBuildingRow()) {
            lowerMarket.getChildren().add(createCardView(cardStr, "building", "lower", currentIndex++));
        }
    }

    private void updateDecksGraphics(ClientModel model) {
        int playerCount = (model.getPlayers() != null && !model.getPlayers().isEmpty()) ? model.getPlayers().size() : 3;
        int era = model.getCurrentEra();

        // --- DECK TRIBÙ ---
        String tribeBackId = "8";
        if (era == 2) tribeBackId = "29";
        else if (era == 3) tribeBackId = "58";

        String tribeFolder = playerCount >= 3 ? (playerCount >= 4 ? (playerCount >= 5 ? "5_players" : "4+_players") : "3+_players") : "2+_players";
        String tribeBack = "/images/cards/characters/" + tribeFolder + "/era" + era + "/back/" + tribeBackId + ".jpg";

        try {
            tribeDeckView.setImage(new Image(getClass().getResourceAsStream(tribeBack)));
            tribeDeckView.setVisible(true);
        } catch (Exception e) {
            System.err.println("[GUI] Immagini mazzo personaggi mancante.");
            tribeDeckView.setVisible(false);
        }

        // --- DECK EDIFICI ---
        String buildBackId = "99";
        if (era == 2) buildBackId = "102";
        else if (era == 3) buildBackId = "109";

        String buildingsBack = "/images/cards/buildings/era " + era + "/back/" + buildBackId + ".jpg";

        try {
            buildingsDeckView.setImage(new Image(getClass().getResourceAsStream(buildingsBack)));
            buildingsDeckView.setVisible(true);
        } catch (Exception e) {
            System.err.println("[GUI] Immagini mazzo edifici mancante.");
            buildingsDeckView.setVisible(false);
        }
    }

    private int extractId(String cardString) {
        if (cardString == null || cardString.isEmpty() || cardString.equals("null")) return -1;
        try { return Integer.parseInt(cardString.split("\\|")[0]); } catch (Exception e) { return -1; }
    }

    private void updateTurnOrderTrack(ClientModel model) {
        List<String> turnOrder = model.getTurnOrderTrack();
        List<String> players = model.getPlayers();
        Map<String, TotemColors> colors = model.getPlayerColors();

        if (turnOrder == null || turnOrder.isEmpty()) {
            turnOrder = players != null && !players.isEmpty() ? new ArrayList<>(players) : (colors != null ? new ArrayList<>(colors.keySet()) : null);
        }
        if (turnOrder == null) return;

        int totalPlayers = (players != null && !players.isEmpty()) ? players.size() : 3;
        try { turnOrderImage.setImage(new Image(getClass().getResourceAsStream("/images/turnorder/TO_" + totalPlayers + ".png"))); } catch (Exception e) {}

        totemArea.getChildren().clear();

        String state = model.getCurrentState();
        boolean isBidding = state != null && (state.equalsIgnoreCase("STARTTURNSTATE") || state.equalsIgnoreCase("STARTTURN") || state.equalsIgnoreCase("BIDDINGSTATE"));

        int validPlayersCount = 0;
        for (String s : turnOrder) {
            String pName = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;
            if (!pName.equalsIgnoreCase("vuoto") && !pName.equalsIgnoreCase("null")) {
                validPlayersCount++;
            }
        }

        int visualPos = isBidding ? (totalPlayers - validPlayersCount) : 0;
        if (visualPos < 0) visualPos = 0;

        for (int i = 0; i < turnOrder.size(); i++) {
            String rawString = turnOrder.get(i);
            String playerName = rawString.contains(" -> ") ? rawString.split(" -> ")[rawString.split(" -> ").length - 1].trim() : rawString;

            if (playerName.equalsIgnoreCase("vuoto") || playerName.equalsIgnoreCase("null")) continue;

            TotemColors playerColor = colors != null ? colors.get(playerName) : null;
            if (playerColor == null) continue;

            String colorName = playerColor.name().toLowerCase();
            ImageView totemFace = new ImageView();
            InputStream is = getClass().getResourceAsStream("/images/totem_face_" + colorName + ".png");
            if (is != null) { totemFace.setImage(new Image(is)); } else {
                InputStream fbIs = getClass().getResourceAsStream("/images/totem_" + colorName + "_3d_160x270.png");
                if (fbIs != null) totemFace.setImage(new Image(fbIs));
            }
            totemFace.setFitWidth(TOTEM_FACE_SIZE);
            totemFace.setFitHeight(TOTEM_FACE_SIZE);
            totemFace.setPreserveRatio(true);
            totemFace.getStyleClass().add("totem-face");

            int actualPos = visualPos;
            visualPos++;

            totemFace.setLayoutX(getTotemX(totalPlayers, actualPos));
            totemFace.setLayoutY(getTotemY(totalPlayers, actualPos));

            totemFace.setUserData(actualPos);

            Tooltip.install(totemFace, new Tooltip("Giocatore: " + playerName + " (Pos: " + (actualPos + 1) + ")"));
            totemArea.getChildren().add(totemFace);
        }
    }

    private double getTotemX(int totalPlayers, int positionIndex) {
        double currentHeight = turnOrderImage != null ? turnOrderImage.getFitHeight() : 290.0;
        if (currentHeight <= 0) currentHeight = 290.0;
        double scaleFactor = currentHeight / 290.0;
        double turnOrderWidth = 180.5 * scaleFactor;
        return (turnOrderWidth / 2.0) - (TOTEM_FACE_SIZE / 2.0);
    }

    private double getTotemY(int totalPlayers, int positionIndex) {
        double currentHeight = turnOrderImage != null ? turnOrderImage.getFitHeight() : 290.0;
        if (currentHeight <= 0) currentHeight = 290.0;
        double scaleFactor = currentHeight / 290.0;

        double oldScaleFactor = currentHeight / 940.0;
        double boxHeight = 332.0 - 222.0;
        double startYOriginal = 0;
        double stepOriginal = 0;

        switch (totalPlayers) {
            case 2: startYOriginal = 222.0; stepOriginal = 170.0; break;
            case 3: startYOriginal = 175.0; stepOriginal = 173.0; break;
            case 4: startYOriginal = 133.0; stepOriginal = 173.0; break;
            case 5: startYOriginal = 64.0;  stepOriginal = 174.0; break;
            default: startYOriginal = 222.0; stepOriginal = 170.0; break;
        }

        double centerYOriginal = startYOriginal + (positionIndex * stepOriginal) + (boxHeight / 2.0);
        return (centerYOriginal * oldScaleFactor) - (TOTEM_FACE_SIZE / 2.0) + (8.0 * scaleFactor);
    }

    private List<String> getExpectedOfferingTiles(int numPlayers) {
        switch (numPlayers) {
            case 2: return List.of("B", "C", "E", "F");
            case 3: return List.of("B", "C", "D", "E", "F");
            case 4: return List.of("B", "C", "D", "E", "F", "G");
            case 5: return List.of("A", "B", "C", "D", "E", "F", "G");
            default: return List.of("B", "C", "D", "E", "F");
        }
    }

    private void updateOfferingTrack(ClientModel model) {
        offeringTrack.getChildren().clear();
        Map<Integer, String> offeringData = model.getOfferingTrack();
        if (offeringData == null || offeringData.isEmpty()) return;

        int numPlayers = (model.getPlayers() != null) ? model.getPlayers().size() : 3;
        List<String> expectedTiles = getExpectedOfferingTiles(numPlayers);

        for (Integer position : offeringData.keySet()) {
            if (position >= expectedTiles.size()) continue;

            String tileLetter = expectedTiles.get(position);
            Group tileGroup = new Group();

            ImageView tileView = new ImageView();
            try { tileView.setImage(new Image(getClass().getResourceAsStream("/images/offeringtiles/OT_" + tileLetter + ".png"))); } catch (Exception e) { continue; }
            tileView.setFitHeight(290.0);
            tileView.setPreserveRatio(true);
            tileView.getStyleClass().add("offering-tile-img");

            AnchorPane totemPane = new AnchorPane();
            totemPane.setPickOnBounds(false);

            String occupier = offeringData.get(position);

            boolean hasReturned = false;
            String playerName = "";

            if (occupier != null && !occupier.equalsIgnoreCase("VUOTO") && !occupier.equalsIgnoreCase("NULL")) {
                playerName = occupier.contains(" -> ") ? occupier.split(" -> ")[occupier.split(" -> ").length - 1].trim() : occupier;
                if (model.getTurnOrderTrack() != null) {
                    for (String returnedPlayer : model.getTurnOrderTrack()) {
                        String returnedName = returnedPlayer.contains(" -> ") ? returnedPlayer.split(" -> ")[returnedPlayer.split(" -> ").length - 1].trim() : returnedPlayer;
                        if (returnedName.equalsIgnoreCase(playerName)) {
                            hasReturned = true;
                            break;
                        }
                    }
                }
            }

            if (!hasReturned && !playerName.isEmpty()) {

                TotemColors color = model.getPlayerColors().get(playerName);
                if (color == null) {
                    for(Map.Entry<String, TotemColors> entry : model.getPlayerColors().entrySet()) {
                        if(entry.getKey().equalsIgnoreCase(playerName)) { color = entry.getValue(); break; }
                    }
                }

                if (color != null) {
                    String colorName = color.name().toLowerCase();
                    InputStream is = getClass().getResourceAsStream("/images/totem_" + colorName + "_3d_160x270.png");

                    if (is != null) {
                        try {
                            ImageView totem3dView = new ImageView(new Image(is));
                            totem3dView.setPreserveRatio(true);
                            totem3dView.getStyleClass().add("offering-totem");

                            double currentTileHeight = turnOrderImage != null ? turnOrderImage.getFitHeight() : 290.0;
                            if (currentTileHeight <= 0) currentTileHeight = 290.0;

                            double targetTotemH = 220.0;
                            double targetTotemW = targetTotemH * (160.0 / 270.0);
                            double centerX = 183.0 + ((392.0 - 183.0) / 2.0);
                            double bottomY = 306.0;

                            double totemW = currentTileHeight * (targetTotemW / 940.0);
                            double totemH = currentTileHeight * (targetTotemH / 940.0);
                            double totemX = currentTileHeight * ((centerX - targetTotemW / 2.0) / 940.0);
                            double totemY = currentTileHeight * ((bottomY - targetTotemH) / 940.0);

                            totem3dView.setFitWidth(totemW);
                            totem3dView.setFitHeight(totemH);
                            totem3dView.setLayoutX(totemX);
                            totem3dView.setLayoutY(totemY);

                            totemPane.getChildren().add(totem3dView);
                        } catch (Exception e) {}
                    }
                }
                Tooltip.install(tileGroup, new Tooltip("Occupata da: " + playerName));
            } else {
                Tooltip.install(tileGroup, new Tooltip("Tessera " + tileLetter + " (Libera)"));
            }

            tileGroup.getChildren().addAll(tileView, totemPane);

            tileGroup.setOnMouseEntered(e -> {
                String myName = Client.getInstance().getNickname();
                String state = model.getCurrentState();
                if (myName != null && myName.equals(model.getCurrentPlayer()) && state != null &&
                        (state.equalsIgnoreCase("STARTTURNSTATE") || state.equalsIgnoreCase("STARTTURN") || state.equalsIgnoreCase("BIDDINGSTATE"))) {
                    tileGroup.setTranslateY(-10);
                    tileGroup.setCursor(Cursor.HAND);
                }
            });

            tileGroup.setOnMouseExited(e -> {
                tileGroup.setTranslateY(0);
                tileGroup.setCursor(Cursor.DEFAULT);
            });

            final int tileIndex = position;
            tileGroup.setOnMouseClicked(e -> {
                String myName = Client.getInstance().getNickname();
                String state = model.getCurrentState();

                if (myName != null && myName.equals(model.getCurrentPlayer()) && state != null &&
                        (state.equalsIgnoreCase("STARTTURNSTATE") || state.equalsIgnoreCase("STARTTURN") || state.equalsIgnoreCase("BIDDINGSTATE"))) {
                    if (Client.getInstance().getConnection() != null) {
                        try {
                            Client.getInstance().getConnection().placeTotem(tileIndex);
                        } catch (Exception ex) {}
                    }
                }
            });

            offeringTrack.getChildren().add(tileGroup);
        }
    }

    private ImageView createCardView(String cardString, String type, String row, int index) {
        ImageView view = new ImageView();
        if (cardString == null || cardString.equals("null") || cardString.isEmpty()) {
            view.setFitHeight(190);
            view.setFitWidth(135);
            return view;
        }

        String[] parts = cardString.split("\\|");
        String imageId = parts[0];
        String eraStr = parts.length > 1 ? parts[1] : "1";
        String minPStr = parts.length > 2 ? parts[2] : "0";
        String cardName = (type.equals("building") && parts.length > 6) ? parts[6] : (parts.length > 3 ? parts[3] : "Carta");
        final String finalCardName = cardName;

        String path = "";
        switch(type) {
            case "building":
                path = "/images/cards/buildings/era " + eraStr + "/front/" + imageId + ".jpg";
                break;
            case "character":
                int minP = 2;
                try { minP = Integer.parseInt(minPStr); } catch (Exception ignored) {}
                path = "/images/cards/characters/" + ((minP >= 5) ? "5_players" : minP + "+_players") + "/era" + eraStr + "/front/" + imageId + ".jpg";
                break;
            case "event":
                path = "/images/cards/events/era" + eraStr + "/front/" + imageId + ".jpg";
                break;
        }

        try { view.setImage(new Image(getClass().getResourceAsStream(path))); } catch (Exception e) {}

        view.setFitHeight(190);
        view.setPreserveRatio(true);
        view.getStyleClass().add("market-card");

        view.setOnMouseEntered(e -> view.setTranslateY(-10));
        view.setOnMouseExited(e -> view.setTranslateY(0));
        Tooltip.install(view, new Tooltip(finalCardName));

        view.setOnMouseClicked(e -> {
            System.out.println("[DRAFT] Cliccata carta ID: " + imageId + " (" + finalCardName + ") alla riga " + row + " indice " + index);
            if (Client.getInstance().getConnection() != null) {
                Client.getInstance().getConnection().pickCard(row, index);
            }
        });

        return view;
    }
}