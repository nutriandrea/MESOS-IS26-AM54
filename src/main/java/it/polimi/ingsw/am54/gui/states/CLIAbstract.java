package it.polimi.ingsw.am54.gui.states;

public interface CLIAbstract {
    void renderSetup();
    void renderStartTurn();
    void renderPicking();
    void renderEndGame();
    void renderEvent();
    void renderDefault();
}
