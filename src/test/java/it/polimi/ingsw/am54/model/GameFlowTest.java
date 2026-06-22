package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.EventState;
import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di flusso end-to-end a livello di model: avvia una partita reale (mazzi e
 * plancia caricati dai JSON), piazza i totem, pesca le carte e risolve gli eventi
 * round dopo round. Guida la partita in modo difensivo fino alla fine o a un limite
 * di sicurezza, esercitando StartTurnState, PickingState ed EventState.
 *
 * Usa i nickname "tester"/"tester2" che abilitano il bypass DEV della pesca
 * obbligatoria, così il turno può sempre essere chiuso senza stallo.
 */
public class GameFlowTest {

    @Test
    public void testFullGameFlowDoesNotDeadlock() throws Exception {
        Game game = new Game();
        addPlayer(game, "tester", TotemColors.RED);
        addPlayer(game, "tester2", TotemColors.BLUE);

        game.setupGame();
        assertTrue(game.getCurrentState() instanceof StartTurnState);

        boolean reachedPicking = false;
        boolean resolvedEvent = false;

        int guard = 0;
        while (guard++ < 500) {
            GameState state = game.getCurrentState();

            if (state.isGameOver()) {
                break;
            } else if (state instanceof StartTurnState) {
                Player current = game.getCurrentPlayer();
                if (current == null) break;
                placeForCurrent(game, current);
            } else if (state instanceof PickingState) {
                reachedPicking = true;
                Player current = game.getCurrentPlayer();
                if (current == null) break;
                pickOrEnd(game, current);
            } else if (state instanceof EventState) {
                state.resolvePhase(game);
                resolvedEvent = true;
            } else {
                break;
            }
        }

        assertTrue(reachedPicking, "La partita deve raggiungere la fase di pesca");
        assertTrue(resolvedEvent, "Deve risolvere almeno una fase evento (fine round)");
        assertTrue(guard < 500, "La partita non deve andare in stallo infinito");
    }

    /** Piazza il totem del giocatore corrente sulla prima offering tile libera. */
    private void placeForCurrent(Game game, Player current) {
        if (current.hasPlacedTotem()) {
            // Non dovrebbe accadere, ma evitiamo lo stallo passando il turno
            try { game.nextPlayer(); } catch (Exception ignored) {}
            return;
        }
        for (int i = 0; i < game.getBoard().getOfferingTrack().size(); i++) {
            if (game.getBoard().getOfferingTrack().get(i).isAvailable()) {
                try {
                    game.placeTotem(current.getNickname(), i);
                    return;
                } catch (Exception ignored) {
                    // tessera contesa, prova la prossima
                }
            }
        }
        // Nessuna tessera piazzabile: forziamo l'avanzamento
        try { game.nextPlayer(); } catch (Exception ignored) {}
    }

    /** Prova a pescare una carta valida; se non possibile, chiude il turno (bypass tester). */
    private void pickOrEnd(Game game, Player current) {
        String nick = current.getNickname();
        // Prova upper, poi lower, su un piccolo range di indici
        for (String row : new String[]{"upper", "lower"}) {
            for (int idx = 0; idx < 3; idx++) {
                try {
                    game.pickCard(nick, row, idx);
                    return;
                } catch (Exception ignored) {
                    // indice/riga non valida o pick esaurito: continua
                }
            }
        }
        try {
            game.endTurn(nick);
        } catch (Exception ignored) {
            // se anche endTurn fallisce, forza il passaggio per evitare stallo
            try { game.endTurn(nick); } catch (Exception ignored2) {}
        }
    }

    private void addPlayer(Game game, String nick, TotemColors color) {
        Player p = new Player();
        p.setNickname(nick);
        p.setTotemColor(color);
        game.getPlayers().add(p);
    }
}
