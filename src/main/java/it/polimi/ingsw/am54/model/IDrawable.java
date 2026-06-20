package it.polimi.ingsw.am54.model;


/**
 * Interface for game elements that can be drawn (revealed)
 * and visually positioned on the game board ({@link Board}).
 * <p>
 * Implemented mainly by game cards ({@link Card} and subclasses)
 * to manage their appearance in the board rows (upper or lower).
 * The method {@link #reveal(Board, String)} encapsulates the placement logic:
 * depending on the card type, the element will be inserted in the appropriate row
 * or, in case of conflict (e.g. Events drawn during refill), may
 * be sent back to the deck.
 * </p>
 *
 * @see Board
 * @see Card#reveal(Board, String)
 * @see TribeCard#reveal(Board, String)
 */
public interface IDrawable {

    /**
     * Reveals and positions this element on the game board in the specified row.
     * <p>
     * The specific behavior depends on the concrete implementation:
     * Character cards are inserted directly into the indicated row,
     * while Event cards are generally put back in the deck if drawn
     * during row refill (because events must be resolved
     * only when they appear as the first element of the row at the right turn).
     * </p>
     *
     * @param board the game board on which to position the element
     * @param row   the destination row ({@code "upper"} for the upper row,
     *              {@code "lower"} for the lower row)
     */
    void reveal(Board board, String row);
}