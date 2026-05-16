package it.polimi.ingsw.am54.gui;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
// import it.polimi.ingsw.am54.network.client.ClientModel;
// import it.polimi.ingsw.am54.model.Card;

public class BoardController {

    @FXML private StackPane rootPane;
    @FXML private AnchorPane gameArea;

    // Mercato
    @FXML private HBox upperMarket;
    @FXML private HBox lowerMarket;

    // Mazzi coperti
    @FXML private ImageView tribeDeckView;
    @FXML private ImageView buildingsDeckView;

    // Tracciati (Li lascio pronti per quando avrai le immagini)
    @FXML private ImageView turnOrderImage;
    @FXML private VBox totemContainer;
    @FXML private HBox offeringTrack;

    private final double NATIVE_WIDTH = 1672.0;
    private final double NATIVE_HEIGHT = 941.0;

    @FXML
    public void initialize() {
        setupScaling();

        // Carica la grafica dei mazzi coperti sulla destra
        loadDecksGraphics();

        // Qui chiameremo un metodo updateMarket() quando il model sarà pronto
        // per stampare le carte FRONT nelle due file.
    }

    private void setupScaling() {
        Scale scale = new Scale(1, 1, 0, 0);
        gameArea.getTransforms().add(scale);
        rootPane.widthProperty().addListener((obs, o, n) -> updateScale(scale));
        rootPane.heightProperty().addListener((obs, o, n) -> updateScale(scale));
    }

    private void updateScale(Scale scale) {
        double factor = Math.min(rootPane.getWidth() / NATIVE_WIDTH, rootPane.getHeight() / NATIVE_HEIGHT);
        scale.setX(factor);
        scale.setY(factor);
        gameArea.setTranslateX((rootPane.getWidth() - NATIVE_WIDTH * factor) / 2.0);
        gameArea.setTranslateY((rootPane.getHeight() - NATIVE_HEIGHT * factor) / 2.0);
    }

    private void loadDecksGraphics() {
        // ClientModel model = ClientModel.getInstance();
        int playerCount = 3; // Mettiamo un default per ora

        // Carica il dorso degli Edifici
        String buildingsBackPath = "/images/cards/buildings/back/0.jpg";
        try {
            buildingsDeckView.setImage(new Image(getClass().getResourceAsStream(buildingsBackPath)));
        } catch (Exception e) {
            System.err.println("Dorso Edifici non trovato: " + buildingsBackPath);
        }

        // Carica il dorso delle Tribù in base ai giocatori
        String tribeBackPath = "/images/cards/characters/2+_players/back/0.jpg";
        if (playerCount >= 3) {
            tribeBackPath = "/images/cards/characters/3+_players/back/0.jpg";
            // Usa 3+ se vuoi mostrare l'inclusione delle carte 3 giocatori, 
            // o 2+ se il mazzo ha in cima quelle base.
        }

        try {
            tribeDeckView.setImage(new Image(getClass().getResourceAsStream(tribeBackPath)));
        } catch (Exception e) {
            System.err.println("Dorso Tribù non trovato: " + tribeBackPath);
        }
    }

    // Metodo mock per testare visivamente le carte scoperte nel mercato
    public void renderMarketMock() {
        upperMarket.getChildren().clear();
        lowerMarket.getChildren().clear();

        // Fila Superiore: Giocatori + 4 (es. 3+4 = 7 carte) + Edifici
        for(int i=0; i<7; i++) {
            ImageView card = createCardView("/images/cards/characters/2+_players/front/0.jpg");
            upperMarket.getChildren().add(card);
        }

        // Fila Inferiore: Giocatori + 1 (es. 3+1 = 4 carte)
        for(int i=0; i<4; i++) {
            ImageView card = createCardView("/images/cards/characters/2+_players/front/1.jpg");
            lowerMarket.getChildren().add(card);
        }
    }

    private ImageView createCardView(String imagePath) {
        ImageView view = new ImageView();
        try {
            view.setImage(new Image(getClass().getResourceAsStream(imagePath)));
        } catch (Exception e) {
            // Segnaposto
        }
        view.setFitHeight(190);
        view.setPreserveRatio(true);
        // Effetto hover per far capire che si possono cliccare e scegliere dal mercato
        view.setOnMouseEntered(e -> view.setTranslateY(-10));
        view.setOnMouseExited(e -> view.setTranslateY(0));
        view.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, black, 5, 0, 0, 5);");
        return view;
    }
}