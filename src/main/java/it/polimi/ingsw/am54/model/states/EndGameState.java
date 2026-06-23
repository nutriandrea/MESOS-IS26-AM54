package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.BuildingCard;
import it.polimi.ingsw.am54.model.IEffect;

import java.util.List;

public class EndGameState extends GameState {

    /**
     * Resolves the final phase of the game: applies the remaining final events and the
     * endgame effects of the buildings, records the aura delta produced, calculates
     * the winners and notifies the clients.
     *
     * @param game the game to conclude
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
            int auraBefore = p.getAuraPoints(); // Records the Aura state at the end of round 10 post-events

            for (BuildingCard building : p.getBuildings()) {
                if (building.getEffects() != null) {
                    for (IEffect<BuildingCard> effect : building.getEffects()) {
                        effect.applyOnEndGame(p, game, building);
                    }
                }
            }

            int auraAfter = p.getAuraPoints(); // Records the Aura modified by the building effects
            p.setEndGameBuildingsAura(auraAfter - auraBefore); // Saves the exact delta calculated by the Server
        }

        List<Player> winners = game.determineWinners();

        game.notifyEndGame(winners);
    }

    /**
     * A new entry after the game is over starts a NEW game: clears the players,
     * resets the state to {@link SetUpGameState} and forwards the join to the setup.
     *
     * @param game     the completed game
     * @param nickname the player requesting the restart
     * @throws Exception in case of an error joining
     */
    @Override
    public void joinGame(Game game, String nickname) throws Exception {
        System.out.println("[SERVER] Riavvio della partita richiesto dal client: " + nickname);

        game.getPlayers().clear();
        game.setState(new SetUpGameState());

        game.getCurrentState().joinGame(game, nickname);
    }

    @Override
    public boolean isRemovePermanent() {
        return true;
    }

    @Override
    public boolean isDisconnectBlocked() {
        return true;
    }

    @Override
    public boolean isAutoResolvable() {
        return true;
    }

    @Override
    public boolean isGameOver() {
        return true;
    }

    @Override
    public boolean shouldResetToSetupOnEmpty() {
        return true;
    }
}