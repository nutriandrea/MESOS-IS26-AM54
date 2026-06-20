package it.polimi.ingsw.am54.gui.managers;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import java.util.Map;
import java.util.function.Function;

/**
 * Manages the display of the game market, including the upper and lower rows
 * of character, building and event cards. Also provides graphical updates
 * of face-down decks (tribes and buildings) based on era and player count.
 *
 * @author AM54
 */
public class MarketManager {

    private final HBox upperMarket;
    private final HBox lowerMarket;
    private final ImageView tribeDeckView;
    private final ImageView buildingsDeckView;

    private static final Function<String[], String> CHARACTER_PATH = parts -> {
        int minP = 2;
        try { minP = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
        return "/images/cards/characters/" + ((minP >= 5) ? "5_players" : minP + "+_players") + "/era" + parts[1] + "/front/" + parts[0] + ".jpg";
    };

    private static final Map<String, Function<String[], String>> CARD_PATH_RESOLVERS = Map.of(
        "BUILDING", parts -> "/images/cards/buildings/era " + parts[1] + "/front/" + parts[0] + ".jpg",
        "EVENT", parts -> "/images/cards/events/era" + parts[1] + "/front/" + parts[0] + ".jpg"
    );

    private static final Map<String, Function<String[], String>> NAME_EXTRACTORS = Map.of(
        "BUILDING", parts -> parts.length > 6 ? parts[6] : "Edificio",
        "EVENT", parts -> parts.length > 4 ? parts[4] : "Evento"
    );

    /**
     * Constructs the manager with the JavaFX containers it will manage.
     *
     * @param upperMarket       the HBox for the upper market row
     * @param lowerMarket       the HBox for the lower market row
     * @param tribeDeckView     the ImageView for the face-down tribe deck
     * @param buildingsDeckView the ImageView for the face-down buildings deck
     */
    public MarketManager(HBox upperMarket, HBox lowerMarket, ImageView tribeDeckView, ImageView buildingsDeckView) {
        this.upperMarket = upperMarket;
        this.lowerMarket = lowerMarket;
        this.tribeDeckView = tribeDeckView;
        this.buildingsDeckView = buildingsDeckView;
    }

    /**
     * Clears and rebuilds both the upper and lower market rows from the
     * current model state, adding event, character and building cards.
     *
     * @param model the current client model
     */
    public void updateMarkets(ClientModel model) {
        System.out.println("[DEBUG-MARKET] characterUpperRow size=" + model.getCharacterUpperRow().size()
                + " data=" + model.getCharacterUpperRow());
        System.out.println("[DEBUG-MARKET] characterLowerRow size=" + model.getCharacterLowerRow().size()
                + " data=" + model.getCharacterLowerRow());
        System.out.println("[DEBUG-MARKET] eventUpperRow size=" + model.getEventUpperRow().size());
        System.out.println("[DEBUG-MARKET] upperBuildingRow size=" + model.getUpperBuildingRow().size());

        upperMarket.getChildren().clear();
        lowerMarket.getChildren().clear();

        // --- UPPER ROW ---
        for (String cardStr : model.getEventUpperRow()) {
            upperMarket.getChildren().add(createCardView(cardStr, "upper", -1, false));
        }
        int characterUpperSize = model.getCharacterUpperRow().size();
        for (int i = 0; i < characterUpperSize; i++) {
            String cardStr = model.getCharacterUpperRow().get(i);
            upperMarket.getChildren().add(createCardView(cardStr, "upper", i, true));
        }
        int buildingUpperSize = model.getUpperBuildingRow().size();
        for (int i = 0; i < buildingUpperSize; i++) {
            String cardStr = model.getUpperBuildingRow().get(i);
            upperMarket.getChildren().add(createCardView(cardStr, "upper", characterUpperSize + i, true));
        }

        // --- LOWER ROW ---
        for (String cardStr : model.getEventLowerRow()) {
            lowerMarket.getChildren().add(createCardView(cardStr, "lower", -1, false));
        }
        int characterLowerSize = model.getCharacterLowerRow().size();
        for (int i = 0; i < characterLowerSize; i++) {
            String cardStr = model.getCharacterLowerRow().get(i);
            lowerMarket.getChildren().add(createCardView(cardStr, "lower", i, true));
        }
        int buildingLowerSize = model.getLowerBuildingRow().size();
        for (int i = 0; i < buildingLowerSize; i++) {
            String cardStr = model.getLowerBuildingRow().get(i);
            lowerMarket.getChildren().add(createCardView(cardStr, "lower", characterLowerSize + i, true));
        }
    }

    /**
     * Updates the face-down tribe and building deck images based on the
     * current era and player count. Hides the building deck in era 3.
     *
     * @param model the current client model
     */
    public void updateDecksGraphics(ClientModel model) {
        int playerCount = (model.getPlayers() != null && !model.getPlayers().isEmpty()) ? model.getPlayers().size() : 3;
        int era = model.getCurrentEra();

        // --- FIX TRIBE DECK: MAPPING ORIENTED TO COMPATIBLE ASSETS TO AVOID COMPILATION BLOCK ---
        String tribeBackId = "8";
        if (playerCount == 2) {
            if (era == 1) tribeBackId = "8";
            else if (era == 2) tribeBackId = "29";
            else if (era == 3) tribeBackId = "58";
        } else if (playerCount == 3) {
            if (era == 1) tribeBackId = "3";
            else if (era == 2) tribeBackId = "31";
            else if (era == 3) tribeBackId = "73";
        } else if (playerCount == 4) {
            if (era == 1) tribeBackId = "17";
            else if (era == 2) tribeBackId = "33";
            else if (era == 3) tribeBackId = "66";
        } else {
            if (era == 1) tribeBackId = "7";
            else if (era == 2) tribeBackId = "34";
            else if (era == 3) tribeBackId = "57";
        }

        String tribeFolder = playerCount >= 3 ? (playerCount >= 4 ? (playerCount >= 5 ? "5_players" : "4+_players") : "3+_players") : "2+_players";
        String tribeBack = "/images/cards/characters/" + tribeFolder + "/era" + era + "/back/" + tribeBackId + ".jpg";

        try {
            if (tribeDeckView != null) {
                tribeDeckView.setImage(new Image(getClass().getResourceAsStream(tribeBack)));
                tribeDeckView.setVisible(true);
            }
        } catch (Exception e) {
            if (tribeDeckView != null) tribeDeckView.setVisible(false);
        }

        // --- DECK EDIFICI ---
        if (buildingsDeckView != null) {
            javafx.scene.Node deckContainer = buildingsDeckView.getParent();
            if (era == 3) {
                buildingsDeckView.setVisible(false);
                if (deckContainer != null) {
                    deckContainer.setVisible(false);
                    deckContainer.setManaged(false);
                }
            } else {
                int nextEra = era + 1;
                String buildBackId = "102"; // Back dell'Era 2 (mostrato in Era 1)
                if (nextEra == 3) {
                    buildBackId = "109"; // Back dell'Era 3 (mostrato in Era 2)
                }
                String buildingsBack = "/images/cards/buildings/era " + nextEra + "/back/" + buildBackId + ".jpg";
                try {
                    buildingsDeckView.setImage(new Image(getClass().getResourceAsStream(buildingsBack)));
                    buildingsDeckView.setVisible(true);
                    if (deckContainer != null) {
                        deckContainer.setVisible(true);
                        deckContainer.setManaged(true);
                    }
                } catch (Exception e) {
                    buildingsDeckView.setVisible(false);
                    if (deckContainer != null) {
                        deckContainer.setVisible(false);
                        deckContainer.setManaged(false);
                    }
                }
            }
        }
    }

    private ImageView createCardView(String cardString, String row, int index, boolean isPickable) {
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
        String code = parts.length > 3 ? parts[3] : "";
        String cardName = NAME_EXTRACTORS.getOrDefault(code, p -> p.length > 3 ? p[3] : "Carta").apply(parts);
        final String finalCardName = cardName;

        String path = CARD_PATH_RESOLVERS.getOrDefault(code, CHARACTER_PATH).apply(parts);

        System.out.println("[DEBUG-CARD] path=" + path + " exists=" + (getClass().getResourceAsStream(path) != null) + " for cardString=" + cardString);
        try { view.setImage(new Image(getClass().getResourceAsStream(path))); } catch (Exception e) {
            System.err.println("[DEBUG-CARD] ERROR loading image for path=" + path + " cardString=" + cardString + " error=" + e.getMessage());
        }

        view.setFitHeight(190);
        view.setPreserveRatio(true);
        view.getStyleClass().add("market-card");

        Tooltip.install(view, new Tooltip(finalCardName));

        if (isPickable) {
            view.setOnMouseEntered(e -> view.setTranslateY(-10));
            view.setOnMouseExited(e -> view.setTranslateY(0));
            view.setOnMouseClicked(e -> {
                System.out.println("[DRAFT] Cliccata carta ID: " + imageId + " (" + finalCardName + ") alla riga " + row + " indice " + index);
                if (Client.getInstance().getConnection() != null) {
                    Client.getInstance().getConnection().pickCard(row, index);
                }
            });
        }

        return view;
    }

    /**
     * Sets the fit height of all card ImageViews and deck views to the
     * given dimensions.
     *
     * @param upperHeight the target height for upper row cards and decks
     * @param lowerHeight the target height for lower row cards
     */
    public void setCardsHeight(double upperHeight, double lowerHeight) {
        if (tribeDeckView != null) tribeDeckView.setFitHeight(upperHeight);
        if (buildingsDeckView != null) buildingsDeckView.setFitHeight(upperHeight);

        for (javafx.scene.Node node : upperMarket.getChildren()) {
            if (node instanceof ImageView) ((ImageView) node).setFitHeight(upperHeight);
        }
        for (javafx.scene.Node node : lowerMarket.getChildren()) {
            if (node instanceof ImageView) ((ImageView) node).setFitHeight(lowerHeight);
        }
    }

    /**
     * Returns the number of items currently displayed in the upper market row.
     *
     * @return the child count of the upper market HBox
     */
    public int getUpperItemsCount() { return upperMarket.getChildren().size(); }

    /**
     * Returns the number of items currently displayed in the lower market row.
     *
     * @return the child count of the lower market HBox
     */
    public int getLowerItemsCount() { return lowerMarket.getChildren().size(); }
}