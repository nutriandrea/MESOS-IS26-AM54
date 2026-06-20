package it.polimi.ingsw.am54.model;

/**
 * Abstract class that represents the game state.
 * Every method throws IllegalStateException by default.
 * Concrete states only override the allowed methods.
 */
public abstract class GameState {

    /**
     * Handles a join game request.
     * Default implementation throws because joining is not allowed in this state.
     *
     * @param game     the game instance
     * @param nickname the nickname of the player joining
     * @throws GameException if the action is not allowed in the current state
     */
    public void joinGame(Game game, String nickname) throws GameException {
        throw new IllegalStateException("You cannot join the game in this phase");
    }

    /**
     * Handles a totem color choice request.
     * Default implementation throws because color choice is not allowed in this state.
     *
     * @param game     the game instance
     * @param nickname the player's nickname
     * @param color    the chosen totem color
     * @throws GameException if the action is not allowed in the current state
     */
    public void chooseTotemColor(Game game, String nickname, String color) throws GameException {
        throw new IllegalStateException("You cannot choose the color in this phase");
    }

    /**
     * Handles a game setup request.
     * Default implementation throws because setup is not allowed in this state.
     *
     * @param game the game instance
     * @throws GameException if the action is not allowed in the current state
     */
    public void setupGame(Game game) throws GameException {
        throw new IllegalStateException("You cannot start the setup in this phase");
    }

    /**
     * Handles a totem placement request.
     * Default implementation throws because totem placement is not allowed in this state.
     *
     * @param game          the game instance
     * @param nickname      the player's nickname
     * @param offerPosition the offering tile position
     * @throws GameException if the action is not allowed in the current state
     */
    public void placeTotem(Game game, String nickname, int offerPosition) throws GameException {
        throw new IllegalStateException("You cannot place the totem in this phase");
    }

    /**
     * Handles a card pick request.
     * Default implementation throws because card picking is not allowed in this state.
     *
     * @param game      the game instance
     * @param nickname  the player's nickname
     * @param row       the row to draw from ("upper" or "lower")
     * @param cardIndex the index of the card in the row
     * @throws GameException if the action is not allowed in the current state
     */
    public void pickCard(Game game, String nickname, String row, int cardIndex) throws GameException {
        throw new IllegalStateException("You cannot draw cards in this phase");
    }

    /**
     * Handles an end turn request.
     * Default implementation throws because ending the turn is not allowed in this state.
     *
     * @param game     the game instance
     * @param nickname the player's nickname
     * @throws GameException if the action is not allowed in the current state
     */
    public void endTurn(Game game, String nickname) throws GameException {
        throw new IllegalStateException("You cannot end the turn in this phase");
    }

    /**
     * Resolves the current phase (e.g. event resolution).
     * Default implementation throws because phase resolution is not allowed in this state.
     *
     * @param game the game instance
     * @throws GameException if the action is not allowed in the current state
     */
    public void resolvePhase(Game game) throws GameException {
        throw new IllegalStateException("You cannot resolve the phase at this time");
    }

    /**
     * Handles a skip turn request.
     * Default implementation throws because skipping is not allowed in this state.
     *
     * @param game     the game instance
     * @param nickname the player's nickname
     * @throws GameException if the action is not allowed in the current state
     */
    public void skipTurn(Game game, String nickname) throws GameException {
        throw new IllegalStateException("You cannot skip the turn in this phase");
    }

    /**
     * Returns whether disconnection is blocked in this state.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if disconnection is blocked
     */
    public boolean isDisconnectBlocked() {
        return false;
    }

    /**
     * Returns whether player removal is permanent in this state.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if removal is permanent
     */
    public boolean isRemovePermanent() {
        return false;
    }

    /**
     * Returns whether this state can be auto-resolved without player input.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if the state is auto-resolvable
     */
    public boolean isAutoResolvable() {
        return false;
    }

    /**
     * Returns whether the game is over in this state.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if the game is over
     */
    public boolean isGameOver() {
        return false;
    }

    /**
     * Returns whether the game should reset to the setup state when all players leave.
     * Default implementation returns {@code false}.
     *
     * @return {@code true} if the game should reset to setup when empty
     */
    public boolean shouldResetToSetupOnEmpty() {
        return false;
    }

    /**
     * Recovers the active player after a disconnection or state transition.
     * Default implementation returns {@code null}.
     *
     * @param game the game instance
     * @return the recovered active player, or {@code null} if not applicable
     */
    public Player recoverActivePlayer(Game game) {
        return null;
    }

    /**
     * Handles a player disconnection event.
     * Default implementation does nothing; concrete states override as needed.
     *
     * @param game            the game instance
     * @param p               the disconnecting player
     * @param isCurrentPlayer whether the disconnecting player is the current player
     * @throws GameException if disconnection cannot be handled in this state
     */
    public void handleDisconnection(Game game, Player p, boolean isCurrentPlayer) throws GameException {
    }
}
