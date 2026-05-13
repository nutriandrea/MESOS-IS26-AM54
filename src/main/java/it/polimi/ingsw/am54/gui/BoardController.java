package it.polimi.ingsw.am54.gui;

import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.network.client.VirtualServer;
import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class BoardController {

    @FXML private Label eraLabel;
    @FXML private Label phaseLabel;
    @FXML private Label turnLabel;
    @FXML private Label errorLabel;

    @FXML private HBox upperRowBox;
    @FXML private HBox lowerRowBox;
    @FXML private VBox turnOrderBox;
    @FXML private HBox offeringTrackBox;
    @FXML private VBox playersStatsBox;
    @FXML private VBox leaderboardBox;
    @FXML private Button endTurnButton;

    private VirtualServer virtualServer;
    private ClientModel model;
    private String myNickname;

    public void setConnectionData(VirtualServer virtualServer, ClientModel model, String nickname) {
        this.virtualServer = virtualServer;
        this.model = model;
        this.myNickname = nickname;

        // Iscriviamo QUESTA schermata agli aggiornamenti di rete
        this.model.setOnModelUpdatedCallback(this::updateUI);
        updateUI(); // Disegna subito la schermata iniziale
    }

    private void updateUI() {
        Platform.runLater(() -> {
            boolean myTurn = myNickname.equals(model.getCurrentPlayer());
            String state = model.getCurrentState();

            // 1. HEADER (Testi informativi)
            eraLabel.setText("ERA: " + model.getCurrentEra());
            phaseLabel.setText("Fase: " + state);
            turnLabel.setText(myTurn ? "E' IL TUO TURNO!" : "Tocca a: " + (model.getCurrentPlayer() != null ? model.getCurrentPlayer() : "Nessuno"));

            if (model.getLastErrorMessage() != null) {
                errorLabel.setText(model.getLastErrorMessage());
                model.clearError(); // Lo cancello dopo averlo mostrato
            } else {
                errorLabel.setText("");
            }

            endTurnButton.setDisable(!myTurn || !state.equals("PickingState"));

            // 2. FILE DI CARTE (Le rendo cliccabili se è il mio turno di Pescare)
            disegnaFilaCarte(upperRowBox, "upper", myTurn && state.equals("PickingState"));
            disegnaFilaCarte(lowerRowBox, "lower", myTurn && state.equals("PickingState"));

            // 3. ORDINE DI TURNO
            turnOrderBox.getChildren().clear();
            for (String slot : model.getTurnOrderTrack()) {
                turnOrderBox.getChildren().add(new Label(slot));
            }

            // 4. TRACCIATO OFFERTE (Pulsanti "Piazza" se è il mio turno di Piazzare)
            offeringTrackBox.getChildren().clear();
            for (Map.Entry<Integer, String> entry : model.getOfferingTrack().entrySet()) {
                VBox tileBox = new VBox(5);
                tileBox.setStyle("-fx-border-color: blue; -fx-padding: 8; -fx-alignment: center;");
                tileBox.getChildren().add(new Label(entry.getValue()));

                if (myTurn && state.equals("StartTurnState")) {
                    Button placeBtn = new Button("Piazza Totem qui");
                    placeBtn.setOnAction(e -> virtualServer.placeTotem(entry.getKey()));
                    tileBox.getChildren().add(placeBtn);
                }
                offeringTrackBox.getChildren().add(tileBox);
            }

            // 5. STATISTICHE GIOCATORI (Cibo, Aura, Inventario base)
            playersStatsBox.getChildren().clear();
            playersStatsBox.getChildren().add(new Label("STATISTICHE" ));
            for (String p : model.getPlayers()) {
                int food = model.getPlayerFood().getOrDefault(p, 0);
                int aura = model.getPlayerAura().getOrDefault(p, 0);

                VBox playerBox = new VBox(2);
                playerBox.setStyle("-fx-border-color: lightgray; -fx-padding: 5;");
                playerBox.getChildren().add(new Label("👤 " + p.toUpperCase()));
                playerBox.getChildren().add(new Label("🍖 Cibo: " + food + " | ✨ Aura: " + aura));

                int numCarte = model.getPlayerTribe().getOrDefault(p, new java.util.ArrayList<>()).size();
                playerBox.getChildren().add(new Label("🃏 Carte Tribù: " + numCarte));

                playersStatsBox.getChildren().add(playerBox);
            }

            // 6. CLASSIFICA LEADERBOARD (mostrata dopo la fine della partita)
            disegnaLeaderboard();
        });
    }

    private void disegnaLeaderboard() {
        leaderboardBox.getChildren().clear();
        // Keep the title label from FXML
        leaderboardBox.getChildren().add(new Label("🏆 CLASSIFICA"));

        List<LeaderboardEntry> entries = model.getLeaderboardEntries();
        if (entries == null || entries.isEmpty()) {
            leaderboardBox.setManaged(false);
            leaderboardBox.setVisible(false);
            return;
        }

        leaderboardBox.setManaged(true);
        leaderboardBox.setVisible(true);

        // Clear and rebuild
        leaderboardBox.getChildren().clear();

        Label title = new Label("🏆 CLASSIFICA (partite da " + model.getLeaderboardNumPlayers() + " giocatori)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        leaderboardBox.getChildren().add(title);

        for (LeaderboardEntry e : entries) {
            String highlight = e.getNickname().equalsIgnoreCase(myNickname) ? " <<<" : "";
            Label entryLabel = new Label(
                "#" + e.getPosition() + " - " + e.getNickname() + ": " + e.getScore() + " punti" + highlight
            );
            if (e.getNickname().equalsIgnoreCase(myNickname)) {
                entryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a6496;");
            }
            leaderboardBox.getChildren().add(entryLabel);
        }
    }

    // Metodo helper per creare i bottoni delle carte al volo
    private void disegnaFilaCarte(HBox rowBox, String riga, boolean canPick) {
        rowBox.getChildren().clear();
        int globalIndex = 0; // L'indice esatto che il server si aspetta (0, 1, 2...)

        // 1. Personaggi (e Eventi se implementati nel getCharacterRow)
        java.util.List<String> chars = riga.equals("upper") ? model.getCharacterUpperRow() : model.getCharacterLowerRow();
        for (String c : chars) {
            Button btn = creaBottoneCarta(c, riga, globalIndex++, canPick);
            rowBox.getChildren().add(btn);
        }

        // 2. Edifici
        java.util.List<String> bldgs = riga.equals("upper") ? model.getUpperBuildingRow() : model.getLowerBuildingRow();
        for (String b : bldgs) {
            Button btn = creaBottoneCarta(b, riga, globalIndex++, canPick);
            btn.setStyle("-fx-base: lightblue;"); // Coloriamo di azzurro gli edifici per distinguerli
            rowBox.getChildren().add(btn);
        }
    }

    private Button creaBottoneCarta(String testoDallaRete, String riga, int index, boolean canPick) {
        Button btn = new Button(testoDallaRete.replace("|", "\n")); // Va a capo ad ogni parametro (es. Nome \n Costo \n Aura)
        btn.setMinSize(100, 120);
        btn.setMaxSize(100, 120);
        btn.setStyle("-fx-font-size: 10px; -fx-text-alignment: center;");

        if (canPick) {
            btn.setOnAction(e -> virtualServer.pickCard(riga, index));
        } else {
            // Se non è il mio turno di pescare, i bottoni non sono cliccabili
            btn.setDisable(true);
        }
        return btn;
    }

    @FXML
    public void onEndTurnClicked() {
        virtualServer.endTurn();
    }
}