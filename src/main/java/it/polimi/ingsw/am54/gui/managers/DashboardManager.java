package it.polimi.ingsw.am54.gui.managers;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.model.TotemColors;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Manages the update of the player's personal dashboard and the display
 * of opponents. Shows resources (food, aura, magic), card count,
 * tribes and buildings, and card ordering in the player's hand.
 *
 * @author AM54
 */
public class DashboardManager {

    private final HBox handCardsContainer;
    private final VBox opponentsContainer;
    private final Label foodLabel, auraLabel, magicLabel, tribeCountLabel, buildingCountLabel;

    private final double CARD_ASPECT_RATIO = 135.0 / 190.0;

    /**
     * Constructs the manager with the JavaFX nodes it will update.
     *
     * @param handCardsContainer   the HBox that holds the player's hand cards
     * @param opponentsContainer   the VBox that holds opponent summary boxes
     * @param foodLabel            the label showing the player's food count
     * @param auraLabel            the label showing the player's aura count
     * @param magicLabel           the label showing the player's magic count
     * @param tribeCountLabel      the label showing the player's tribe card count
     * @param buildingCountLabel   the label showing the player's building count
     */
    public DashboardManager(HBox handCardsContainer, VBox opponentsContainer, Label foodLabel, Label auraLabel, Label magicLabel, Label tribeCountLabel, Label buildingCountLabel) {
        this.handCardsContainer = handCardsContainer;
        this.opponentsContainer = opponentsContainer;
        this.foodLabel = foodLabel;
        this.auraLabel = auraLabel;
        this.magicLabel = magicLabel;
        this.tribeCountLabel = tribeCountLabel;
        this.buildingCountLabel = buildingCountLabel;
    }

    /**
     * Updates the personal dashboard: resource labels (food, aura, magic),
     * card counts and the sorted hand-card list for the local player.
     *
     * @param model the current client model
     */
    public void updatePersonalDashboard(ClientModel model) {
        String myName = Client.getInstance().getNickname();
        if (myName == null || (model.getPlayers() != null && !model.getPlayers().contains(myName))) {
            myName = model.getCurrentPlayer() != null ? model.getCurrentPlayer() : (!model.getPlayers().isEmpty() ? model.getPlayers().get(0) : "Unknown");
        }

        int food = model.getPlayerFood().getOrDefault(myName, 0);
        int aura = model.getPlayerAura().getOrDefault(myName, 0);
        int magic = model.getPlayerMagic() != null ? model.getPlayerMagic().getOrDefault(myName, 0) : 0;

        if (foodLabel != null) foodLabel.setText("🍖 Food: " + food);
        if (auraLabel != null) auraLabel.setText("✨ Aura: " + aura);
        if (magicLabel != null) magicLabel.setText("🔮 Magic: " + magic);

        List<String> myCharacters = model.getPlayerTribe().getOrDefault(myName, new ArrayList<>());
        List<String> myBuildings = model.getPlayerBuildings().getOrDefault(myName, new ArrayList<>());

        if (tribeCountLabel != null) tribeCountLabel.setText("👥 Tribe Cards: " + myCharacters.size());
        if (buildingCountLabel != null) buildingCountLabel.setText("🏛️ Buildings: " + myBuildings.size());

        if (handCardsContainer != null) {
            handCardsContainer.getChildren().clear();
            List<String> sortedCards = sortPlayerCards(myCharacters, myBuildings);
            for (String cardStr : sortedCards) {
                handCardsContainer.getChildren().add(createHandCardView(cardStr, 150.0));
            }
        }
    }

    /**
     * Rebuilds the opponents panel, showing each opponent's totem, resources
     * and scrollable card list.
     *
     * @param model the current client model
     */
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

            HBox headerBox = new HBox(15);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            ImageView totemIcon = new ImageView();
            totemIcon.setFitWidth(35);
            totemIcon.setFitHeight(35);
            totemIcon.setPreserveRatio(true);

            TotemColors oppColor = model.getPlayerColors().get(oppName);
            if (oppColor != null) {
                String colorName = oppColor.name().toLowerCase();
                try {
                    totemIcon.setImage(new Image(getClass().getResourceAsStream("/images/totem_front_" + colorName + ".png")));
                } catch (Exception e) {}
            }

            Label nameLabel = new Label(oppName.toUpperCase() + "   |   🍖 Food: " + food + "   |   ✨ Aura: " + aura + "   |   🔮 Magic: " + magic);
            nameLabel.getStyleClass().add("opponent-name-label");

            headerBox.getChildren().addAll(totemIcon, nameLabel);

            // --- FIX SCROLL: Lock horizontal and block vertical scrolling ---
            ScrollPane cardsScroll = new ScrollPane();
            cardsScroll.setFitToHeight(true);
            cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            cardsScroll.getStyleClass().add("opponent-scroll-pane");

            // Force rigid heights to prevent VBox vibration
            cardsScroll.setMinHeight(150);
            cardsScroll.setMaxHeight(150);
            cardsScroll.setVmax(0); // No margin to scroll down!

            HBox cardsBox = new HBox(10);
            cardsBox.setAlignment(Pos.CENTER_LEFT);
            cardsBox.setMinHeight(140);

