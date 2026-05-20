package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LoginController {

    @FXML private StackPane rootPane;

    @FXML private VBox logoBox;
    @FXML private ImageView logoMesos;
    @FXML private VBox playStateBox;
    @FXML private VBox nicknameStateBox;

    @FXML private TextField serverIpField;
    @FXML private RadioButton socketRadio;
    @FXML private RadioButton rmiRadio;
    @FXML private ToggleGroup protocolGroup;
    @FXML private TextField nicknameField;
    @FXML private Label errorLabel;

    @FXML private Label clickToEnterLabel;

    // NOTA: Abbiamo rimosso la MediaView perché non usiamo più i video!

    private boolean waitingForClick = false;
    private boolean isConnecting = false;

    @FXML
    public void initialize() {
        // Risolve il bug del layout al rientro eseguendo i calcoli solo a dimensioni consolidate
        Platform.runLater(() -> {
            if (rootPane != null) {
                logoMesos.fitWidthProperty().bind(rootPane.widthProperty().multiply(0.5));
                logoBox.translateYProperty().bind(rootPane.heightProperty().multiply(-0.33));
                playStateBox.translateYProperty().bind(rootPane.heightProperty().multiply(0.33));
            }
        });

        Client.getInstance().getModel().setOnModelUpdatedCallback(() -> {
            Platform.runLater(this::checkServerResponse);
        });
    }

    @FXML
    private void handlePlayAction() {
        logoBox.setVisible(false);
        logoBox.setManaged(false);
        playStateBox.setVisible(false);
        playStateBox.setManaged(false);

        nicknameStateBox.setVisible(true);
        nicknameStateBox.setManaged(true);

        nicknameField.requestFocus();
    }

    @FXML
    private void handleNicknameSubmit() {
        String ip = serverIpField.getText().trim();
        String nick = nicknameField.getText().trim();

        if (ip.isEmpty() || nick.isEmpty()) {
            errorLabel.setText("DATI MANCANTI!");
            return;
        }

        if (isConnecting) return;
        isConnecting = true;
        errorLabel.setText("CONNESSIONE...");

        try {
            ServerConnection connection;
            ClientModel model = Client.getInstance().getModel();

            model.clearError();

            if (socketRadio.isSelected()) {
                connection = new SocketServerConnection(ip, 12345, model);
            } else {
                Registry registry = LocateRegistry.getRegistry(ip, 1099);
                RMIGameServer serverStub = (RMIGameServer) registry.lookup("AM54-Server");
                connection = new RMIServerConnection(serverStub, model);
            }

            Client.getInstance().setConnection(connection);
            Client.getInstance().setNickname(nick);

            connection.joinGame(nick);

        } catch (Exception e) {
            errorLabel.setText("ERRORE: " + e.getMessage());
            isConnecting = false;
        }
    }

    private void checkServerResponse() {
        if (!isConnecting) return;

        ClientModel model = Client.getInstance().getModel();
        String myNick = Client.getInstance().getNickname();

        if (model.getLastErrorMessage() != null) {
            errorLabel.setText(model.getLastErrorMessage().toUpperCase());
            model.clearError();
            Client.getInstance().setConnection(null);
            Client.getInstance().setNickname(null);
            isConnecting = false;
            nicknameField.selectAll();
            nicknameField.requestFocus();
        } else if (model.getPlayers().contains(myNick)) {
            errorLabel.setText("");
            isConnecting = false;

            nicknameStateBox.setVisible(false);
            nicknameStateBox.setManaged(false);

            logoBox.setVisible(true);
            logoBox.setManaged(true);

            playStateBox.setVisible(false);
            playStateBox.setManaged(false);

            clickToEnterLabel.setVisible(true);
            clickToEnterLabel.setManaged(true);

            waitingForClick = true;
        }
    }

    @FXML
    private void handleRootClick() {
        if (waitingForClick) {
            waitingForClick = false;

            // Nascondiamo l'interfaccia
            logoBox.setVisible(false);
            logoBox.setManaged(false);
            clickToEnterLabel.setVisible(false);
            clickToEnterLabel.setManaged(false);

            // Facciamo partire la nuova transizione nativa!
            startCinematicTransition();
        }
    }

    private void startCinematicTransition() {

        double screenW = rootPane.getScene().getWidth();
        double screenH = rootPane.getScene().getHeight();

        double imageW = 2752.0;
        double imageH = 1536.0;

        double caveX_img = 1441.0;
        double caveY_img = 985.0;


        double coverScale = Math.max(screenW / imageW, screenH / imageH);

        double caveX_screen = caveX_img * coverScale;
        double caveY_screen = caveY_img * coverScale;

        double centerX = screenW / 2.0;
        double centerY = screenH / 2.0;

        double dx = caveX_screen - centerX;
        double dy = caveY_screen - centerY;

        double zoomTarget = 6.0;
        double panX = -dx * zoomTarget;
        double panY = -dy * zoomTarget;

        GUIApp.playEntranceAnimation(panX, panY, () -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Lobby.fxml"));
                Parent lobbyRoot = loader.load();

                GUIApp.changeBackground("lobby-background");
                GUIApp.changeSceneContent(lobbyRoot);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}