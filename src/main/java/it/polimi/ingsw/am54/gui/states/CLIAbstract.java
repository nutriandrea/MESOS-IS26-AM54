package it.polimi.ingsw.am54.gui.states;

/**
 * Abstract class that defines the contract for CLI implementations
 * (command line interface). Each implementation must provide
 * specific rendering for the different game phases: setup, turn start,
 * picking, event, endgame and default behavior.
 *
 * @author AM54
 */
public interface CLIAbstract {
    /** Renders the setup/lobby phase of the game. */
    void renderSetup();
    /** Renders the start-of-turn phase. */
    void renderStartTurn();
    /** Renders the card-picking phase. */
    void renderPicking();
    /** Renders the end-game phase. */
    void renderEndGame();
    /** Renders the event-resolution phase. */
    void renderEvent();
    /** Renders a default fallback view when no specific phase applies. */
    void renderDefault();
}
