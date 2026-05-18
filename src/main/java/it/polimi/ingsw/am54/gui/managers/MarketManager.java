package it.polimi.ingsw.am54.gui.managers;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class MarketManager {

    private final HBox upperMarket;
    private final HBox lowerMarket;
    private final ImageView tribeDeckView;
    private final ImageView buildingsDeckView;

    public MarketManager(HBox upperMarket, HBox lowerMarket, ImageView tribeDeckView, ImageView buildingsDeckView) {
        this.upperMarket = upperMarket;
        this.lowerMarket = lowerMarket;
        this.tribeDeckView = tribeDeckView;
        this.buildingsDeckView = buildingsDeckView;
    }

    public void updateMarkets(ClientModel model) {
        upperMarket.getChildren().clear();
        lowerMarket.getChildren().clear();

        // --- UPPER ROW ---
        // 1. Eventi (Posizionati a sinistra, Non pickabili)
        for (String cardStr : model.getEventUpperRow()) {
            upperMarket.getChildren().add(createCardView(cardStr, "event", "upper", -1, false));
        }
        // 2. Characters (Al centro, Pickabili, indice corretto da 0 a size-1)
        int characterUpperSize = model.getCharacterUpperRow().size();
        for (int i = 0; i < characterUpperSize; i++) {
            String cardStr = model.getCharacterUpperRow().get(i);
            upperMarket.getChildren().add(createCardView(cardStr, "character", "upper", i, true));
        }
        // 3. Buildings (A destra, Pickabili, indice a partire da characterUpperSize)
        int buildingUpperSize = model.getUpperBuildingRow().size();
        for (int i = 0; i < buildingUpperSize; i++) {
            String cardStr = model.getUpperBuildingRow().get(i);
            upperMarket.getChildren().add(createCardView(cardStr, "building", "upper", characterUpperSize + i, true));
        }

        // --- LOWER ROW ---
        // 1. Eventi (Posizionati a sinistra, Non pickabili)
        for (String cardStr : model.getEventLowerRow()) {
            lowerMarket.getChildren().add(createCardView(cardStr, "event", "lower", -1, false));
        }
        // 2. Characters (Al centro, Pickabili, indice corretto da 0 a size-1)
        int characterLowerSize = model.getCharacterLowerRow().size();
        for (int i = 0; i < characterLowerSize; i++) {
            String cardStr = model.getCharacterLowerRow().get(i);
            lowerMarket.getChildren().add(createCardView(cardStr, "character", "lower", i, true));
        }
        // 3. Buildings (A destra, Pickabili, indice a partire da characterLowerSize)
        int buildingLowerSize = model.getLowerBuildingRow().size();
        for (int i = 0; i < buildingLowerSize; i++) {
            String cardStr = model.getLowerBuildingRow().get(i);
            lowerMarket.getChildren().add(createCardView(cardStr, "building", "lower", characterLowerSize + i, true));
        }
    }

    public void updateDecksGraphics(ClientModel model) {
        int playerCount = (model.getPlayers() != null && !model.getPlayers().isEmpty()) ? model.getPlayers().size() : 3;
        int era = model.getCurrentEra();

        // --- DECK TRIBÙ ---
        String tribeBackId = "8";
        if (era == 2) tribeBackId = "29";
        else if (era == 3) tribeBackId = "58";

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
        String buildBackId = "99";
        if (era == 2) buildBackId = "102";
        else if (era == 3) buildBackId = "109";

        String buildingsBack = "/images/cards/buildings/era " + era + "/back/" + buildBackId + ".jpg";

        try {
            if (buildingsDeckView != null) {
                buildingsDeckView.setImage(new Image(getClass().getResourceAsStream(buildingsBack)));
                buildingsDeckView.setVisible(true);
            }
        } catch (Exception e) {
            if (buildingsDeckView != null) buildingsDeckView.setVisible(false);
        }
    }

    private ImageView createCardView(String cardString, String type, String row, int index, boolean isPickable) {
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

        Tooltip.install(view, new Tooltip(finalCardName));

        // Applica animazioni e handler di click solo se la carta è pickabile (disabilitato per eventi)
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

    public void setCardsHeight(double height) {
        if (tribeDeckView != null) tribeDeckView.setFitHeight(height);
        if (buildingsDeckView != null) buildingsDeckView.setFitHeight(height);

        for (javafx.scene.Node node : upperMarket.getChildren()) {
            if (node instanceof ImageView) ((ImageView) node).setFitHeight(height);
        }
        for (javafx.scene.Node node : lowerMarket.getChildren()) {
            if (node instanceof ImageView) ((ImageView) node).setFitHeight(height);
        }
    }

    public int getUpperItemsCount() { return upperMarket.getChildren().size(); }
    public int getLowerItemsCount() { return lowerMarket.getChildren().size(); }
}