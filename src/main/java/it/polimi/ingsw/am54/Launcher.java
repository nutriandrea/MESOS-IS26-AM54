package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.gui.GUIApp;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;

// NUOVI IMPORT PER IL DOCK DEL MAC
import java.awt.Taskbar;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class Launcher extends Application {

    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("--cli") || args[0].equalsIgnoreCase("-c"))) {
            MainClient.main(args);
            return;
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Icona per la Finestra (Serve per Windows/Linux)
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app_mesos.png")));
        } catch (Exception e) {}

        // 2. FORZATURA ICONA NEL DOCK DI macOS (La vera magia)
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    InputStream is = getClass().getResourceAsStream("/images/app_mesos.png");
                    if (is != null) {
                        java.awt.Image dockIcon = ImageIO.read(is);
                        taskbar.setIconImage(dockIcon);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Impossibile impostare l'icona del Dock Mac");
        }

        primaryStage.setTitle("Mesos - Launcher");

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #d4af37; -fx-border-width: 3px;");

        Label title = new Label("MESOS");
        title.setStyle("-fx-font-family: 'Herculanum'; -fx-font-size: 50px; -fx-text-fill: #d4af37; -fx-effect: dropshadow(three-pass-box, black, 10, 0.5, 0, 0);");

        Label subtitle = new Label("Scegli la modalità di gioco");
        subtitle.setStyle("-fx-font-family: 'Herculanum'; -fx-font-size: 20px; -fx-text-fill: white;");

        Button btnGUI = createStyledButton("🔮 GIOCA IN GRAFICA (GUI)");
        Button btnCLI = createStyledButton("💻 GIOCA DA TERMINALE (CLI)");

        btnGUI.setOnAction(e -> {
            try {
                GUIApp guiApp = new GUIApp();
                guiApp.start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnCLI.setOnAction(e -> {
            String os = System.getProperty("os.name").toLowerCase();

            try {
                // Trova automaticamente dove si trova il file JAR nel computer
                String jarPath = new java.io.File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

                if (os.contains("mac")) {
                    // Magia per Mac: apre l'app Terminale e digita il comando
                    String[] cmd = {
                            "osascript",
                            "-e",
                            "tell application \"Terminal\" to do script \"java -jar '" + jarPath + "' --cli\""
                    };
                    Runtime.getRuntime().exec(cmd);
                } else {
                    // Magia per Windows: apre il prompt dei comandi (cmd)
                    new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe", "/k", "java -jar \"" + jarPath + "\" --cli").start();
                }

                // Chiude la finestrella del Launcher grafico, dato che ora si gioca nel terminale
                System.exit(0);

            } catch (Exception ex) {
                // Se qualcosa va storto (es. permessi bloccati), fa il fallback al vecchio messaggio
                root.getChildren().clear();
                Label cliWarning = new Label("⚠️ ERRORE AVVIO AUTOMATICO ⚠️\n\nApri il Terminale e digita manualmente:\n\njava -jar IS26-AM54-1.0-SNAPSHOT-Client.jar --cli");
                cliWarning.setStyle("-fx-font-family: 'Herculanum'; -fx-font-size: 18px; -fx-text-fill: #ff4444; -fx-text-alignment: center;");
                Button btnClose = createStyledButton("ESCI");
                btnClose.setOnAction(ev -> System.exit(0));
                root.getChildren().addAll(title, cliWarning, btnClose);
            }
        });

        root.getChildren().addAll(title, subtitle, btnGUI, btnCLI);

        Scene scene = new Scene(root, 500, 400);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private Button createStyledButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #000000; -fx-font-family: 'Herculanum'; " +
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10 20 10 20; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #f0e68c; -fx-text-fill: #000000; -fx-font-family: 'Herculanum'; " +
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10 20 10 20; -fx-cursor: hand; -fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #000000; -fx-font-family: 'Herculanum'; " +
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10 20 10 20; -fx-cursor: hand;"));
        return btn;
    }
}