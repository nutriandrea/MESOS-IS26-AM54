package it.polimi.ingsw.am54.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;

// Nuovi import per l'effetto hover animato
import javafx.scene.input.MouseEvent;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

public class LoginController {

    @FXML
    public void onPlayClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Lobby.fxml"));
            Parent lobbyRoot = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Invece di: Scene scene = new Scene(lobbyRoot); stage.setScene(scene);
            // Usiamo:
            stage.getScene().setRoot(lobbyRoot);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onHoverEnter(MouseEvent event) {
        Node button = (Node) event.getSource();

        // Crea un'animazione fluida di 150 millisecondi
        ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
        st.setToX(1.15); // Ingrandisce del 15% in larghezza
        st.setToY(1.15); // Ingrandisce del 15% in altezza
        st.play();
    }

    @FXML
    public void onHoverExit(MouseEvent event) {
        Node button = (Node) event.getSource();

        // Riporta il bottone alla dimensione originale (1.0 = 100%)
        ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }
}