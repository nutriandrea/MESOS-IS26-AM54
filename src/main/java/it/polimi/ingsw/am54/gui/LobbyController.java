package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.SocketServerConnection;
import it.polimi.ingsw.am54.network.client.VirtualServer;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Map;

public class LobbyController {

    @FXML private VBox nicknameContainer;
    @FXML private VBox lobbyContainer;
    @FXML private TextField nicknameField;
    @FXML private Label errorLabel;

    @FXML private ListView<String> playersListView;
    @FXML private HBox colorSelectionBox;
    @FXML private Button startGameButton;
    @FXML private Label statusLabel;

    private VirtualServer virtualServer;
    private ClientModel model;
    private String myNickname;

    /**
     * Metodo chiamato quando l'utente preme "ENTRA NELLA TRIBÙ".
     * Gestisce la connessione a localhost e l'ingresso in gioco.
     */
    @FXML
    public void onJoinClicked() {
        String name = nicknameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Inserisci un nome valido!");
            return;
        }

        try {
            this.myNickname = name;
            this.model = new ClientModel();
            // Assumiamo localhost come richiesto
            SocketServerConnection connection = new SocketServerConnection("localhost", 12345, model);
            this.virtualServer = new VirtualServer(connection, name);

            // Configuriamo il callback per aggiornare la UI ogni volta che il modello cambia
            this.model.setOnModelUpdatedCallback(this::updateUI);

            // Inviamo la richiesta di join
            virtualServer.joinGame();

            // Passiamo alla visualizzazione della lobby
            nicknameContainer.setVisible(false);
            nicknameContainer.setManaged(false);
            lobbyContainer.setVisible(true);
            lobbyContainer.setManaged(true);

            updateUI();

        } catch (IOException e) {
            errorLabel.setText("Errore: Il server è spento o irraggiungibile.");
            e.printStackTrace();
        }
    }

    private void updateUI() {
        Platform.runLater(() -> {
            // Seguiamo la logica renderSetup() della CLI
            playersListView.getItems().clear();
            Map<String, TotemColors> colors = model.getPlayerColors();

            for (String player : model.getPlayers()) {
                String c = colors.containsKey(player) ? " - " + colors.get(player) : " - Scegliendo...";
                playersListView.getItems().add(player.toUpperCase() + c);
            }

            // Mostra/Nascondi totem (askForColor nella CLI)
            if (colors.containsKey(myNickname)) {
                colorSelectionBox.setVisible(false);
                colorSelectionBox.setManaged(false);
            } else {
                colorSelectionBox.setVisible(true);
                colorSelectionBox.setManaged(true);
                statusLabel.setText("Scegli il tuo totem  " + myNickname.toUpperCase());
            }

            // Gestione del tasto START
            // 1. Devi essere il primo della lista (il "Capo Tribù")
            if (!model.getPlayers().isEmpty() && model.getPlayers().get(0).equals(myNickname)) {

                // 2. Il tasto appare solo se TUTTI hanno scelto il totem E siete almeno in 2
                if (colors.size() == model.getPlayers().size() && model.getPlayers().size() >= 2) {
                    startGameButton.setVisible(true);
                    startGameButton.setManaged(true);
                    statusLabel.setText("TUTTI PRONTI! AVVIA LA PARTITA");
                } else {
                    startGameButton.setVisible(false);
                    startGameButton.setManaged(false);

                    // Suggerimento dinamico in base a cosa manca
                    if (model.getPlayers().size() < 2) {
                        statusLabel.setText("In attesa di altri giocatori (Minimo 2)...");
                    } else {
                        statusLabel.setText("In attesa che tutti scelgano un totem...");
                    }
                }
            } else {
                // Se non sei il leader, il tasto non deve mai occupare spazio
                startGameButton.setVisible(false);
                startGameButton.setManaged(false);

                if (colors.containsKey(myNickname)) {
                    statusLabel.setText("In attesa che l'host avvii la partita...");
                }
            }

            // Transizione alla fase di gioco
            if (model.getCurrentState().equals("StartTurnState")) {
                goToBoard();
            }
        });
    }

    private void goToBoard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Board.fxml"));
            Parent boardRoot = loader.load();
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.getScene().setRoot(boardRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ANIMAZIONI E BOTTONI ---
    @FXML public void onHoverEnter(MouseEvent event) {
        Node node = (Node) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
        st.setToX(1.15); st.setToY(1.15); st.play();
    }
    @FXML public void onHoverExit(MouseEvent event) {
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