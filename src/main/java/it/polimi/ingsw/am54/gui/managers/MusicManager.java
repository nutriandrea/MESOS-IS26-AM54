package it.polimi.ingsw.am54.gui.managers;

import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.Map;

/**
 * Manages background audio playback (MP3) via JavaFX MediaPlayer.
 * <p>
 * Singleton — unique player for the entire application.
 * <p>
 * Mette i file MP3 in {@code src/main/resources/audio/}:
 * <ul>
 *   <li>{@code lobby.mp3} — music on the lobby screen</li>
 *   <li>{@code board.mp3} — music during the game</li>
 * </ul>
 *
 * @author AM54
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

    /**
     * Returns the singleton instance of this manager, creating it on first access.
     *
     * @return the shared {@code MusicManager} instance
     */
    public static synchronized MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }

    /**
     * Loads an MP3 file from resources. If the file does not exist, returns null.
     */
    private Media loadMedia(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            System.err.println("[MusicManager] File not found: " + resourcePath);
            return null;
        }
        return new Media(url.toString());
    }

    /**
     * Starts or stops the music.
     *
     * @param track  "lobby" o "board" — quale traccia riprodurre
     * @param button the button whose icon to change (🎵 / 🔊)
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

        // If it is a track change or first start
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

    /**
     * Returns the icon string representing the current playback state.
     *
     * @return "🔊" if music is playing, "🔇" otherwise
     */
    public String getCurrentIcon() {
        return isPlaying ? "🔊" : "🔇";
    }

    /**
     * Stops the music and releases the player.
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
