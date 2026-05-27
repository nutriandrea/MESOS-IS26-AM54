package it.polimi.ingsw.am54.gui.managers;

import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.Map;

/**
 * Gestisce la riproduzione audio di sottofondo (MP3) tramite JavaFX MediaPlayer.
 * <p>
 * Singleton — unico player per tutta l'applicazione.
 * <p>
 * Mette i file MP3 in {@code src/main/resources/audio/}:
 * <ul>
 *   <li>{@code lobby.mp3} — musica nella schermata lobby</li>
 *   <li>{@code board.mp3} — musica durante la partita</li>
 * </ul>
 */
public class MusicManager {

    private static MusicManager instance;

    private MediaPlayer currentPlayer;
    private boolean isPlaying = false;

    private final Map<String, Media> trackMedia;

    private MusicManager() {
        this.trackMedia = Map.of(
            "lobby", loadMedia("/audio/lobby.mp3"),
            "board", loadMedia("/audio/board.mp3")
        );
    }

    public static synchronized MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }

    /**
     * Carica un file MP3 dalle risorse. Se il file non esiste, restituisce null.
     */
    private Media loadMedia(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            System.err.println("[MusicManager] File non trovato: " + resourcePath);
            return null;
        }
        return new Media(url.toString());
    }

    /**
     * Avvia o ferma la musica.
     *
     * @param track  "lobby" o "board" — quale traccia riprodurre
     * @param button il pulsante su cui cambiare icona (🎵 / 🔊)
     */
    public void toggle(String track, Button button) {
        Media target = trackMedia.get(track);

        if (target == null) return;

        // Se sta suonando, ferma
        if (isPlaying && currentPlayer != null) {
            currentPlayer.stop();
            isPlaying = false;
            button.setText("🔇"); // Cambiato emoji
            return;
        }

        // Se è un cambio traccia o primo avvio
        if (currentPlayer == null || !currentPlayer.getMedia().getSource().equals(target.getSource())) {
            if (currentPlayer != null) currentPlayer.dispose();
            currentPlayer = new MediaPlayer(target);
            currentPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            currentPlayer.setVolume(0.4);
        }

        currentPlayer.play();
        isPlaying = true;
        button.setText("🔊"); // Cambiato emoji
    }

    // Aggiungi questo metodo per sapere lo stato all'apertura delle scene
    public String getCurrentIcon() {
        return isPlaying ? "🔊" : "🔇";
    }

    /**
     * Ferma la musica e rilascia il player.
     */
    public void stop() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.dispose();
            currentPlayer = null;
        }
        isPlaying = false;
    }
}
