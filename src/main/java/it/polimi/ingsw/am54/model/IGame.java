package it.polimi.ingsw.am54.model;
/**
 * Public interface of the game, exposed for network communication.
 * @author AM54
 */
public interface IGame {

    /**
     * Allows a new player to join the game.
     *
     * @param nickname the name of the player to add
     * @throws GameException if the nickname is already taken or the game has already started
     */
    void joinGame(String nickname) throws GameException;

    /**
     * Assigns a totem color to the specified player.
     *
     * @param nickname the player's name
     * @param color    the chosen color (e.g. "RED", "BLUE")
     * @throws GameException if the color is not available or the phase does not allow it
     */
    void chooseTotemColor(String nickname, String color) throws GameException;

    /**
     * Places the player's totem on an offering tile on the track.
     *
     * @param nickname       the player's name
     * @param offerPosition  the position of the offering tile (numeric index)
     * @throws GameException if the position is invalid or the tile is already occupied
     */
    void placeTotem(String nickname, int offerPosition) throws GameException;

    /**
     * Allows the player to draw a card from one of the table rows.
     *
     * @param nickname  the player's name
     * @param row       the row to draw from ("upper" or "lower")
     * @param cardIndex the index of the card in the row
     * @throws GameException if the card is not purchasable or the player is not allowed to draw
     */
    void pickCard(String nickname, String row, int cardIndex) throws GameException;

    /**
     * Ends the turn of the specified player.
     *
     * @param nickname the name of the player ending their turn
     * @throws GameException if it is not the player's turn or the action is not allowed
     */
    void endTurn(String nickname) throws GameException;

    /**
     * Executes the complete initial game setup (decks, board, turn order).
     *
     * @throws GameException if an error occurs during setup
     */
    void setupGame() throws GameException;

    /**
     * Registers an observer to receive updates on the game state.
     *
     * @param observer the observer to add
     */
    void addObserver(IGameObserver observer);

    /**
     * Removes an observer from the list, stopping notifications.
     *
     * @param observer the observer to remove
     */
    void removeObserver(IGameObserver observer);
}