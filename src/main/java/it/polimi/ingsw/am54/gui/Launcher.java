package it.polimi.ingsw.am54.gui;

/**
 * Entry point of the JavaFX application.
 * Launches the GUI by calling the main of {@link GUIApp}.
 *
 * @author AM54
 */
public class Launcher {
    /**
     * Application entry point. Delegates to {@link GUIApp#main} to
     * bootstrap the JavaFX GUI.
     *
     * @param args command-line arguments forwarded to the JavaFX launcher
     */
    public static void main(String[] args) {
        // Trick Java: we start from here, then call the graphics!
        GUIApp.main(args);
    }
}