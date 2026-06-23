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
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Taskbar;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Finestra di avvio del client Mesos: lascia scegliere tra modalità grafica (GUI)
 * e modalità testuale (CLI). È il punto d'ingresso del JAR client (richiamata da
 * {@link MainLauncher}, che evita il classico errore "JavaFX runtime components
 * are missing" quando il main estende {@link Application}).
 *
 * <p>Avviando con l'argomento {@code --cli} (o {@code -c}) salta del tutto la
 * finestra grafica e parte direttamente in modalità terminale.</p>
 */
public class Launcher extends Application {

    private static final String GOLD = "#d4af37";

    /**
     * Punto d'ingresso del client. Se il primo argomento è {@code --cli}/{@code -c}
     * avvia direttamente la modalità testuale, altrimenti apre la finestra di scelta.
     *
     * @param args argomenti da riga di comando
     */
    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("--cli") || args[0].equalsIgnoreCase("-c"))) {
            MainClient.main(args);
            return;
        }
        launch(args);
    }

    /**
     * Costruisce e mostra la schermata di scelta della modalità di gioco.
     *
     * @param primaryStage lo stage primario fornito da JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        applyAppIcon(primaryStage);
        primaryStage.setTitle("Mesos · Launcher");

        // --- Titolo ---
        Label title = new Label("MESOS");
        title.setStyle("-fx-font-family: 'Herculanum'; -fx-font-size: 64px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + GOLD + ";"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 14, 0.4, 0, 3);");

        Label subtitle = new Label("Scegli come vuoi giocare");
        subtitle.setStyle("-fx-font-family: 'Herculanum'; -fx-font-size: 18px; -fx-text-fill: #e8e0c8;");

        Line divider = new Line(0, 0, 320, 0);
        divider.setStroke(Color.web(GOLD));
        divider.setOpacity(0.5);

        // --- Pulsanti con descrizione ---
        VBox guiCard = createModeCard("🔮  GIOCA IN GRAFICA",
                "Interfaccia grafica completa (consigliata)",
                e -> launchGui(primaryStage));

        VBox cliCard = createModeCard("💻  GIOCA DA TERMINALE",
                "Interfaccia testuale, apre una finestra terminale",
                e -> launchCli(primaryStage));

        VBox buttons = new VBox(18, guiCard, cliCard);
        buttons.setAlignment(Pos.CENTER);

        Label footer = new Label("Ingegneria del Software · A.A. 2025/2026");
        footer.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.45);");

        VBox root = new VBox(22, title, subtitle, divider, buttons, footer);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(44));
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #241c10, #0e0b06);"
              + " -fx-border-color: " + GOLD + "; -fx-border-width: 2px; -fx-border-radius: 6px;");

        Scene scene = new Scene(root, 560, 520);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    // ===================================================================
    // AVVIO DELLE MODALITÀ
    // ===================================================================

    private void launchGui(Stage primaryStage) {
        try {
            new GUIApp().start(primaryStage);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void launchCli(Stage primaryStage) {
        try {
            String jarPath = new File(getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();
            // Usiamo il JAVA con cui gira il launcher: non dipende dal PATH di sistema.
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("mac")) {
                String[] cmd = {"osascript", "-e",
                        "tell application \"Terminal\" to do script "
                      + "\"'" + javaBin + "' -jar '" + jarPath + "' --cli\""};
                Runtime.getRuntime().exec(cmd);
            } else if (os.contains("win")) {
                // Generiamo un .bat temporaneo: niente inferno di quoting, console in UTF-8
                // e finestra che resta aperta anche in caso di errore.
                File bat = File.createTempFile("mesos-cli-", ".bat");
                bat.deleteOnExit();
                String script = "@echo off\r\n"
                        + "chcp 65001 >nul\r\n"
                        + "title Mesos - CLI\r\n"
                        + "\"" + javaBin + ".exe\" -jar \"" + jarPath + "\" --cli\r\n"
                        + "echo.\r\n"
                        + "pause\r\n";
                Files.write(bat.toPath(), script.getBytes(StandardCharsets.UTF_8));
                new ProcessBuilder("cmd.exe", "/c", "start", "Mesos - CLI", bat.getAbsolutePath()).start();
            } else {
                // Linux: prova i terminali più comuni
                String run = "'" + javaBin + "' -jar '" + jarPath + "' --cli; exec bash";
                try {
                    new ProcessBuilder("x-terminal-emulator", "-e", "bash", "-c", run).start();
                } catch (Exception e) {
                    new ProcessBuilder("xterm", "-e", "bash", "-c", run).start();
                }
            }
            System.exit(0);
        } catch (Exception ex) {
            showCliFallback(primaryStage);
        }
    }

    private void showCliFallback(Stage primaryStage) {
        Label warn = new Label("⚠️  Avvio automatico non riuscito\n\n"
                + "Apri un terminale ed esegui:\n\n"
                + "java -jar IS26-AM54-1.0-SNAPSHOT-Client.jar --cli");
        warn.setStyle("-fx-font-family: 'Herculanum'; -fx-font-size: 16px; -fx-text-fill: #ff6b6b;"
                + " -fx-text-alignment: center; -fx-wrap-text: true;");
        warn.setAlignment(Pos.CENTER);
        Button close = createButton("ESCI");
        close.setOnAction(ev -> System.exit(0));
        VBox box = new VBox(24, warn, close);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(44));
        box.setStyle("-fx-background-color: linear-gradient(to bottom, #241c10, #0e0b06);"
                + " -fx-border-color: " + GOLD + "; -fx-border-width: 2px;");
        primaryStage.setScene(new Scene(box, 560, 520));
    }

    // ===================================================================
    // COSTRUZIONE UI
    // ===================================================================

    private VBox createModeCard(String buttonText, String description,
                                javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = createButton(buttonText);
        btn.setPrefWidth(360);
        btn.setOnAction(action);

        Label desc = new Label(description);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.6);");

        VBox card = new VBox(6, btn, desc);
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        String base = "-fx-background-color: " + GOLD + "; -fx-text-fill: #1a1208;"
                + " -fx-font-family: 'Herculanum'; -fx-font-size: 18px; -fx-font-weight: bold;"
                + " -fx-background-radius: 12px; -fx-padding: 12 26 12 26; -fx-cursor: hand;";
        String hover = "-fx-background-color: #f0e68c; -fx-text-fill: #1a1208;"
                + " -fx-font-family: 'Herculanum'; -fx-font-size: 18px; -fx-font-weight: bold;"
                + " -fx-background-radius: 12px; -fx-padding: 12 26 12 26; -fx-cursor: hand;"
                + " -fx-scale-x: 1.04; -fx-scale-y: 1.04;"
                + " -fx-effect: dropshadow(gaussian, rgba(212,175,55,0.6), 16, 0.3, 0, 0);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void applyAppIcon(Stage primaryStage) {
        try {
            InputStream icon = getClass().getResourceAsStream("/images/app_mesos.png");
            if (icon != null) primaryStage.getIcons().add(new Image(icon));
        } catch (Exception ignored) {}

        // Icona nel Dock di macOS
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    InputStream is = getClass().getResourceAsStream("/images/app_mesos.png");
                    if (is != null) taskbar.setIconImage(ImageIO.read(is));
                }
            }
        } catch (Exception ignored) {}
    }
}
