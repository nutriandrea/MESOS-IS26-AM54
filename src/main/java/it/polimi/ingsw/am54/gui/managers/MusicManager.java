package it.polimi.ingsw.am54.gui.managers;

import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.Map;

/**
 * Manages background audio playback (MP3) using JavaFX MediaPlayer.
 * <p>
 * Singleton — a single player instance for the entire application.
 * <p>
 * MP3 files must be placed in {@code src/main/resources/audio/}:
 * <ul>
 *   <li>{@code lobby.mp3} — lobby screen music</li>
 *   <li>{@code board.mp3} — in-game music</li>
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
     * Loads an MP3 file from the resources. Returns null if the file does not exist.
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
     * Starts or stops the music.
     *
     * @param track  "lobby" or "board" — which track to play
     * @param button the button whose icon will be updated
     */
    public void toggle(String track, Button button) {
        Media target = trackMedia.get(track);

        if (target == null) return;

        // If currently playing, stop it
        if (isPlaying && currentPlayer != null) {
            currentPlayer.stop();
            isPlaying = false;
            button.setText("🔇"); // Emoji updated
            return;
        }

        // If changing track or starting for the first time
        if (currentPlayer == null || !currentPlayer.getMedia().getSource().equals(target.getSource())) {
            if (currentPlayer != null) currentPlayer.dispose();
            currentPlayer = new MediaPlayer(target);
            currentPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            currentPlayer.setVolume(0.4);
        }

        currentPlayer.play();
        isPlaying = true;
        button.setText("🔊"); // Emoji updated
    }

    // Helper method to retrieve the current state when opening scenes
    public String getCurrentIcon() {
        return isPlaying ? "🔊" : "🔇";
    }
}