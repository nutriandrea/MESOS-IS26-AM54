package it.polimi.ingsw.am54.model.states;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import it.polimi.ingsw.am54.model.*;

import java.util.ArrayList;

public class ExtraPickIntegrationTest {

    private Game game;
    private Player p1;
    private Player p2;

    @BeforeEach
    public void setUp() throws Exception {
        game = new Game();
        game.setBoard(new Board());

        game.setTribeDeck(new Deck<>());
        game.setBuildingDeck(new Deck<>());

        p1 = new Player();
        p1.setNickname("Alice");

        p2 = new Player();
        p2.setNickname("Bob");

        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        game.getBoard().initializeLayout(2);

        p1.modifyFood(50);
        p2.modifyFood(50);

        for (int i = 0; i < 5; i++) {
            game.getBoard().getCharacterUpperRow().add(
                    new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1)
            );
            game.getBoard().getCharacterLowerRow().add(
                    new CharacterCard(1, JobType.HUNTER, 0, null, new ArrayList<>(), 1)
            );
        }
    }

    @Test
    public void testExtraPick_FlussoDoppiaPescata() throws Exception {
        game.setState(new PickingState());

        // ==========================================
        // FIX: Creiamo due tessere "su misura" per il test!
        // Vogliamo essere sicuri al 100% che diano 1 pescata in UPPER.
        // ==========================================
        OfferingTile tileAlice = new OfferingTile('A', 1, 0, 0);
        OfferingTile tileBob = new OfferingTile('B', 1, 0, 0);

        // Inseriamole forzatamente nella Board al posto di quelle originali
        game.getBoard().getOfferingTrack().set(0, tileAlice);
        game.getBoard().getOfferingTrack().set(1, tileBob);

        // PIAZZIAMO ALICE
        tileAlice.occupy(p1);
        p1.setCurrentTile(tileAlice);
        p1.setHasPlacedTotem(true);
        p1.setExtraPick(true); // Diamo il bonus ad Alice!

        // PIAZZIAMO BOB
        tileBob.occupy(p2);
        p2.setCurrentTile(tileBob);
        p2.setHasPlacedTotem(true);

        // ==========================================
        // ACT & ASSERT: IL FLUSSO DELLA PARTITA
        // ==========================================

        // TURNO 1: Prima pescata di Alice
        assertEquals("Alice", game.getCurrentPicker().getNickname());
        game.pickCard("Alice", "upper", 0);

        // VERIFICA 1: Alice ha usato il pick della TESSERA. Il bonus Extra Pick è ancora in tasca!
        assertTrue(p1.hasExtraPick(), "Il bonus Extra Pick NON deve essere ancora consumato");
        assertEquals("Alice", game.getCurrentPicker().getNickname(), "Deve toccare ancora ad Alice grazie all'Extra Pick");
        assertEquals(1, p1.getMyTribe().size());

        // TURNO 2: Seconda pescata di Alice (Qui usa il bonus!)
        game.pickCard("Alice", "upper", 0);

        // VERIFICA 2: Ora il bonus è stato consumato, il turno passa a Bob
        assertFalse(p1.hasExtraPick(), "Ora Alice deve aver consumato il bonus");
        assertEquals("Bob", game.getCurrentPicker().getNickname(), "Ora deve essere il turno di Bob");
        assertEquals(2, p1.getMyTribe().size());

        // TURNO 3: Bob fa la sua unica pescata
        game.pickCard("Bob", "upper", 0);

        // VERIFICA 3: Tutti hanno finito
        assertNull(game.getCurrentPicker(), "Nessuno deve più pescare");
        assertFalse(game.getCurrentState() instanceof PickingState, "Lo stato deve essere avanzato");
    }
}