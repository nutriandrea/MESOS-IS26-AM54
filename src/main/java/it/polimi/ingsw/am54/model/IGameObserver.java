package it.polimi.ingsw.am54.model;

import java.util.List;

/**
 * Observer interface for the game state observation pattern.
 * <p>
 * Implemented by Views (GUI, CLI, RMI, WebSocket) to receive timely
 * notifications about events occurring during the game. Each method
 * corresponds to a specific event in the game lifecycle: setup,
 * player actions, turn changes, event resolution and endgame.
 * </p>
 * <p>
 * Observer registration occurs via the method
 * {@link Game#addObserver(IGameObserver)}. Management is thread-safe thanks
 * to the use of {@code CopyOnWriteArrayList} within the {@link Game} class.
 * </p>
 *
 * @see Game#addObserver(IGameObserver)
 * @see Game#removeObserver(IGameObserver)
 */
public interface IGameObserver {

    // ========== SETUP EVENTS ==========

    /**
     * Notifies that a new player has joined the game.
     *
     * @param game   the game instance the player joined
     * @param player the player who joined
     */
    void onPlayerJoined(Game game, Player player);

    /**
     * Notifies that a player has chosen their totem color.
     *
     * @param game   the game instance
     * @param player the player who chose the color
     * @param color  the selected totem color
     */
    void onTotemColorChosen(Game game, Player player, TotemColors color);

    // ========== GAME EVENTS ==========

    /**
     * Notifies that a player has placed their totem on the offering track.
     *
     * @param game          the game instance
     * @param player        the player who placed the totem
     * @param offerPosition the position (index) of the occupied offering tile
     */
    void onTotemPlaced(Game game, Player player, int offerPosition);

    /**
     * Notifies that a player has drawn a card from one of the board rows.
     *
     * @param game            the game instance
     * @param player          the player who drew
     * @param row             the row from which the card was drawn ({@code "upper"} or {@code "lower"})
     * @param cardIndex       the index of the drawn card within the row
     * @param newFoodBalance  the player's food balance after the draw
     * @param newMagicBalance the player's magic points balance after the draw
     */
    void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance);

    /**
     * Notifies that a player has returned the totem and received the bonus
     * from the turn order track, ending their turn.
     *
     * @param game           the game instance
     * @param player         the player who ended the turn
     * @param position       the position on the turn order track
     * @param newFoodBalance the food balance after the turn bonus resolution
     * @param newAuraBalance the aura balance after the turn bonus resolution
     */
    void onTotemReturned(Game game, Player player, int position, int newFoodBalance, int newAuraBalance);

    // ========== FLOW EVENTS (TURNS AND STATES) ==========

    /**
     * Notifies that the turn has passed to a new player.
     *
     * @param game              the game instance
     * @param newCurrentPlayer the player who is now the current one
     */
    void onTurnChanged(Game game, Player newCurrentPlayer);

    /**
     * Notifies that the game state has changed (e.g. from Setup to StartTurn, from StartTurn to PlaceTotem, etc.).
     *
     * @param game     the game instance
     * @param newState the new game state
     */
    void onStateChanged(Game game, GameState newState);

    /**
     * Notifies that a global Event card has been resolved.
     *
     * @param game      the game instance
     * @param eventCard the Event card that was resolved
     */
    void onEventResolved(Game game, EventCard eventCard);

    // ========== FINAL EVENT ==========

    /**
     * Notifies that the game has ended and communicates the leaderboard of winners.
     * <p>
     * In case of a tie, the list contains multiple players; the tie-break is already
     resolved by {@link Game#determineWinners()} based on remaining food.
     * </p>
     *
     * @param game    the game instance
     * @param winners list of winning players (one or more in case of a tie)
     */
    void onEndGame(Game game, List<Player> winners);
}