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

        // CALCOLO PUNTEGGI
        List<Player> winners = game.determineWinners();

        // NOTIFICA VITTORIA
        game.notifyEndGame(winners);
    }

    @Override
    public void joinGame(Game game, String nickname) throws Exception {
        System.out.println("[SERVER] Riavvio della partita richiesto dal client: " + nickname);

        // Svuotiamo la lista dei vecchi giocatori e resettiamo lo stato iniziale
        game.getPlayers().clear();
        game.setState(new SetUpGameState());

        // Ora che il server è pulito, passiamo la palla al nuovo stato per farlo entrare come Host!
        game.getCurrentState().joinGame(game, nickname);
    }

}