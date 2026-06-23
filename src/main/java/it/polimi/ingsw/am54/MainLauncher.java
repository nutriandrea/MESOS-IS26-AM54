package it.polimi.ingsw.am54;

/**
 * Main class of the client JAR. Exists as an intermediate layer that does NOT extend
 * {@link javafx.application.Application}: this avoids the "JavaFX runtime
 * components are missing" error when launching the fat-jar with {@code java -jar}.
 * Delegates immediately to {@link Launcher#main(String[])}.
 */
public class MainLauncher {
    /**
     * Client entry point: forwards the arguments to the {@link Launcher}.
     *
     * @param args command line arguments (e.g. {@code --cli} for textual mode)
     */
    public static void main(String[] args) {
        Launcher.main(args);
    }
}