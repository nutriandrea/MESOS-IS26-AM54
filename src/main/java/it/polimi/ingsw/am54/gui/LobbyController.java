package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.SocketServerConnection;
import it.polimi.ingsw.am54.network.client.VirtualServer;
import javafx.animation.PauseTransition;
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

        if (name.isEmpty()) {
            Platform.runLater(() -> errorLabel.setText("Inserisci un nome!"));
            return;
        }

        try {
            this.myNickname = name;

            // 1. Disabilitiamo il campo per evitare che l'utente spammi "Invio"
            nicknameField.setDisable(true);

            // 2. Creiamo una nuova connessione pulita
            this.model = new ClientModel();
            SocketServerConnection connection = new SocketServerConnection("localhost", 12345, model);
            this.virtualServer = new VirtualServer(connection, name);
            this.model.setOnModelUpdatedCallback(this::updateUI);

            Platform.runLater(() -> errorLabel.setText(""));
            virtualServer.joinGame();

            // 3. TRUCCO DEFINITIVO: Il Timeout
            // Avviamo un timer di 1 secondo. Se il server ci rifiuta silenziosamente,
            // il timer scadrà, noi saremo ancora qui e forzeremo la scritta di errore!
            PauseTransition timeout = new PauseTransition(Duration.seconds(1.0));
            timeout.setOnFinished(e -> {
                // Se dopo 1 secondo il container del nickname è ancora visibile
                if (nicknameContainer.isVisible()) {
                    nicknameField.setDisable(false); // Sblocchiamo la casella di testo
                    errorLabel.setText("Nickname già in uso! Scegline un altro.");
                    nicknameField.selectAll(); // Evidenzia il testo per cancellarlo subito
                }
            });
            timeout.play();

        } catch (IOException e) {
            Platform.runLater(() -> {
                nicknameField.setDisable(false);
                errorLabel.setText("Connessione fallita!");
            });
        }
    }

    private void updateUI() {
        Platform.runLater(() -> {
            // Se il server è stato bravo e ci ha inviato un errore esplicito
            String serverError = model.getLastErrorMessage();
            if (serverError != null && !serverError.isEmpty()) {
                if (nicknameContainer.isVisible()) {
                    nicknameField.setDisable(false);
                    errorLabel.setText("Nickname già in uso! Scegline un altro.");
                    nicknameField.selectAll();
                    model.clearError();
                }
                return;
            }

            // Controlliamo se siamo stati accettati dal server
            boolean isAccepted = false;
            if (model.getPlayers() != null) {
                for (String p : model.getPlayers()) {
                    if (p.equalsIgnoreCase(myNickname)) {
                        isAccepted = true;
                        break;
                    }
                }
            }

            if (isAccepted && nicknameContainer.isVisible()) {
                // SUCCESSO: Siamo in lista!
                nicknameField.setDisable(false);
                nicknameContainer.setVisible(false);
                nicknameContainer.setManaged(false);
                lobbyContainer.setVisible(true);
                lobbyContainer.setManaged(true);
                errorLabel.setText("");
            }

            // Aggiornamento Lista Giocatori
            playersListView.getItems().clear();
            Map<String, TotemColors> chosenColors = model.getPlayerColors();
            for (String p : model.getPlayers()) {
                String c = chosenColors.containsKey(p) ? " - " + chosenColors.get(p) : " - Scelta...";
                playersListView.getItems().add(p.toUpperCase() + c);
            }

            // Aggiornamento Totem
            resetTotemUI();
            for (Map.Entry<String, TotemColors> entry : chosenColors.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(myNickname)) {
                    highlightMyTotem(entry.getValue());
                } else {
                    showXOnOtherTotem(entry.getValue());
                }
            }

            // Tasto START
            if (!model.getPlayers().isEmpty() && model.getPlayers().get(0).equalsIgnoreCase(myNickname)) {
                if (chosenColors.size() == model.getPlayers().size() && model.getPlayers().size() >= 2) {
                    startGameButton.setVisible(true); startGameButton.setManaged(true);
                } else {
                    startGameButton.setVisible(false); startGameButton.setManaged(false);
                }
            }
        });
    }

    // --- METODI HELPER (Invariati) ---
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
        if (targetImg != null) targetImg.getStyleClass().add("totem-selezionato-mio");
    }

    private void showXOnOtherTotem(TotemColors color) {
        Button targetBtn = getButtonByColor(color);
        ImageView targetImg = getImageViewByColor(color);
        Label targetX = getLabelXByColor(color);
        if (targetBtn != null) targetBtn.setDisable(true);
        if (targetImg != null) targetImg.getStyleClass().add("immagine-occupata");
        if (targetX != null) targetX.setVisible(true);
    }

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