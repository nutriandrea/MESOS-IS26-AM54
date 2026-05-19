package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.BuildingCard;
import it.polimi.ingsw.am54.model.IEffect;

import java.util.List;

//Stato di fine partita: calcola i punteggi finali e decreta il vincitore
//Nessun giocatore puo' pescare o piazzare totem
public class EndGameState extends GameState {

    @Override
    public void resolvePhase(Game game) {
        EventCard currentFinalEvent;

        // Cicliamo finché la Board ci restituisce eventi
        while ((currentFinalEvent = game.getBoard().popNextVisibleEvent()) != null) {

            // ==========================================
            // INIEZIONE TRIGGER: On Event (Per gli Eventi Finali)
            // ==========================================
            for (Player p : game.getPlayers()) {
                for (BuildingCard building : p.getBuildings()) {
                    if (building.getEffects() != null) {
                        for (IEffect<BuildingCard> effect : building.getEffects()) {
                            effect.applyOnEvent(p, game, building, currentFinalEvent);
                        }
                    }
                }
            }

            // Risolviamo l'evento (modifica i punteggi/cibo dei giocatori)
            currentFinalEvent.resolveEvent(game);

            game.notifyEventResolved(currentFinalEvent);
        }

        // ==========================================
        // INIEZIONE TRIGGER IL GRAN FINALE: On End Game
        // Facciamo un giro su tutti i giocatori per attivare i poteri degli
        // Edifici che assegnano i Punti Aura finali prima del conteggio!
        // ==========================================
        for (Player p : game.getPlayers()) {
            for (BuildingCard building : p.getBuildings()) {
                if (building.getEffects() != null) {
                    for (IEffect<BuildingCard> effect : building.getEffects()) {
                        effect.applyOnEndGame(p, game, building);
                    }
                }
            }
        }

        // CALCOLO PUNTEGGI
        List<Player> winners = game.determineWinners();

        // NOTIFICA VITTORIA
        game.notifyEndGame(winners);
    }
}