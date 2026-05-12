package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.SocketServerConnection;
import it.polimi.ingsw.am54.network.client.VirtualServer;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Map;

public class LobbyController {

    @FXML private VBox nicknameContainer, lobbyContainer;
    @FXML private TextField nicknameField;
    @FXML private Label errorLabel, statusLabel;
    @FXML private ListView<String> playersListView;
    @FXML private Button startGameButton;

    @FXML private Button btnYellow, btnRed, btnBlack, btnBlue, btnWhite;
    @FXML private ImageView imgYellow, imgRed, imgBlack, imgBlue, imgWhite;
    @FXML private Label xYellow, xRed, xBlack, xBlue, xWhite;

    private VirtualServer virtualServer;
    private ClientModel model;
    private String myNickname;

    @FXML
    public void onJoinClicked() {
        String name = nicknameField.getText().trim();
        if (name.isEmpty()) { errorLabel.setText("Inserisci un nome!"); return; }
        try {
            this.myNickname = name;
            this.model = new ClientModel();
            SocketServerConnection connection = new SocketServerConnection("localhost", 12345, model);
            this.virtualServer = new VirtualServer(connection, name);
            this.model.setOnModelUpdatedCallback(this::updateUI);
            virtualServer.joinGame();
            nicknameContainer.setVisible(false); nicknameContainer.setManaged(false);
            lobbyContainer.setVisible(true); lobbyContainer.setManaged(true);
        } catch (IOException e) { errorLabel.setText("Connessione fallita!"); }
    }

    private void updateUI() {
        Platform.runLater(() -> {
            playersListView.getItems().clear();
            Map<String, TotemColors> chosenColors = model.getPlayerColors();
            for (String p : model.getPlayers()) {
                String c = chosenColors.containsKey(p) ? " - " + chosenColors.get(p) : " - Scelta...";
                playersListView.getItems().add(p.toUpperCase() + c);
            }

            resetTotemUI();

            for (Map.Entry<String, TotemColors> entry : chosenColors.entrySet()) {
                String playerName = entry.getKey();
                TotemColors color = entry.getValue();

                if (playerName.equals(myNickname)) {
                    // SE È IL MIO TOTEM: Glow dorato
                    highlightMyTotem(color);
                } else {
                    // SE È IL TOTEM DI UN ALTRO: X Rossa e Trasparenza
                    showXOnOtherTotem(color);
                }
            }

            // Gestione visibilità tasto START
            if (!model.getPlayers().isEmpty() && model.getPlayers().get(0).equals(myNickname)) {
                if (chosenColors.size() == model.getPlayers().size() && model.getPlayers().size() >= 2) {
                    startGameButton.setVisible(true); startGameButton.setManaged(true);
                }
            }
        });
    }

    private void resetTotemUI() {
        Button[] btns = {btnYellow, btnRed, btnBlack, btnBlue, btnWhite};
        ImageView[] imgs = {imgYellow, imgRed, imgBlack, imgBlue, imgWhite};
        Label[] xs = {xYellow, xRed, xBlack, xBlue, xWhite};
        for (int i = 0; i < 5; i++) {
            btns[i].setDisable(false);
            imgs[i].getStyleClass().removeAll("immagine-occupata", "totem-selezionato-mio");
            xs[i].setVisible(false);
        }
    }

    private void highlightMyTotem(TotemColors color) {
        ImageView targetImg = getImageViewByColor(color);
        if (targetImg != null) {
            targetImg.getStyleClass().add("totem-selezionato-mio");
        }
    }

    private void showXOnOtherTotem(TotemColors color) {
        Button targetBtn = getButtonByColor(color);
        ImageView targetImg = getImageViewByColor(color);
        Label targetX = getLabelXByColor(color);

        if (targetBtn != null) targetBtn.setDisable(true);
        if (targetImg != null) targetImg.getStyleClass().add("immagine-occupata");
        if (targetX != null) targetX.setVisible(true);
    }

    // Metodi helper per recuperare i nodi in base al colore
    private Button getButtonByColor(TotemColors color) {
        return switch (color) {
            case YELLOW -> btnYellow; case RED -> btnRed; case BLACK -> btnBlack;
            case BLUE -> btnBlue; case WHITE -> btnWhite;
        };
    }

    private ImageView getImageViewByColor(TotemColors color) {
        return switch (color) {
            case YELLOW -> imgYellow; case RED -> imgRed; case BLACK -> imgBlack;
            case BLUE -> imgBlue; case WHITE -> imgWhite;
        };
    }

    private Label getLabelXByColor(TotemColors color) {
        return switch (color) {
            case YELLOW -> xYellow; case RED -> xRed; case BLACK -> xBlack;
            case BLUE -> xBlue; case WHITE -> xWhite;
        };
    }

    @FXML public void onHoverEnter(javafx.scene.input.MouseEvent event) {
        Node node = (Node) event.getSource();
        if (!node.isDisable()) {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
            st.setToX(1.15); st.setToY(1.15); st.play();
        }
    }

    @FXML public void onHoverExit(javafx.scene.input.MouseEvent event) {
        Node node = (Node) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
        st.setToX(1.0); st.setToY(1.0); st.play();
    }

    @FXML public void onColorRed() { virtualServer.chooseTotem(TotemColors.RED); }
    @FXML public void onColorYellow() { virtualServer.chooseTotem(TotemColors.YELLOW); }
    @FXML public void onColorBlack() { virtualServer.chooseTotem(TotemColors.BLACK); }
    @FXML public void onColorWhite() { virtualServer.chooseTotem(TotemColors.WHITE); }
    @FXML public void onColorBlue() { virtualServer.chooseTotem(TotemColors.BLUE); }
    @FXML public void onStartGameClicked() { virtualServer.setupGame(); }
}