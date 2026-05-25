package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class EndGameStateTest {

    private Game game;
    private Player p1;
    private EndGameState endState;

    @BeforeEach
    public void setUp() {
        game = new Game();
        Board board = new Board();
        game.setBoard(board);

        p1 = new Player();
        p1.setNickname("Alice");
        game.getPlayers().add(p1);

        endState = new EndGameState();
        game.setState(endState);
    }

    @Test
    public void testResolvePhase_ProcessesEventsAndEndsGame() {
        // ==========================================
        // 1. SETUP EVENTO FITTIZIO (Mock)
        // ==========================================
        final boolean[] eventResolved = {false};

        // Creiamo un evento che accende la nostra "lampadina di test" quando viene risolto
        EventCard mockEvent = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override
            public void resolveEvent(Game g) {
                eventResolved[0] = true; // Il test capirà che questa riga è stata eseguita
            }
        };

        // Aggiungiamo l'evento alla board.
        // In EndGameState gli eventi rimasti in UPPER devono essere svuotati!
        game.getBoard().addEventCard(mockEvent, "upper");

        // ==========================================
        // 2. SETUP OBSERVER PER LE NOTIFICHE
        // ==========================================
        final boolean[] eventNotified = {false};
        final boolean[] endGameNotified = {false};

        game.addObserver(new IGameObserver() {
            @Override public void onPlayerJoined(Game game, Player player) {}
            @Override public void onTotemColorChosen(Game game, Player player, TotemColors color) {}
            @Override public void onTotemPlaced(Game game, Player player, int offerPosition) {}
            @Override public void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {}
            @Override public void onTurnChanged(Game game, Player player) {}
            @Override public void onStateChanged(Game game, GameState newState) {}
            @Override public void onTotemReturned(Game game, Player player, int position, int f, int a) {}

            // INTERCETTIAMO LE DUE NOTIFICHE DELL'ENDGAMESTATE
            @Override
            public void onEventResolved(Game game, EventCard eventCard) {
                eventNotified[0] = true;
            }

            @Override
            public void onEndGame(Game game, List<Player> winnersList) {
                endGameNotified[0] = true;
                // Alice gioca da sola in questo test, quindi vincerà sicuramente lei
                assertEquals(1, winnersList.size());
                assertEquals(p1, winnersList.get(0));
            }
        });

        // ==========================================
        // 3. ESECUZIONE
        // ==========================================
        endState.resolvePhase(game);

        // ==========================================
        // 4. VERIFICHE
        // ==========================================

        // A. Il ciclo while è stato eseguito correttamente
        assertTrue(eventResolved[0], "Il metodo resolveEvent della carta deve essere stato chiamato all'interno del while");
        assertTrue(eventNotified[0], "La notifica onEventResolved deve essere scattata");

        // B. La plancia è stata svuotata
        assertNull(game.getBoard().popNextVisibleEvent(), "Tutti gli eventi devono essere stati rimossi dalla plancia a fine partita");

        // C. Calcolo vincitori e notifica finale
        assertTrue(endGameNotified[0], "La notifica onEndGame deve essere scattata con successo");
    }
}