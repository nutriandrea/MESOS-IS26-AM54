package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.StartTurnState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class GameSetupIntegrationTest {

    private Game game;

    @BeforeEach
    public void setUp() {
        game = new Game();
    }

    @Test
    public void testFullSetup_3Players() throws Exception {
        // 1. Fase Lobby: aggiungiamo 3 giocatori
        // Nota: Assicurati che SetUpGameState aggiunga effettivamente i player alla lista
        game.joinGame("Alice");
        game.joinGame("Andrea");
        game.joinGame("Charlie");

        assertEquals(3, game.getPlayersNumber());

        // 2. ESECUZIONE SETUP
        // Questo metodo carica i JSON, crea i mazzi, inizializza la board e distribuisce cibo
        game.setupGame();

        // ==========================================
        // VERIFICA 1: STATO E VARIABILI DI GIOCO
        // ==========================================
        assertEquals(1, game.getCurrentEra(), "Il gioco deve iniziare nell'Era 1");
        assertEquals(1, game.getRoundCount(), "Il gioco deve iniziare al Round 1");
        assertTrue(game.getCurrentState() instanceof StartTurnState, "Dopo il setup lo stato deve essere StartTurnState");
        assertNotNull(game.getCurrentPlayer(), "Il primo giocatore di turno deve essere stato assegnato");

        // ==========================================
        // VERIFICA 2: DISTRIBUZIONE CIBO (Regola 2, 3, 3)
        // ==========================================
        // Recuperiamo i player nell'ordine in cui sono stati messi negli slot (tempPlayers shuffle)
        List<TurnOrderSlot> slots = game.getBoard().getTurnOrderTile();

        Player p1 = slots.get(0).getOccupiedBy();
        Player p2 = slots.get(1).getOccupiedBy();
        Player p3 = slots.get(2).getOccupiedBy();

        assertEquals(2, p1.getFood(), "Il primo giocatore nell'ordine deve avere 2 cibo");
        assertEquals(3, p2.getFood(), "Il secondo giocatore deve avere 3 cibo");
        assertEquals(3, p3.getFood(), "Il terzo giocatore deve avere 3 cibo");

        // ==========================================
        // VERIFICA 3: POPOLAMENTO BOARD (REGOLE SETUP)
        // ==========================================
        Board board = game.getBoard();

        // Fila Inferiore: numPlayers + 1 = 3 + 1 = 4 Personaggi
        assertEquals(4, board.getCharacterRowSize("lower"), "La fila inferiore deve avere 4 personaggi a inizio partita");

        // Fila Superiore: numPlayers + 4 = 3 + 4 = 7 (Somma personaggi + eventi)
        int upperTotal = board.getCharacterRowSize("upper") + board.getEventRowSize("upper");
        assertEquals(7, upperTotal, "La fila superiore deve avere 7 carte (personaggi+eventi) a inizio partita");

        // Edifici: Il numero dipende dal vostro EraCounts.json per 3 giocatori
        // Supponendo che per 3 giocatori l'Era 1 preveda un certo numero (es. 6)
        assertTrue(board.getBuildingRowSize("upper") > 0, "La riga degli edifici non deve essere vuota");
    }

    @Test
    public void testFullSetup_5Players_FoodDistribution() throws Exception {
        game.joinGame("P1");
        game.joinGame("P2");
        game.joinGame("P3");
        game.joinGame("P4");
        game.joinGame("P5");

        game.setupGame();

        List<TurnOrderSlot> slots = game.getBoard().getTurnOrderTile();

        // Verifica regola 4-5 giocatori (4 cibo)
        assertEquals(4, slots.get(3).getOccupiedBy().getFood(), "Il quarto giocatore deve avere 4 cibo");
        assertEquals(4, slots.get(4).getOccupiedBy().getFood(), "Il quinto giocatore deve avere 4 cibo");
    }

    @Test
    public void testMazziInizializzati() throws Exception {
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.setupGame();

        assertNotNull(game.getTribeDeck(), "Il mazzo tribù deve essere creato");
        assertNotNull(game.getBuildingDeck(), "Il mazzo edifici deve essere creato");
        assertFalse(game.getTribeDeck().isEmpty(), "Il mazzo tribù non deve essere vuoto");
    }
}