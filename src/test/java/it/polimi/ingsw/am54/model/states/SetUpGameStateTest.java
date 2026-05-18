package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SetUpGameStateTest {

    private Game game;
    private SetUpGameState setUpState;

    @BeforeEach
    public void setUp() {
        game = new Game();
        setUpState = new SetUpGameState();
        game.setState(setUpState);
    }

    // ==========================================
    // 1. TEST JOIN GAME (Lobby e Nickname)
    // ==========================================
    @Test
    public void testJoinGame_SuccessAndDuplicate() throws Exception {
        // Ingresso con successo
        setUpState.joinGame(game, "Alice");
        assertEquals(1, game.getPlayersNumber());
        assertEquals("Alice", game.getPlayers().get(0).getNickname());

        // Test Nickname Duplicato (Case Insensitive)
        Exception duplicateEx = assertThrows(Exception.class, () -> {
            setUpState.joinGame(game, "alice");
        });
        assertTrue(duplicateEx.getMessage().contains("già in uso"));
    }

    @Test
    public void testJoinGame_RoomFull() throws Exception {
        // Riempiamo la stanza fino al limite (5 giocatori)
        setUpState.joinGame(game, "P1");
        setUpState.joinGame(game, "P2");
        setUpState.joinGame(game, "P3");
        setUpState.joinGame(game, "P4");
        setUpState.joinGame(game, "P5");

        assertEquals(5, game.getPlayersNumber());

        // Il sesto giocatore deve essere respinto
        Exception fullRoomEx = assertThrows(Exception.class, () -> {
            setUpState.joinGame(game, "P6");
        });
        assertTrue(fullRoomEx.getMessage().contains("la stanza è piena"));
    }

    // ==========================================
    // 2. TEST SCELTA COLORE TOTEM
    // ==========================================
    @Test
    public void testChooseTotemColor_SuccessAndErrors() throws Exception {
        setUpState.joinGame(game, "Alice");
        setUpState.joinGame(game, "Bob");

        // Scelta valida
        setUpState.chooseTotemColor(game, "Alice", "RED");
        assertEquals(TotemColors.RED, game.getPlayerByNickname("Alice").getTotemColors());

        // Colore non valido / Inesistente
        Exception invalidColorEx = assertThrows(Exception.class, () -> {
            setUpState.chooseTotemColor(game, "Bob", "COLORE_INVENTATO");
        });
        assertTrue(invalidColorEx.getMessage().contains("non è valido"));

        // Colore già preso da Alice
        Exception takenColorEx = assertThrows(Exception.class, () -> {
            setUpState.chooseTotemColor(game, "Bob", "red");
        });
        assertTrue(takenColorEx.getMessage().contains("già stato scelto"));
    }

    // ==========================================
    // 3. TEST AVVIO PARTITA (Setup e Controlli)
    // ==========================================
    @Test
    public void testSetupGame_NotEnoughPlayers() throws Exception {
        setUpState.joinGame(game, "Alice");
        setUpState.chooseTotemColor(game, "Alice", "RED");

        // Proviamo ad avviare da soli
        Exception fewPlayersEx = assertThrows(Exception.class, () -> {
            setUpState.setupGame(game);
        });
        assertTrue(fewPlayersEx.getMessage().contains("servono almeno 2 giocatori"));
    }

    @Test
    public void testSetupGame_MissingColor() throws Exception {
        setUpState.joinGame(game, "Alice");
        setUpState.joinGame(game, "Bob");

        setUpState.chooseTotemColor(game, "Alice", "RED");
        // Bob non sceglie il colore

        Exception missingColorEx = assertThrows(Exception.class, () -> {
            setUpState.setupGame(game);
        });
        assertTrue(missingColorEx.getMessage().contains("non ha ancora scelto il colore"));
    }

    @Test
    public void testSetupGame_Success() throws Exception {
        setUpState.joinGame(game, "Alice");
        setUpState.joinGame(game, "Bob");

        setUpState.chooseTotemColor(game, "Alice", "RED");
        // Usiamo un altro colore standard
        setUpState.chooseTotemColor(game, "Bob", "BLUE");

        // Avviamo la partita
        setUpState.setupGame(game);

        // Verifichiamo che il setup orchestrato abbia funzionato
        // e che la macchina a stati sia avanzata
        assertTrue(game.getCurrentState() instanceof StartTurnState, "Lo stato deve essere passato a StartTurnState dopo l'avvio");

        // Verifica indiretta che game.setupGame() sia stato eseguito
        assertNotNull(game.getBoard(), "La plancia deve essere stata inizializzata");
        assertNotNull(game.getTribeDeck(), "I mazzi devono essere stati creati");
    }
}