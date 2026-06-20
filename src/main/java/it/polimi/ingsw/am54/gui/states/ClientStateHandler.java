package it.polimi.ingsw.am54.gui.states;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Abstract handler for managing client states.
 * Defines the contract for managing the various game states,
 * providing methods to get textual instructions, check whether to show
 * the end turn button, control interaction with offering
 * tiles and block input when it is not the player's turn.
 *
 * @author AM54
 */
public interface ClientStateHandler {
    /**
     * Returns whether the "end turn" button should be visible in this state.
     *
     * @return {@code true} if the end-turn button should be shown
     */
    boolean showsEndTurnButton();

    /**
     * Returns the human-readable instruction text for the current player.
     *
     * @param model  the current client model
     * @param myName the nickname of the local player
     * @return instruction text to display
     */
    String getInstruction(ClientModel model, String myName);

    /**
     * Returns whether the player may interact with offering tiles in this state.
     *
     * @return {@code true} if tile interaction is allowed; default {@code false}
     */
    default boolean canInteractWithTiles() {
        return false;
    }

    /**
     * Returns whether this state belongs to the lobby (pre-game) phase.
     *
     * @return {@code true} if this is a lobby state; default {@code false}
     */
    default boolean isLobbyState() {
        return false;
    }

    /**
     * Returns whether player input should be blocked when it is not their turn.
     *
     * @param isMyTurn whether it is currently the local player's turn
     * @return {@code true} if input should be blocked; default {@code false}
     */
    default boolean shouldBlockInput(boolean isMyTurn) {
        return false;
    }

    /**
     * Delegates CLI rendering to the appropriate method on the given CLI view.
     *
     * @param cli the CLI renderer to invoke
     */
    default void renderCLI(CLIAbstract cli) {
        cli.renderDefault();
    }
}
