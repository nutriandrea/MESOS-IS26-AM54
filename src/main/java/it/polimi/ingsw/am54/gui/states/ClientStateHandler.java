package it.polimi.ingsw.am54.gui.states;

import it.polimi.ingsw.am54.network.client.ClientModel;

public interface ClientStateHandler {
    boolean showsEndTurnButton();
    String getInstruction(ClientModel model, String myName);

    default boolean canInteractWithTiles() {
        return false;
    }

    default boolean isLobbyState() {
        return false;
    }

    default boolean shouldBlockInput(boolean isMyTurn) {
        return false;
    }

    default void renderCLI(CLIAbstract cli) {
        cli.renderDefault();
    }
}
