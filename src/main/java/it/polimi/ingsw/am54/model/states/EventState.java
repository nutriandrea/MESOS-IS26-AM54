package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;

public class EventState extends GameState {

    @Override
    public void resolvePhase(Game game) throws Exception {

        EventCard currentEvent;
        while ((currentEvent = game.getBoard().popNextEvent()) != null) {

            //RISOLVIAMO L'EVENTO
            // Questo modificherà le statistiche dei giocatori (Cibo, PP)
            currentEvent.resolveEvent(game);

            game.notifyEventResolved();
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
        if (game.isTribeDeckEmpty()) {
            game.setState(new EndGameState());
        } else {
            // Se c'è ancora gioco, passiamo il testimone al piazzamento Totem!
            game.advanceRound();
            game.setState(new StartTurnState());
        }
    }
}
