package it.polimi.ingsw.am54.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyCombination;

public class GUIApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();

        // Creiamo la scena
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Codex Naturalis - Mesos Tribe");

        // --- TRUCCO PER IL VERO FULL SCREEN ---

        // Impostiamo le regole PRIMA di mostrare la finestra
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // 1. Rendiamo la finestra visibile
        primaryStage.show();

        // 2. Chiamiamo il FullScreen IMMEDIATAMENTE DOPO lo show
        // Questo forza il sistema operativo a switchare la modalità
        primaryStage.setFullScreen(true);

        // Disabilitiamo il resizable per sicurezza extra
        primaryStage.setResizable(false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}