package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameException;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.BuildingCard;
import it.polimi.ingsw.am54.model.IEffect;

import java.util.List;

/**
 * Manages the game conclusion phase.
 * Resolves the last visible events, applies endgame effects
 * of all buildings and determines the winners based on accumulated Aura points.
 *
 * @author AM54
 */
public class EndGameState extends GameState {

    /**
     * Resolves the endgame phase by processing remaining visible events,
     * applying endgame building effects to all players, and determining the winners.
     *
     * @param game the current game instance
     */
    @Override
    public void resolvePhase(Game game) {
        EventCard currentFinalEvent;

        while ((currentFinalEvent = game.getBoard().popNextVisibleEvent()) != null) {

            for (Player p : game.getPlayers()) {
                for (BuildingCard building : p.getBuildings()) {
                    if (building.getEffects() != null) {
                        for (IEffect<BuildingCard> effect : building.getEffects()) {
                            effect.applyOnEvent(p, game, building, currentFinalEvent);
                        }
                    }
                }
            }

            currentFinalEvent.resolveEvent(game);

            game.notifyEventResolved(currentFinalEvent);
        }

        for (Player p : game.getPlayers()) {
            int auraBefore = p.getAuraPoints(); // Register lo stato dell'Aura a fine round 10 post-eventi

            for (BuildingCard building : p.getBuildings()) {
                if (building.getEffects() != null) {
                    for (IEffect<BuildingCard> effect : building.getEffects()) {
                        effect.applyOnEndGame(p, game, building);
                    }
                }
            }

            int auraAfter = p.getAuraPoints(); // Register l'Aura modificata dagli effetti degli edifici
            p.setEndGameBuildingsAura(auraAfter - auraBefore); // Salviamo il delta esatto calcolato dal Server
        }

        List<Player> winners = game.determineWinners();

        game.notifyEndGame(winners);
    }

    /**
     * Handles a rejoin request after the game has ended by clearing all players
     * and transitioning back to the setup phase.
     *
     * @param game     the current game instance
     * @param nickname the nickname of the client requesting the restart
     * @throws GameException if the restart fails during setup
     */
    @Override
    public void joinGame(Game game, String nickname) throws GameException {
        System.out.println("[SERVER] Riavvio della partita richiesto dal client: " + nickname);

        game.getPlayers().clear();
        game.setState(new SetUpGameState());

        game.getCurrentState().joinGame(game, nickname);
    }

    /**
     * Indicates whether permanent game elements should be removed in this state.
     *
     * @return {@code true} as the game has ended and permanent elements are cleared
     */
    @Override
    public boolean isRemovePermanent() {
        return true;
    }

    /**
     * Indicates whether player disconnection is blocked in this state.
     *
     * @return {@code true} as disconnections are not allowed during endgame resolution
     */
    @Override
    public boolean isDisconnectBlocked() {
        return true;
    }

    /**
     * Indicates whether this state can be resolved automatically without player input.
     *
     * @return {@code true} as the endgame phase requires no player interaction
     */
    @Override
    public boolean isAutoResolvable() {
        return true;
    }

    /**
     * Indicates whether the game is over.
     *
     * @return {@code true} as this state represents the game conclusion
     */
    @Override
    public boolean isGameOver() {
        return true;
    }

    /**
     * Indicates whether the game should reset to the setup state when no players remain.
     *
     * @return {@code true} to allow a new game to be started after the room empties
     */
    @Override
    public boolean shouldResetToSetupOnEmpty() {
        return true;
    }
}
