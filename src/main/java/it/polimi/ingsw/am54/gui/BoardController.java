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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoardController {

    @FXML private StackPane rootPane;
    @FXML private AnchorPane gameArea;

    @FXML private HBox upperMarket;
    @FXML private HBox lowerMarket;

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

    private final double NATIVE_WIDTH = 1672.0;
    private final double NATIVE_HEIGHT = 941.0;

    private final double DEFAULT_CARD_HEIGHT = 190.0;
    private final double CARD_ASPECT_RATIO = 135.0 / 190.0;

    // VARIABILE AGGIORNATA A 50.0 PER FACCE PIÙ GRANDI
    private final double TOTEM_FACE_SIZE = 50.0;

    private boolean isDashboardOpen = false;
    private final double DASHBOARD_OPEN_HEIGHT = 210.0;

    private String lastKnownPlayer = null;
    private Timeline dotsTimeline;
    private String currentDots = "";

    @FXML
    public void initialize() {
        setupScaling();
        loadRecapImages();
        setupDotsAnimation();

        upperMarket.widthProperty().addListener((obs, oldV, newV) -> resizeMarketCards(upperMarket));
        lowerMarket.widthProperty().addListener((obs, oldV, newV) -> resizeMarketCards(lowerMarket));

        ClientModel model = Client.getInstance().getModel();
        if (model != null) {
            model.setOnModelUpdatedCallback(this::refreshBoard);
            refreshBoard();
        }
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

    private void setupScaling() {
        Scale scaleTransform = new Scale(1, 1, 0, 0);
        gameArea.getTransforms().add(scaleTransform);
        rootPane.widthProperty().addListener((obs, oldV, newV) -> updateScale(scaleTransform));
        rootPane.heightProperty().addListener((obs, oldV, newV) -> updateScale(scaleTransform));
    }

    private void updateScale(Scale scaleTransform) {
        double screenW = rootPane.getWidth();
        double screenH = rootPane.getHeight();
        double factor = Math.min(screenW / NATIVE_WIDTH, screenH / NATIVE_HEIGHT);
        scaleTransform.setX(factor);
        scaleTransform.setY(factor);
        gameArea.setTranslateX((screenW - NATIVE_WIDTH * factor) / 2.0);
        gameArea.setTranslateY((screenH - NATIVE_HEIGHT * factor) / 2.0);
    }

    private void loadRecapImages() {
        try {
            recapFrontImageView.setImage(new Image(getClass().getResourceAsStream("/images/recap/recap_front.jpg")));
            recapBackImageView.setImage(new Image(getClass().getResourceAsStream("/images/recap/recap_back.jpg")));
        } catch (Exception e) {
            System.err.println("[GUI] Immagini Riepilogo non trovate.");
        }
    }

    @FXML private void toggleRecapOverlay() { recapOverlay.setVisible(!recapOverlay.isVisible()); }

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
                turnNotificationTitle.setText("👥 È il turno di " + activePlayer.toUpperCase()+ " 👥");
                turnNotificationTitle.getStyleClass().add("turn-title-other");
            }

            turnNotificationOverlay.setVisible(true);

            PauseTransition hideDelay = new PauseTransition(Duration.seconds(2.0));
            hideDelay.setOnFinished(e -> turnNotificationOverlay.setVisible(false));
            hideDelay.play();
        }
    }

    private void updateActionBanner(ClientModel model) {
        String activePlayer = model.getCurrentPlayer();
        String myName = Client.getInstance().getNickname();
        String state = model.getCurrentState();

        if (activePlayer == null || state == null) return;

        boolean isMyTurn = activePlayer.equals(myName);

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
            case "building": path = "/images/cards/buildings/era " + eraStr + "/front/" + imageId + ".jpg"; break;
            case "character":
                int minP = 2;
                try { minP = Integer.parseInt(minPStr); } catch (Exception ignored) {}
                String folder = (minP >= 5) ? "5_players" : minP + "+_players";
                path = "/images/cards/characters/" + folder + "/era" + eraStr + "/front/" + imageId + ".jpg";
                break;
        }

        try { view.setImage(new Image(getClass().getResourceAsStream(path))); } catch (Exception e) {}

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
        resizeMarketCards(upperMarket);
        resizeMarketCards(lowerMarket);
    }

    private void resizeMarketCards(HBox marketContainer) {
        if (marketContainer == null || marketContainer.getChildren().isEmpty()) return;
        double availableWidth = marketContainer.getWidth();
        if (availableWidth <= 0) return;

        int cardCount = marketContainer.getChildren().size();
        double totalSpacing = (15.0 * (cardCount - 1));
        double widthForCards = availableWidth - totalSpacing;
        double defaultTotalWidth = (DEFAULT_CARD_HEIGHT * CARD_ASPECT_RATIO) * cardCount;

        if (defaultTotalWidth > widthForCards) {
            double scaleFactor = widthForCards / defaultTotalWidth;
            double newHeight = DEFAULT_CARD_HEIGHT * scaleFactor;
            for (javafx.scene.Node node : marketContainer.getChildren()) {
                if (node instanceof ImageView) { ((ImageView) node).setFitHeight(newHeight); }
            }
        } else {
            for (javafx.scene.Node node : marketContainer.getChildren()) {
                if (node instanceof ImageView) { ((ImageView) node).setFitHeight(DEFAULT_CARD_HEIGHT); }
            }
        }
    }

    private void updateDecksGraphics(ClientModel model) {
        int playerCount = (model.getPlayers() != null && !model.getPlayers().isEmpty()) ? model.getPlayers().size() : 3;
        int era = model.getCurrentEra();
        int maxCharId = -1;
        for (String c : model.getCharacterUpperRow()) maxCharId = Math.max(maxCharId, extractId(c));
        for (String c : model.getCharacterLowerRow()) maxCharId = Math.max(maxCharId, extractId(c));

        if (maxCharId == -1) {
            if (era == 1) maxCharId = -1; if (era == 2) maxCharId = 28; if (era == 3) maxCharId = 57;
        }
        int nextDeckCardId = maxCharId + 1;
        String tribeFolder = playerCount >= 3 ? (playerCount >= 4 ? (playerCount >= 5 ? "5_players" : "4+_players") : "3+_players") : "2+_players";
        String tribeBack = "/images/cards/characters/" + tribeFolder + "/era" + era + "/back/" + nextDeckCardId + ".jpg";
        try { tribeDeckView.setImage(new Image(getClass().getResourceAsStream(tribeBack))); } catch (Exception e) {
            try { tribeDeckView.setImage(new Image(getClass().getResourceAsStream("/images/cards/characters/2+_players/era1/back/8.jpg"))); } catch (Exception ignored) {}
        }

        int maxBuildId = -1;
        for (String b : model.getUpperBuildingRow()) maxBuildId = Math.max(maxBuildId, extractId(b));
        for (String b : model.getLowerBuildingRow()) maxBuildId = Math.max(maxBuildId, extractId(b));
        if (maxBuildId == -1) {
            if (era == 1) maxBuildId = 95; if (era == 2) maxBuildId = 101; if (era == 3) maxBuildId = 108;
        }
        int nextBuildCardId = maxBuildId + 1;
        String buildingsBack = "/images/cards/buildings/era " + era + "/back/" + nextBuildCardId + ".jpg";
        try { buildingsDeckView.setImage(new Image(getClass().getResourceAsStream(buildingsBack))); } catch (Exception e) {
            try { buildingsDeckView.setImage(new Image(getClass().getResourceAsStream("/images/cards/buildings/era 1/back/99.jpg"))); } catch (Exception ignored) {}
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

        int numPlayers = turnOrder.size();
        try { turnOrderImage.setImage(new Image(getClass().getResourceAsStream("/images/turnorder/TO_" + numPlayers + ".png"))); turnOrderImage.setFitHeight(290.0); } catch (Exception e) {}

        totemArea.getChildren().clear();
        for (int i = 0; i < turnOrder.size(); i++) {
            String rawString = turnOrder.get(i);
            String playerName = rawString.contains(" -> ") ? rawString.split(" -> ")[rawString.split(" -> ").length - 1].trim() : rawString;
            TotemColors playerColor = colors != null ? colors.get(playerName) : null;
            if (playerColor == null) continue;

            String colorName = playerColor.name().toLowerCase();
            ImageView totemFace = new ImageView();
            InputStream is = getClass().getResourceAsStream("/images/totem_face_" + colorName + ".png");
            if (is != null) { totemFace.setImage(new Image(is)); } else {
                InputStream fbIs = getClass().getResourceAsStream("/images/totem_" + colorName + "_3d.png");
                if (fbIs != null) totemFace.setImage(new Image(fbIs));
            }
            totemFace.setFitWidth(TOTEM_FACE_SIZE); totemFace.setFitHeight(TOTEM_FACE_SIZE); totemFace.setPreserveRatio(true);
            totemFace.getStyleClass().add("totem-face");

            totemFace.setLayoutX(getTotemX(numPlayers, i)); totemFace.setLayoutY(getTotemY(numPlayers, i));
            Tooltip.install(totemFace, new Tooltip("Giocatore: " + playerName + " (Pos: " + (i + 1) + ")"));
            totemArea.getChildren().add(totemFace);
        }
    }

    private double getTotemX(int numPlayers, int positionIndex) {
        double scaleFactor = 290.0 / 940.0; double startXOriginal = 188.0; double boxWidth = 395.0 - 188.0;
        return ((startXOriginal + (boxWidth / 2.0)) * scaleFactor) - (TOTEM_FACE_SIZE / 2.0);
    }

    private double getTotemY(int numPlayers, int positionIndex) {
        double scaleFactor = 290.0 / 940.0; double boxHeight = 332.0 - 222.0; double startYOriginal = 0; double stepOriginal = 0;
        switch (numPlayers) {
            case 2: startYOriginal = 222.0; stepOriginal = 170.0; break;
            case 3: startYOriginal = 175.0; stepOriginal = 173.0; break;
            case 4: startYOriginal = 133.0; stepOriginal = 173.0; break;
            case 5: startYOriginal = 64.0;  stepOriginal = 174.0; break;
            default: startYOriginal = 222.0; stepOriginal = 170.0; break;
        }
        return ((startYOriginal + (positionIndex * stepOriginal) + (boxHeight / 2.0)) * scaleFactor) - (TOTEM_FACE_SIZE / 2.0) + 8.0;
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
            ImageView tileView = new ImageView();
            try { tileView.setImage(new Image(getClass().getResourceAsStream("/images/offeringtiles/OT_" + tileLetter + ".png"))); } catch (Exception e) { continue; }
            tileView.setFitHeight(290.0); tileView.setPreserveRatio(true); tileView.getStyleClass().add("offering-tile-img");

            String occupier = offeringData.get(position);
            if (occupier != null && !occupier.equals("VUOTO")) { Tooltip.install(tileView, new Tooltip("Occupata da: " + occupier)); }
            offeringTrack.getChildren().add(tileView);
        }
    }

    private ImageView createCardView(String cardString, String type, String row, int index) {
        ImageView view = new ImageView();
        if (cardString == null || cardString.equals("null") || cardString.isEmpty()) { view.setFitHeight(190); view.setFitWidth(135); return view; }

        String[] parts = cardString.split("\\|");
        String imageId = parts[0];
        String eraStr = parts.length > 1 ? parts[1] : "1";
        String minPStr = parts.length > 2 ? parts[2] : "0";
        String cardName = (type.equals("building") && parts.length > 6) ? parts[6] : (parts.length > 3 ? parts[3] : "Carta");
        final String finalCardName = cardName;

        String path = "";
        switch(type) {
            case "building": path = "/images/cards/buildings/era " + eraStr + "/front/" + imageId + ".jpg"; break;
            case "character":
                int minP = 2; try { minP = Integer.parseInt(minPStr); } catch (Exception ignored) {}
                path = "/images/cards/characters/" + ((minP >= 5) ? "5_players" : minP + "+_players") + "/era" + eraStr + "/front/" + imageId + ".jpg";
                break;
            case "event": path = "/images/cards/events/era" + eraStr + "/front/" + imageId + ".jpg"; break;
        }
        try { view.setImage(new Image(getClass().getResourceAsStream(path))); } catch (Exception e) {}
        view.setFitHeight(DEFAULT_CARD_HEIGHT); view.setPreserveRatio(true); view.getStyleClass().add("market-card");

        view.setOnMouseEntered(e -> view.setTranslateY(-10)); view.setOnMouseExited(e -> view.setTranslateY(0));
        Tooltip.install(view, new Tooltip(finalCardName));

        view.setOnMouseClicked(e -> {
            System.out.println("[DRAFT] Cliccata carta ID: " + imageId + " (" + finalCardName + ") alla riga " + row + " indice " + index);

            // AGGIORNATO AL NUOVO METODO getConnection()
            var server = Client.getInstance().getConnection();
            if (server != null) { server.pickCard(row, index); }
        });
        return view;
    }
}