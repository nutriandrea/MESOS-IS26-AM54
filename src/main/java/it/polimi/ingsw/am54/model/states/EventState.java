package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;

public class EventState extends GameState {
    @Override public String getStateName() { return "EventState"; }

    @Override
    public void resolvePhase(Game game){

        EventCard currentEvent;
        while ((currentEvent = game.getBoard().popNextEvent()) != null) {

            // ==========================================
            // INIEZIONE TRIGGER: On Event
            // Prima che l'evento colpisca, diamo la possibilità agli Edifici di preparare
            // la "cartella clinica" del giocatore (sconti, scudi dello sciamano, ecc.)
            // ==========================================
            for (it.polimi.ingsw.am54.model.Player p : game.getPlayers()) {
                for (it.polimi.ingsw.am54.model.BuildingCard building : p.getBuildings()) {
                    if (building.getEffects() != null) {
                        for (it.polimi.ingsw.am54.model.IEffect<it.polimi.ingsw.am54.model.BuildingCard> effect : building.getEffects()) {
                            effect.applyOnEvent(p, game, building, currentEvent);
                        }
                    }
                }
            }

            // RISOLVIAMO L'EVENTO
            // Ora l'evento calcolerà i danni/bonus leggendo i modificatori temporanei
            // appena impostati dagli Edifici, dopodiché pulirà tutto (resetEventAttributes).
            currentEvent.resolveEvent(game);

            game.notifyEventResolved(currentEvent);
        }

        // 3. Scarto tutte le carte (Personaggi) rimaste nella fila inferiore
        game.getBoard().clearLowerRow();

        // 4. Sposta Personaggi ed Eventi dalla fila superiore alla fila inferiore
        game.getBoard().moveUpperToLower();

        // 5. Riempi la fila superiore (delegato al Game)
        game.refillUpperTribeRow();

        // 6. INIZIO NUOVA ERA (Se la prossima carta nel deck è dell'era successiva)
        if (game.isEraTransition()) {
            game.advanceEra();

            // REGOLE: Se transizione a Era III -> scarta eventuali Edifici dalla fila inferiore
            if (game.getCurrentEra() == 3) {
                game.getBoard().discardLowerRowBuildings();
            }

            // Sposta gli Edifici dalla fila superiore alla fila inferiore
            game.getBoard().moveBuildings();

            // Piazza i nuovi Edifici dell'Era nella fila superiore
            game.refillUpperBuildings();
        }

        // 8. Controllo Condizione Fine Partita (Regola: Il mazzo Tribù è esaurito)
        game.advanceRound();

        // A. Resetta la flag dei totem per tutti i giocatori
        for(it.polimi.ingsw.am54.model.Player p : game.getPlayers()) {
            p.setHasPlacedTotem(false);
        }

        // B. Il nuovo primo giocatore è quello seduto nel primo slot dell'Ordine di Turno
        for (it.polimi.ingsw.am54.model.TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            if (slot.getOccupiedBy() != null) {
                game.setCurrentPlayer(slot.getOccupiedBy());
                break; // Trovato il primo, ci fermiamo
            }
        }
        for (it.polimi.ingsw.am54.model.OfferingTile tile : game.getBoard().getOfferingTrack()) {
            tile.freeSlot(); // Libera l'occupante
            tile.resetPicks();
        }

        game.setState(new StartTurnState());
    }
}