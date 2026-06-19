package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameException;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.BuildingCard;
import it.polimi.ingsw.am54.model.IEffect;

import java.util.List;

public class EndGameState extends GameState {

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
            int auraBefore = p.getAuraPoints(); // Registriamo lo stato dell'Aura a fine round 10 post-eventi

            for (BuildingCard building : p.getBuildings()) {
                if (building.getEffects() != null) {
                    for (IEffect<BuildingCard> effect : building.getEffects()) {
                        effect.applyOnEndGame(p, game, building);
                    }
                }
            }

            int auraAfter = p.getAuraPoints(); // Registriamo l'Aura modificata dagli effetti degli edifici
            p.setEndGameBuildingsAura(auraAfter - auraBefore); // Salviamo il delta esatto calcolato dal Server
        }

        List<Player> winners = game.determineWinners();

        game.notifyEndGame(winners);
    }

    @Override
    public void joinGame(Game game, String nickname) throws GameException {
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