            // Translate mouse wheel: Vertical becomes Horizontal
            cardsScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
                if (event.getDeltaY() != 0) {
                    double speed = 2.5;
                    cardsScroll.setHvalue(cardsScroll.getHvalue() - (event.getDeltaY() * speed) / cardsBox.getWidth());
                    event.consume(); // Cancels the vertical movement
                }
            });
            // ----------------------------------------------------------------------

            List<String> oppChars = model.getPlayerTribe().getOrDefault(oppName, new ArrayList<>());
            List<String> oppBuilds = model.getPlayerBuildings().getOrDefault(oppName, new ArrayList<>());
            List<String> sortedOppCards = sortPlayerCards(oppChars, oppBuilds);

            for (String cardStr : sortedOppCards) {
                cardsBox.getChildren().add(createHandCardView(cardStr, 120.0));
            }

            if (cardsBox.getChildren().isEmpty()) {
                Label noCards = new Label("No cards currently held.");
                noCards.getStyleClass().add("opponent-no-cards");
                cardsBox.getChildren().add(noCards);
            }

            cardsScroll.setContent(cardsBox);
            oppBox.getChildren().addAll(headerBox, cardsScroll);
            opponentsContainer.getChildren().add(oppBox);
        }
    }

    /**
     * Merges character and building card lists, tags each with its category,
     * and sorts them by type weight, era and card id.
     *
     * @param chars  the player's tribe/character cards
     * @param builds the player's building cards
     * @return a sorted list of "id|era|minP|type|...|category" strings
     */
    public List<String> sortPlayerCards(List<String> chars, List<String> builds) {
        List<String> allCards = new ArrayList<>();
        for (String c : chars) allCards.add(c + "|character");
        for (String b : builds) allCards.add(b + "|building");

        allCards.sort((c1, c2) -> {
            int w1 = getSortWeight(c1);
            int w2 = getSortWeight(c2);
            if (w1 != w2) return Integer.compare(w1, w2);

            // NEW: If cards are Inventors (weight 6), group by Level/Icon
            if (w1 == 6) {
                int lvl1 = getCardLevel(c1);
                int lvl2 = getCardLevel(c2);
                if (lvl1 != lvl2) return Integer.compare(lvl1, lvl2);
            }

            int e1 = getEra(c1);
            int e2 = getEra(c2);
            if (e1 != e2) return Integer.compare(e1, e2);

            return Integer.compare(getId(c1), getId(c2));
        });

        return allCards;
    }

    /** Card display order in hand (strategy map, not string dispatch). */
    private static final Map<String, Integer> TYPE_WEIGHTS = Map.of(
            "HUNTER", 1,
            "FARMER", 2,
            "ARTIST", 3,
            "SHAMAN", 4,
            "CONSTRUCTOR", 5,
            "INVENTOR", 6
    );

    private int getCardLevel(String cardStr) {
        String[] parts = cardStr.split("\\|");
        try { return parts.length > 4 ? Integer.parseInt(parts[4]) : 0; }
        catch (Exception e) { return 0; }
    }

    private int getSortWeight(String cardStrWithType) {
        String[] parts = cardStrWithType.split("\\|");
        String category = parts[parts.length - 1]; // "building" or "character" (suffix added by sortPlayerCards)
        int era = getEra(cardStrWithType);

        if ("building".equals(category)) return 100 + era;

        String type = parts.length > 3 ? parts[3] : "";
        return TYPE_WEIGHTS.getOrDefault(type.toUpperCase(), 50);
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

    /**
     * Creates an ImageView for a hand card, loading the appropriate front
     * image and attaching a tooltip with the card name.
     *
     * @param cardStrWithType the card descriptor "id|era|minP|type|...|category"
     * @param desiredHeight   the target height in pixels
     * @return a configured ImageView for the card
     */
    public ImageView createHandCardView(String cardStrWithType, double desiredHeight) {
        ImageView view = new ImageView();
        if (cardStrWithType == null || cardStrWithType.isEmpty() || cardStrWithType.equals("null")) {
            view.setFitHeight(desiredHeight); view.setFitWidth(desiredHeight * CARD_ASPECT_RATIO); return view;
        }

        String[] split = cardStrWithType.split("\\|");
        String category = split[split.length - 1]; // "building" o "character" (suffisso)

        String imageId = split[0];
        String eraStr = split.length > 1 ? split[1] : "1";
        String cardType = split.length > 3 ? split[3] : "";
        String cardName = cardType;

        if ("building".equals(category)) {
            if (split.length > 6) cardName = split[6];
        }

        String path = buildCardImagePath(category, imageId, eraStr, split);

        try { view.setImage(new Image(getClass().getResourceAsStream(path))); } catch (Exception e) {}

        view.setFitHeight(desiredHeight);
        view.setFitWidth(desiredHeight * CARD_ASPECT_RATIO);
        view.setPreserveRatio(false);
        view.setSmooth(true);

        if (cardName != null && !cardName.isEmpty()) {
            view.setUserData(cardName);
            String finalCardName = cardName;
            view.setOnMouseEntered(ev -> {
                if (view.getUserData() instanceof Integer) return;
                Tooltip t = new Tooltip(finalCardName);
                Tooltip.install(view, t);
            });
        }

        return view;
    }

    private String buildCardImagePath(String category, String imageId, String eraStr, String[] split) {
        if ("building".equals(category)) {
            return "/images/cards/buildings/era " + eraStr + "/front/" + imageId + ".jpg";
        }
        int minP = 2;
        try { minP = Integer.parseInt(split.length > 2 ? split[2] : "0"); } catch (Exception e) {}
        return "/images/cards/characters/" + (minP >= 5 ? "5_players" : minP + "+_players") + "/era" + eraStr + "/front/" + imageId + ".jpg";
    }
}