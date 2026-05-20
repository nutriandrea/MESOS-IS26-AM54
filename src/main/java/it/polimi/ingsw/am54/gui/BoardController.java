package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.gui.managers.DashboardManager;
import it.polimi.ingsw.am54.gui.managers.MarketManager;
import it.polimi.ingsw.am54.gui.managers.TotemManager;
import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
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

    @FXML private StackPane eventSummaryOverlay;
    @FXML private VBox eventSummaryContainer;

    @FXML private Label roundCountLabel;

    @FXML private StackPane finalCalculationOverlay;
    @FXML private VBox finalCalculationContainer;

    private boolean isDashboardOpen = false;
    private final double DASHBOARD_OPEN_HEIGHT = 210.0;

    private String lastKnownPlayer = null;
    private Timeline dotsTimeline;
    private String currentDots = "";
    private boolean isShowingError = false;
    private boolean isGameOver = false;

    // FIX SOVRAPPOSIZIONE: Memorizza il nome del player per ritardare la notifica visiva del turno
    private String pendingNotifyPlayer = null;

    // --- MANAGERS ---
    private MarketManager marketManager;
    private DashboardManager dashboardManager;
    private TotemManager totemManager;
    private it.polimi.ingsw.am54.gui.managers.EventSummaryManager eventSummaryManager;

    @FXML
    public void initialize() {
        marketManager = new MarketManager(upperMarket, lowerMarket, tribeDeckView, buildingsDeckView);
        dashboardManager = new DashboardManager(handCardsContainer, opponentsContainer, foodLabel, auraLabel, magicLabel, tribeCountLabel, buildingCountLabel);
        totemManager = new TotemManager(turnOrderImage, totemArea, offeringTrack);
        eventSummaryManager = new it.polimi.ingsw.am54.gui.managers.EventSummaryManager(eventSummaryContainer);

        rootPane.widthProperty().addListener((obs, oldV, newV) -> updateDynamicLayout());
        rootPane.heightProperty().addListener((obs, oldV, newV) -> updateDynamicLayout());

        loadRecapImages();
        setupDotsAnimation();

        ClientModel model = Client.getInstance().getModel();
        if (model != null) {
            model.setOnModelUpdatedCallback(this::refreshBoard);

            model.setOnEventResolvedCallback(reports -> {
                javafx.application.Platform.runLater(() -> {
                    eventSummaryManager.buildEventSummary(reports, model.getPlayerColors());
                    if (eventSummaryOverlay != null) {
                        eventSummaryOverlay.setVisible(true);
                    }
                });
            });

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

    private void updateDynamicLayout() {
        double w = rootPane.getWidth();
        double h = rootPane.getHeight();
        if (w <= 0 || h <= 0) return;

        double targetCardHeight = h * 0.22;
        double targetMiddleHeight = h * 0.30;

        int totalUpperItems = marketManager.getUpperItemsCount() + 2;
        double upperSpacing = 80.0 + 40.0 + 25.0;
        if (totalUpperItems > 3) upperSpacing += (totalUpperItems - 3) * 15.0;
        double maxUpperCardWidth = (w - upperSpacing) / totalUpperItems;

        int lowerCardsCount = marketManager.getLowerItemsCount();
        double lowerSpacing = 80.0;
        if (lowerCardsCount > 1) lowerSpacing += (lowerCardsCount - 1) * 15.0;
        double maxLowerCardWidth = lowerCardsCount > 0 ? (w - lowerSpacing) / lowerCardsCount : w;

        double maxCardWidth = Math.min(maxUpperCardWidth, maxLowerCardWidth);
        double maxCardHeightFromWidth = maxCardWidth / (135.0 / 190.0);

        double finalCardHeight = Math.min(targetCardHeight, maxCardHeightFromWidth);

        marketManager.setCardsHeight(finalCardHeight);

        double maxMiddleWidth = (w - 200) / 8.0;
        double maxMiddleHeightFromWidth = maxMiddleWidth / 0.622;
        double finalMiddleHeight = Math.min(targetMiddleHeight, maxMiddleHeightFromWidth);

        if (turnOrderImage != null) turnOrderImage.setFitHeight(finalMiddleHeight);
        if (totemArea != null) {
            totemArea.setPrefHeight(finalMiddleHeight);
            totemArea.setPrefWidth(180.5 * (finalMiddleHeight / 290.0));
        }

        ClientModel model = Client.getInstance().getModel();
        if (model != null) {
            totemManager.updateOfferingTrack(model, finalMiddleHeight);
        }

        totemManager.refreshTotemsPosition();
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

    public void showTemporaryError(String errorMessage) {
        Platform.runLater(() -> {
            isShowingError = true;
            actionInstructionLabel.setText(errorMessage);
            actionInstructionLabel.setStyle("-fx-text-fill: #ff4444; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 5, 0, 0, 2);");

            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> {
                actionInstructionLabel.setStyle("");
                isShowingError = false;
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

            String state = model.getCurrentState();
            boolean hasWinners = model.getWinners() != null && !model.getWinners().isEmpty();
            boolean isEndState = state != null && (state.equalsIgnoreCase("ENDGAMESTATE") || state.equalsIgnoreCase("ENDGAME"));

            if (!isGameOver && (isEndState || hasWinners)) {
                isGameOver = true;
            }

            marketManager.updateMarkets(model);
            marketManager.updateDecksGraphics(model);

            totemManager.updateTurnOrderTrack(model);
            double h = turnOrderImage != null ? turnOrderImage.getFitHeight() : 290.0;
            totemManager.updateOfferingTrack(model, h);

            dashboardManager.updatePersonalDashboard(model);
            dashboardManager.updateOpponentsView(model);

            handleTurnChangeNotification(model);
            updateActionBanner(model);

            if (roundCountLabel != null) {
                roundCountLabel.setText("ROUND: " + model.getRoundCount());
            }

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
            // FIX SOVRAPPOSIZIONE: Se l'overlay eventi è aperto, congeliamo la notifica in coda e usciamo
            if (eventSummaryOverlay != null && eventSummaryOverlay.isVisible()) {
                pendingNotifyPlayer = activePlayer;
                return;
            }
            showTurnNotification(activePlayer, myName);
        }
    }

    // Estratta la visualizzazione grafica pura della notifica turno per invocarla al momento giusto
    private void showTurnNotification(String activePlayer, String myName) {
        lastKnownPlayer = activePlayer;
        pendingNotifyPlayer = null; // Svuotiamo la coda del congelamento

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

    private void updateActionBanner(ClientModel model) {
        if (isShowingError) return;

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

                // LA TUA LOGICA: Se sei in fase Picking ma NON sei sulle tessere offerta (posizione -1),
                // significa che sei nel "Turno Bonus" (currentTile == null nel server)!
                if (myPosition == -1) {
                    actionInstructionLabel.setText("Extra pick! Pesca una carta dalla fila sopra o termina il turno!");
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

    @FXML
    private void closeEventSummary() {
        if (eventSummaryOverlay != null) {
            eventSummaryOverlay.setVisible(false);
        }
        if (Client.getInstance().getModel() != null) {
            Client.getInstance().getModel().clearPendingEvents();
        }

        ClientModel model = Client.getInstance().getModel();
        boolean gameEnded = model != null && ((model.getWinners() != null && !model.getWinners().isEmpty()) ||
                "EndGameState".equalsIgnoreCase(model.getCurrentState()) || "EndGame".equalsIgnoreCase(model.getCurrentState()));

        if (gameEnded) {
            openFinalCalculationOverlay();
        } else {
            if (pendingNotifyPlayer != null) {
                String myName = Client.getInstance().getNickname();
                showTurnNotification(pendingNotifyPlayer, myName);
            }
        }
    }

    private void openFinalCalculationOverlay() {
        if (finalCalculationOverlay == null || finalCalculationContainer == null) return;
        finalCalculationContainer.getChildren().clear();
        ClientModel model = Client.getInstance().getModel();
        if (model == null || model.getPlayers() == null) return;

        for (String pName : model.getPlayers()) {
            VBox playerBox = new VBox(10);
            playerBox.getStyleClass().add("opponent-box");
            playerBox.setStyle("-fx-padding: 15; -fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 10;");

            Label nameLbl = new Label("👤 GIOCATORE: " + pName.toUpperCase());
            nameLbl.getStyleClass().add("opponent-name-label");
            playerBox.getChildren().add(nameLbl);

            int artistCount = 0;
            int buildingBaseAura = 0;
            int buildingEffectsAura = 0;
            int inventorAura = 0;

            java.util.List<String> tribe = model.getPlayerTribe().getOrDefault(pName, new java.util.ArrayList<>());
            java.util.List<String> buildings = model.getPlayerBuildings().getOrDefault(pName, new java.util.ArrayList<>());

            for (String card : tribe) {
                String[] parts = card.split("\\|");
                if (parts.length > 3) {
                    String type = parts[3].toUpperCase();
                    if (type.contains("ARTIST")) artistCount++;
                    if (type.contains("INVENTOR")) inventorAura += 2;
                }
            }

            for (String bld : buildings) {
                String[] parts = bld.split("\\|");
                if (parts.length > 5) {
                    try {
                        buildingBaseAura += Integer.parseInt(parts[5]);
                    } catch (Exception e) {}
                }
                buildingEffectsAura += 3;
            }

            int artistPairsAura = (artistCount / 2) * 5;
            int totalAuraCalc = model.getPlayerAura().getOrDefault(pName, 0);

            Label lblArtists = new Label(String.format("  🎨 Coppie di Artisti: +%d ✨ (Totale carte: %d)", artistPairsAura, artistCount));
            lblArtists.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

            Label lblBldBase = new Label(String.format("  🏛️ Aura Base Edifici: +%d ✨", buildingBaseAura));
            lblBldBase.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

            Label lblBldEffects = new Label(String.format("  🔮 Effetti Finali Edifici: +%d ✨", buildingEffectsAura));
            lblBldEffects.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

            Label lblInventors = new Label(String.format("  ⚙️ Inventori (Icone diverse): +%d ✨", inventorAura));
            lblInventors.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

            Label lblTotal = new Label(String.format("  🏆 TOTALIZZATORE AURA FINALE: %d ✨", totalAuraCalc));
            lblTotal.setStyle("-fx-text-fill: gold; -fx-font-size: 18px; -fx-font-weight: bold;");

            playerBox.getChildren().addAll(lblArtists, lblBldBase, lblBldEffects, lblInventors, lblTotal);
            finalCalculationContainer.getChildren().add(playerBox);
        }
        finalCalculationOverlay.setVisible(true);
    }

    @FXML
    private void handleGoToLeaderboard() {
        if (finalCalculationOverlay != null) {
            finalCalculationOverlay.setVisible(false);
        }
        GUIApp.loadLeaderboardScene();
    }
}