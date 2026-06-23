package it.polimi.ingsw.am54;

/**
 * Main class del JAR client. Esiste come livello intermedio che NON estende
 * {@link javafx.application.Application}: questo evita l'errore "JavaFX runtime
 * components are missing" quando si lancia il fat-jar con {@code java -jar}.
 * Delega subito a {@link Launcher#main(String[])}.
 */
public class MainLauncher {
    /**
     * Punto d'ingresso del client: inoltra gli argomenti al {@link Launcher}.
     *
     * @param args argomenti da riga di comando (es. {@code --cli} per la modalità testuale)
     */
    public static void main(String[] args) {
        Launcher.main(args);
    }
}