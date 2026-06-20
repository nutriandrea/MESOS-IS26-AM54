package it.polimi.ingsw.am54.model;

/**
 * Custom exception for handling game errors.
 * Thrown by various states and methods of the model to signal
 * rule violations, invalid actions or error conditions
 * during the game.
 * @author AM54
 */
public class GameException extends Exception {
    /**
     * Constructs a GameException with the specified detail message.
     *
     * @param message the detail message describing the game error
     */
    public GameException(String message) {
        super(message);
    }
}
