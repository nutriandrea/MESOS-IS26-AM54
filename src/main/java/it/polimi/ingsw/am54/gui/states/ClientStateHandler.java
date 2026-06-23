package it.polimi.ingsw.am54.gui.states;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Defines the UI behavior and interaction rules for a specific game state.
 * This interface is used by both the GUI and the CLI to determine which controls
 * are active, what instructions to display, and how to render the current phase.
 */
public interface ClientStateHandler {

    /**
     * Determines if the "End Turn" button should be visible in this state.
     *
     * @return true if the button should be shown, false otherwise
     */
    boolean showsEndTurnButton();

    /**
     * Retrieves the instruction message to display to the player.
     *
     * @param model  the current client model
     * @param myName the nickname of the local player
     * @return the instruction string
     */
    String getInstruction(ClientModel model, String myName);

    /**
     * Indicates whether the player can interact with the offering tiles
     * in this state (e.g., to place a totem).
     *
     * @return true if tile interaction is allowed, false otherwise
     */
    default boolean canInteractWithTiles() {
        return false;
    }

    /**
     * Identifies if the current state belongs to the pre-game lobby phase.
     *
     * @return true if it is a lobby state, false if it is an active gameplay state
     */
    default boolean isLobbyState() {
        return false;
    }

    /**
     * Determines whether user input should be blocked based on the current turn.
     *
     * @param isMyTurn true if it is the local player's turn
     * @return true if input should be blocked, false otherwise
     */
    default boolean shouldBlockInput(boolean isMyTurn) {
        return false;
    }

    /**
     * Triggers the appropriate rendering method on the provided CLI interface.
     *
     * @param cli the CLI renderer instance
     */
    default void renderCLI(CLIAbstract cli) {
        cli.renderDefault();
    }
}