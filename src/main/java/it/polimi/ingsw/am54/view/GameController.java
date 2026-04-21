package it.polimi.ingsw.am54.view;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.IGameObserver;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.messages.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.List;

public class GameController implements IGameObserver {
    private Client client;
    private String nickname;
    private String currentState = "SetUpGameState";

    @FXML private TextField serverField;
    @FXML private TextField portField;
    @FXML private TextField nicknameField;
    @FXML private ComboBox<String> connectionTypeCombo;
    @FXML private ComboBox<String> colorCombo;
    
    @FXML private VBox playerList;
    @FXML private VBox myCards;
    @FXML private HBox offeringTiles;
    @FXML private HBox upperRow;
    @FXML private HBox lowerRow;
    
    @FXML private Text gameStateText;
    @FXML private Text currentPlayerText;
    @FXML private TextArea messageArea;
    @FXML private Button actionButton;

    public void initialize() {
        connectionTypeCombo.getItems().addAll("Socket", "RMI");
        connectionTypeCombo.setValue("Socket");
        
        colorCombo.getItems().addAll("RED", "YELLOW", "WHITE", "BLACK");
        
        serverField.setText("localhost");
        portField.setText("12345");
    }

    @FXML
    private void connect() {
        String server = serverField.getText();
        int port = Integer.parseInt(portField.getText());
        nickname = nicknameField.getText();
        
        if (connectionTypeCombo.getValue().equals("Socket")) {
            client = new Client();
            if (client.connect(server, port)) {
                client.sendMessage(new JoinGameRequest(nickname));
                log("Connesso al server " + server + ":" + port);
            } else {
                log("Errore di connessione!");
            }
        } else {
            log("RMI non ancora implementato lato client");
        }
    }

    @FXML
    private void chooseColor() {
        if (client != null && colorCombo.getValue() != null) {
            client.sendMessage(new ChooseTotemRequest(TotemColors.valueOf(colorCombo.getValue())));
            log("Colore scelto: " + colorCombo.getValue());
        }
    }

    @FXML
    private void setupGame() {
        if (client != null) {
            client.sendMessage(new SetupGameRequest());
            log("Setup inviato");
        }
    }

    @FXML
    private void placeTotem(int position) {
        if (client != null) {
            client.sendMessage(new PlaceTotemRequest(position));
            log("Totem piazzato alla posizione " + position);
        }
    }

    @FXML
    private void pickCard(String row, int index) {
        if (client != null) {
            client.sendMessage(new PickCardRequest(row, index));
            log("Carta pescata da " + row + " indice " + index);
        }
    }

    private void log(String msg) {
        Platform.runLater(() -> messageArea.appendText(msg + "\n"));
    }

    @Override
    public void onPlayerJoined(Game game, Player player) {
        Platform.runLater(() -> {
            Text p = new Text(player.getNickname());
            playerList.getChildren().add(p);
            log("Giocatore unito: " + player.getNickname());
        });
    }

    @Override
    public void onPlayerReconnected(Game game, Player player) {
        log("Giocatore riconnesso: " + player.getNickname());
    }

    @Override
    public void onTotemColorChosen(Game game, Player player, TotemColors color) {
        log(player.getNickname() + " ha scelto " + color);
    }

    @Override
    public void onTotemPlaced(Game game, Player player, int offerPosition) {
        log(player.getNickname() + " ha piazzato il totem alla posizione " + offerPosition);
    }

    @Override
    public void onCardPicked(Game game, Player player, String row, int cardIndex) {
        log(player.getNickname() + " ha pescato una carta");
    }

    @Override
    public void onTurnChanged(Game game, Player newCurrentPlayer) {
        Platform.runLater(() -> {
            currentPlayerText.setText(newCurrentPlayer.getNickname());
            log("Turno di: " + newCurrentPlayer.getNickname());
        });
    }

    @Override
    public void onStateChanged(Game game, GameState newState) {
        Platform.runLater(() -> {
            currentState = newState.getClass().getSimpleName();
            gameStateText.setText(currentState);
            log("Stato cambiato: " + currentState);
        });
    }

    @Override
    public void onEndGame(Game game, List<Player> winners) {
        StringBuilder sb = new StringBuilder("Vincitore/i: ");
        for (Player p : winners) {
            sb.append(p.getNickname()).append(" ");
        }
        log(sb.toString());
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, sb.toString());
            alert.showAndWait();
        });
    }
}