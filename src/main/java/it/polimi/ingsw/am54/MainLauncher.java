package it.polimi.ingsw.am54;

public class MainLauncher {
    public static void main(String[] args) {
        // Questo passaggio intermedio "inganna" JavaFX
        // e gli permette di avviarsi correttamente all'interno di un JAR!
        Launcher.main(args);
    }
}