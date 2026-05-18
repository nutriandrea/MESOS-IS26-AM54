package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

public class PickingStateEdgeTest {

    private Game game;
    private Player p1;
    private Player p2;
    private PickingState pickingState;

    @BeforeEach
    public void setUp() {
        game = new Game();
        Board board = new Board();
        game.setBoard(board);

        p1 = new Player(); p1.setNickname("Alice");
        p2 = new Player(); p2.setNickname("Bob");

        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        board.initializeLayout(2);

        // Setup base: Alice in posizione 0, Bob in posizione 1
        OfferingTile tile0 = new OfferingTile('A', 1, 1, 0); // 1 pick upper, 1 pick lower
        OfferingTile tile1 = new OfferingTile('B', 1, 0, 0);
        board.getOfferingTrack().set(0, tile0);
        board.getOfferingTrack().set(1, tile1);

        tile0.occupy(p1);
        p1.setCurrentTile(tile0);
        p1.setHasPlacedTotem(true);

        tile1.occupy(p2);
        p2.setCurrentTile(tile1);
        p2.setHasPlacedTotem(true);

        pickingState = new PickingState();
        game.setState(pickingState);
    }

    // ==========================================
    // 1. TEST ACQUISTO EDIFICIO E ROLLBACK
    // ==========================================
    @Test
    public void testPickCard_BuildingRollback() {
        // Alice non ha cibo (0 food)
        assertEquals(0, p1.getFood());

        // Inseriamo un Personaggio e un Edificio (Costo 2) nella riga UPPER
        game.getBoard().addCharacterCard(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1), "upper");

        BuildingCard expensiveBuilding = new BuildingCard(null, 2, 0, null, new ArrayList<>(), 1);
        game.getBoard().addBuildingCard(expensiveBuilding, "upper");

        // L'indice 0 è il personaggio, l'indice 1 è l'edificio.
        // Alice prova a comprare l'edificio (indice 1) senza avere 2 cibo.
        Exception exception = assertThrows(Exception.class, () -> {
            game.pickCard("Alice", "upper", 1);
        });

        // VERIFICHE
        assertEquals("Non hai abbastanza cibo (\uD83C\uDF56) per prendere questa carta!", exception.getMessage());

        // ROLLBACK: L'edificio deve essere stato rimesso sulla board!
        assertEquals(1, game.getBoard().getBuildingRowSize("upper"), "L'edificio deve essere tornato sulla plancia");
        // Il pick non deve essere stato consumato
        assertEquals(1, p1.getCurrentTile().getRemainingUpperPicks());
    }

    // ==========================================
    // 2. TEST END TURN: ECCEZIONE PERSONAGGI OBBLIGATORI
    // ==========================================
    @Test
    public void testEndTurn_MandatoryCharactersLeft() {
        // Alice ha 1 pick upper disponibile sulla tessera
        // Inseriamo un Personaggio in upper (quindi DEVE pescarlo)
        game.getBoard().addCharacterCard(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1), "upper");

        // Alice prova a passare il turno volontariamente
        Exception exception = assertThrows(Exception.class, () -> {
            game.endTurn("Alice");
        });

        assertEquals("Non puoi terminare il turno volontariamente: ci sono ancora Personaggi disponibili che DEVI pescare!", exception.getMessage());
    }

    // ==========================================
    // 3. TEST END TURN: PULIZIA EXTRA PICK
    // ==========================================
    @Test
    public void testEndTurn_ClearsExtraPick_IfNoMandatoryCards() throws Exception {
        // Rimuoviamo i pick obbligatori azzerando la tessera di Alice
        p1.getCurrentTile().useUpperPick();
        p1.getCurrentTile().useLowerPick();

        // Alice ha finito la tessera, ma ha l'Extra Pick attivo (che decide di non usare)
        p1.setExtraPick(true);

        // Alice passa il turno volontariamente (ora può farlo perché la tessera è vuota)
        game.endTurn("Alice");

        // VERIFICA: Il gioco deve averle confiscato l'Extra Pick per non farglielo accumulare
        assertFalse(p1.hasExtraPick(), "L'Extra Pick deve essere stato rimosso dopo aver passato il turno");
        assertEquals("Bob", game.getCurrentPicker().getNickname(), "Il turno deve essere passato a Bob");
    }

    // ==========================================
    // 4. TEST SKIP TURN: DISCONNESSIONE E PULIZIA
    // ==========================================
    @Test
    public void testSkipTurn_ClearsBonus_AndIgnoresMandatoryCards() throws Exception {
        // Alice ha un Personaggio obbligatorio da pescare
        game.getBoard().addCharacterCard(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1), "upper");

        // Alice ha l'Extra Pick attivo
        p1.setExtraPick(true);

        // Alice si disconnette (skipTurn)
        pickingState.skipTurn(game, "Alice");

        // VERIFICHE:
        // 1. L'eccezione di pescata obbligatoria è stata bypassata (il test non è crashato)
        // 2. Il bonus è stato rimosso
        assertFalse(p1.hasExtraPick(), "Il bonus deve essere stato pulito durante la disconnessione");

        // 3. Il turno è avanzato
        assertEquals("Bob", game.getCurrentPicker().getNickname());
    }

    // ==========================================
    // 5. TEST AUTO END TURN: FILE COMPLETAMENTE VUOTE
    // ==========================================
    @Test
    public void testAutoEndTurn_WhenRowsArePhysicallyEmpty() throws Exception {
        // Alice ha 1 pick upper sulla tessera.
        // Ma la fila UPPER della Board è FISICAMENTE VUOTA (nessun personaggio, nessun edificio).
        assertEquals(0, game.getBoard().getCharacterRowSize("upper"));
        assertEquals(0, game.getBoard().getBuildingRowSize("upper"));

        // Per triggerare il controllo, facciamo consumare ad Alice l'unico pick LOWER che ha.
        // Aggiungiamo una carta in LOWER e gliela facciamo pescare.
        game.getBoard().addCharacterCard(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1), "lower");

        // ESECUZIONE
        game.pickCard("Alice", "lower", 0);

        // VERIFICA DELLA MAGIA:
        // Alice ha appena pescato in lower. La sua tessera ha ancora 1 pick in upper.
        // MA la fila upper è vuota. Quindi shouldAutoEndTurn deve aver restituito TRUE,
        // chiudendo automaticamente il turno di Alice e passando a Bob!
        assertEquals("Bob", game.getCurrentPicker().getNickname(), "Il turno doveva chiudersi in automatico perché la fila in alto era vuota");
    }
}