package it.polimi.ingsw.am54.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;
import javafx.scene.input.MouseEvent;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.stage.Stage;

public class LoginController {

    /**
     * Gestisce il click sul tasto PLAY caricando la scena della Lobby.
     */
    @FXML
    public void onPlayClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Lobby.fxml"));
            Parent lobbyRoot = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(lobbyRoot);
        } catch (IOException e) {
            System.err.println("Errore nel caricamento di Lobby.fxml: " + e.getMessage());
        }
    }

    /**
     * Effetto zoom all'entrata del mouse.
     */
    @FXML
    public void onHoverEnter(MouseEvent event) {
        Node button = (Node) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
        st.setToX(1.15);
        st.setToY(1.15);
        st.play();
    }

    /**
     * Ritorna alla dimensione originale all'uscita del mouse.
     */
    @FXML
    public void onHoverExit(MouseEvent event) {
        Node button = (Node) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }
}