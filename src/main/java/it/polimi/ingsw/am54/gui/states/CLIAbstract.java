package it.polimi.ingsw.am54.gui.states;

/**
 * Interface defining the rendering methods for the Command Line Interface (CLI).
 * Each method corresponds to a specific game phase, allowing the CLI to update
 * its textual output according to the current game state.
 */
public interface CLIAbstract {
    void renderSetup();
    void renderStartTurn();
    void renderPicking();
    void renderEndGame();
    void renderEvent();
    void renderDefault();
}