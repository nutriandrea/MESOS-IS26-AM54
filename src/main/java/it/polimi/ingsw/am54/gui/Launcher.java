package it.polimi.ingsw.am54.gui;

/**
 * Entry point class for the GUI application.
 * Required to bypass Java module path restrictions when launching packaged JAR files.
 */
public class Launcher {
    public static void main(String[] args) {
        // Trick Java: start from here, and then call the GUI app!
        GUIApp.main(args);
    }
}