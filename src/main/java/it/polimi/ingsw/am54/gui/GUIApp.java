package it.polimi.ingsw.am54.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class GUIApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Ottieni l'URL del file
        java.net.URL fontRes = getClass().getResource("/fonts/Alice_in_Wonderland_3.ttf");

        if (fontRes == null) {
            System.err.println("ERRORE: File trajanpro_bold.otf non trovato in resources/fonts/!");
        } else {
            // 2. Carica usando l'URL (toExternalForm) che è più stabile per OTF
            javafx.scene.text.Font mioFont = javafx.scene.text.Font.loadFont(fontRes.toExternalForm(), 80);

            if (mioFont == null) {
                System.err.println("ERRORE: Il file esiste ma JavaFX non riesce a leggerlo.");
            } else {
                // 3. LEGGI QUESTO NOME NELLA CONSOLE!
                System.out.println("IL NOME DA USARE NEL FXML E': " + mioFont.getFamily());
            }
        }

        URL fxmlLocation = getClass().getResource("/fxml/Login.fxml");
        if (fxmlLocation == null) {
            System.err.println("File Login.fxml non trovato!");
            System.exit(1);
        }

        Parent root = FXMLLoader.load(fxmlLocation);

        // IMPORTANTE: Non passare dimensioni qui!
        // In questo modo la Scene sarà grande quanto l'immagine al 100%
        Scene scene = new Scene(root);

        primaryStage.setTitle("AM54 - Mesos");
        primaryStage.setScene(scene);

        primaryStage.setResizable(true);

        // Impedisce alla finestra di essere rimpicciolita al punto da rompere la UI (1.48:1)
        primaryStage.setMinWidth(888);
        primaryStage.setMinHeight(600);

        primaryStage.show();
    }

    // Il main lancia semplicemente l'applicazione JavaFX
    public static void main(String[] args) {
        launch(args);
    }
}