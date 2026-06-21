package it.polimi.ingsw.am54.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.input.KeyCombination;
import javafx.animation.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.awt.Taskbar;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class GUIApp extends Application {

    private static StackPane rootWrapper;
    private static Region backgroundLayer;
    private static StackPane contentLayer;
    private static StackPane transitionLayer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        rootWrapper = new StackPane();
        rootWrapper.setStyle("-fx-background-color: black;");

        rootWrapper.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());
        rootWrapper.getStylesheets().add(getClass().getResource("/css/lobby.css").toExternalForm());

        backgroundLayer = new Region();
        backgroundLayer.getStyleClass().add("login-background");

        contentLayer = new StackPane();
        Parent loginContent = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
        contentLayer.getChildren().add(loginContent);

        transitionLayer = new StackPane();
        transitionLayer.setMouseTransparent(true);


        Rectangle blackScreen = new Rectangle();
        blackScreen.setFill(Color.BLACK);
        blackScreen.widthProperty().bind(rootWrapper.widthProperty());
        blackScreen.heightProperty().bind(rootWrapper.heightProperty());
        transitionLayer.getChildren().add(blackScreen);

        rootWrapper.getChildren().addAll(backgroundLayer, contentLayer, transitionLayer);

        Scene scene = new Scene(rootWrapper);
        primaryStage.setScene(scene);

        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // Impostiamo il fullscreen PRIMA di chiamare il metodo show()
        primaryStage.setFullScreen(true);
        primaryStage.show();

        // ==========================================================
        // EFFETTO "FADE IN" DA VIDEOGIOCO
        // Aspettiamo mezzo secondo che il Mac finisca l'animazione
        // del Fullscreen, poi dissolviamo il nero!
        // ==========================================================
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), blackScreen);
        fadeIn.setFromValue(1.0);
        fadeIn.setToValue(0.0);
        fadeIn.setDelay(Duration.millis(600)); // Tempo di attesa per la stabilizzazione
        fadeIn.setOnFinished(e -> transitionLayer.getChildren().remove(blackScreen));
        fadeIn.play();
    }

    public static void changeSceneContent(Parent newContent) {
        newContent.setStyle("-fx-background-color: transparent;");
        contentLayer.getChildren().setAll(newContent);
    }

    public static void changeBackground(String styleClass) {
        backgroundLayer.getStyleClass().clear();
        backgroundLayer.getStyleClass().add(styleClass);
    }

    public static void playEntranceAnimation(double panX, double panY, Runnable onPitchBlack) {
        Rectangle blackScreen = new Rectangle();
        blackScreen.setFill(Color.BLACK);
        blackScreen.setOpacity(0.0);
        blackScreen.widthProperty().bind(rootWrapper.widthProperty());
        blackScreen.heightProperty().bind(rootWrapper.heightProperty());
        transitionLayer.getChildren().add(blackScreen);

        Timeline phase1 = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(backgroundLayer.scaleXProperty(), 1.0),
                        new KeyValue(backgroundLayer.scaleYProperty(), 1.0),
                        new KeyValue(backgroundLayer.translateXProperty(), 0.0),
                        new KeyValue(backgroundLayer.translateYProperty(), 0.0),
                        new KeyValue(blackScreen.opacityProperty(), 0.0)
                ),
                new KeyFrame(Duration.seconds(0.7),
                        new KeyValue(backgroundLayer.scaleXProperty(), 6.0, Interpolator.EASE_IN),
                        new KeyValue(backgroundLayer.scaleYProperty(), 6.0, Interpolator.EASE_IN),
                        new KeyValue(backgroundLayer.translateXProperty(), panX, Interpolator.EASE_IN),
                        new KeyValue(backgroundLayer.translateYProperty(), panY, Interpolator.EASE_IN)
                ),
                new KeyFrame(Duration.seconds(0.4),
                        new KeyValue(blackScreen.opacityProperty(), 0.0)
                ),
                new KeyFrame(Duration.seconds(0.7),
                        new KeyValue(blackScreen.opacityProperty(), 1.0)
                )
        );

        phase1.setOnFinished(e -> {
            if (onPitchBlack != null) {
                onPitchBlack.run();
            }

            backgroundLayer.setTranslateX(0);
            backgroundLayer.setTranslateY(0);

            backgroundLayer.setScaleX(1.5);
            backgroundLayer.setScaleY(1.5);
            contentLayer.setScaleX(1.5);
            contentLayer.setScaleY(1.5);

            ScaleTransition stBg = new ScaleTransition(Duration.seconds(0.6), backgroundLayer);
            stBg.setToX(1.0); stBg.setToY(1.0);

            ScaleTransition stUI = new ScaleTransition(Duration.seconds(0.6), contentLayer);
            stUI.setToX(1.0); stUI.setToY(1.0);

            FadeTransition ft = new FadeTransition(Duration.seconds(0.3), blackScreen);
            ft.setToValue(0.0);

            ParallelTransition phase2 = new ParallelTransition(stBg, stUI, ft);
            phase2.setOnFinished(ev -> transitionLayer.getChildren().clear());
            phase2.play();
        });

        phase1.play();
    }

    public static void loadBoardScene() {
        Platform.runLater(() -> {
            try {
                // Schermo nero temporaneo per transizione fluida
                Rectangle blackScreen = new Rectangle();
                blackScreen.setFill(Color.BLACK);
                blackScreen.widthProperty().bind(rootWrapper.widthProperty());
                blackScreen.heightProperty().bind(rootWrapper.heightProperty());
                transitionLayer.getChildren().add(blackScreen);

                java.net.URL fxmlUrl = GUIApp.class.getResource("/fxml/Board.fxml");
                if (fxmlUrl == null) {
                    throw new RuntimeException("File /fxml/Board.fxml non trovato nel JAR! Controlla i percorsi.");
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent boardRoot = loader.load();

                changeBackground("board-background");
                changeSceneContent(boardRoot);

                FadeTransition ft = new FadeTransition(Duration.millis(300), blackScreen);
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                ft.setDelay(Duration.millis(450));
                ft.setOnFinished(e -> transitionLayer.getChildren().remove(blackScreen));
                ft.play();

                System.out.println("[GUI] Board caricata con successo.");
            } catch (Throwable e) {
                // SE CRASHA, ORA TI APPARE UN POP-UP VISIBILE CHE TI DICE IL PERCHÉ!
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Crash del Client");
                alert.setHeaderText("Impossibile caricare la Plancia (Board.fxml)");
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(sw.toString());
                area.setEditable(false);
                area.setWrapText(true);
                alert.getDialogPane().setExpandableContent(area);
                alert.getDialogPane().setExpanded(true);
                alert.showAndWait();
            }
        });
    }

    public static void loadLeaderboardScene() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(GUIApp.class.getResource("/fxml/Leaderboard.fxml"));
                Parent root = loader.load();
                changeBackground("board-background"); // Usa un background scuro base
                changeSceneContent(root);
                System.out.println("[GUI] Classifica Finale caricata con successo.");
            } catch (Exception e) {
                System.err.println("[GUI Errore] Impossibile caricare Leaderboard.fxml");
                e.printStackTrace();
            }
        });
    }

    public static void loadLoginScene() {
        Platform.runLater(() -> {
            try {
                // Ricrea tutto l'environment pulito per il login
                FXMLLoader loader = new FXMLLoader(GUIApp.class.getResource("/fxml/Login.fxml"));
                Parent loginRoot = loader.load();
                changeBackground("login-background");
                changeSceneContent(loginRoot);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}