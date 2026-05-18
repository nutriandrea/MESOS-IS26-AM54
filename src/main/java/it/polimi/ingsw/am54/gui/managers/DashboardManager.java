package it.polimi.ingsw.am54.gui.managers;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class DashboardManager {

    private final HBox handCardsContainer;
    private final VBox opponentsContainer;
    private final Label foodLabel, auraLabel, magicLabel, tribeCountLabel, buildingCountLabel;

    private final double CARD_ASPECT_RATIO = 135.0 / 190.0;

    public DashboardManager(HBox handCardsContainer, VBox opponentsContainer, Label foodLabel, Label auraLabel, Label magicLabel, Label tribeCountLabel, Label buildingCountLabel) {
        this.handCardsContainer = handCardsContainer;
        this.opponentsContainer = opponentsContainer;
        this.foodLabel = foodLabel;
        this.auraLabel = auraLabel;
        this.magicLabel = magicLabel;
        this.tribeCountLabel = tribeCountLabel;
        this.buildingCountLabel = buildingCountLabel;
    }

    public void updatePersonalDashboard(ClientModel model) {
        String myName = Client.getInstance().getNickname();
        if (myName == null || (model.getPlayers() != null && !model.getPlayers().contains(myName))) {
            myName = model.getCurrentPlayer() != null ? model.getCurrentPlayer() : (!model.getPlayers().isEmpty() ? model.getPlayers().get(0) : "Sconosciuto");
        }

        int food = model.getPlayerFood().getOrDefault(myName, 0);
        int aura = model.getPlayerAura().getOrDefault(myName, 0);
        int magic = model.getPlayerMagic() != null ? model.getPlayerMagic().getOrDefault(myName, 0) : 0;

        if (foodLabel != null) foodLabel.setText("🍖 Cibo: " + food);
        if (auraLabel != null) auraLabel.setText("✨ Aura: " + aura);
        if (magicLabel != null) magicLabel.setText("🔮 Magia: " + magic);

        List<String> myCharacters = model.getPlayerTribe().getOrDefault(myName, new ArrayList<>());
        List<String> myBuildings = model.getPlayerBuildings().getOrDefault(myName, new ArrayList<>());

        if (tribeCountLabel != null) tribeCountLabel.setText("👥 Carte Tribù: " + myCharacters.size());
        if (buildingCountLabel != null) buildingCountLabel.setText("🏛️ Edifici: " + myBuildings.size());

        if (handCardsContainer != null) {
            handCardsContainer.getChildren().clear();
            List<String> sortedCards = sortPlayerCards(myCharacters, myBuildings);
            for (String cardStr : sortedCards) {
                handCardsContainer.getChildren().add(createHandCardView(cardStr, 150.0));
            }
        }
    }

    public void updateOpponentsView(ClientModel model) {
        if (opponentsContainer == null) return;
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
            int magic = model.getPlayerMagic() != null ? model.getPlayerMagic().getOrDefault(oppName, 0) : 0;

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
            List<String> sortedOppCards = sortPlayerCards(oppChars, oppBuilds);

            for (String cardStr : sortedOppCards) {
                cardsBox.getChildren().add(createHandCardView(cardStr, 120.0));
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

    // ==============================================================================
    // LOGICA DI ORDINAMENTO AVANZATA
    // ==============================================================================

    public List<String> sortPlayerCards(List<String> chars, List<String> builds) {
        List<String> allCards = new ArrayList<>();
        for (String c : chars) allCards.add(c + "|character");
        for (String b : builds) allCards.add(b + "|building");

        allCards.sort((c1, c2) -> {
            // 1. Ordine principale per Tipo di Lavoro / Building
            int w1 = getSortWeight(c1);
            int w2 = getSortWeight(c2);
            if (w1 != w2) return Integer.compare(w1, w2);

            // 2. A parità di lavoro, ordina per Era (1 -> 2 -> 3)
            int e1 = getEra(c1);
            int e2 = getEra(c2);
            if (e1 != e2) return Integer.compare(e1, e2);

            // 3. A parità di Era, ordina per ID della carta
            return Integer.compare(getId(c1), getId(c2));
        });

        return allCards;
    }

    private int getSortWeight(String cardStrWithType) {
        String[] parts = cardStrWithType.split("\\|");
        String type = parts[parts.length - 1];
        int era = getEra(cardStrWithType);

        if (type.equals("building")) return 100 + era;

        String str = cardStrWithType.toLowerCase();
        if (str.contains("hunter")) return 1;
        if (str.contains("farmer")) return 2;
        if (str.contains("artist")) return 3;
        if (str.contains("shaman")) return 4;
        if (str.contains("builder")) return 5;
        if (str.contains("inventor")) return 6;

        return 50; // Per personaggi generici o senza job stampato
    }

    private int getEra(String cardStr) {
        String[] parts = cardStr.split("\\|");
        try { return parts.length > 1 ? Integer.parseInt(parts[1]) : 1; }
        catch (Exception e) { return 1; }
    }

    private int getId(String cardStr) {
        String[] parts = cardStr.split("\\|");
        try { return Integer.parseInt(parts[0]); }
        catch (Exception e) { return 0; }
    }

    // ==============================================================================

    public ImageView createHandCardView(String cardStrWithType, double desiredHeight) {
        ImageView view = new ImageView();
        if (cardStrWithType == null || cardStrWithType.isEmpty() || cardStrWithType.equals("null")) {
            view.setFitHeight(desiredHeight); view.setFitWidth(desiredHeight * CARD_ASPECT_RATIO); return view;
        }

        String[] split = cardStrWithType.split("\\|");
        String type = split[split.length - 1];

        String imageId = split[0];
        String eraStr = split.length > 1 ? split[1] : "1";
        String cardName = split.length > 3 ? split[3] : "Carta";
        if (type.equals("building") && split.length > 6) cardName = split[6];

        String path = "";
        if (type.equals("building")) {
            path = "/images/cards/buildings/era " + eraStr + "/front/" + imageId + ".jpg";
        } else {
            int minP = 2;
            try { minP = Integer.parseInt(split.length > 2 ? split[2] : "0"); } catch (Exception e) {}
            path = "/images/cards/characters/" + (minP >= 5 ? "5_players" : minP + "+_players") + "/era" + eraStr + "/front/" + imageId + ".jpg";
        }

        try { view.setImage(new Image(getClass().getResourceAsStream(path))); } catch (Exception e) {}

        view.setFitHeight(desiredHeight);
        view.setPreserveRatio(true);
        view.getStyleClass().add("hand-card");
        Tooltip.install(view, new Tooltip(cardName));
        return view;
    }
}