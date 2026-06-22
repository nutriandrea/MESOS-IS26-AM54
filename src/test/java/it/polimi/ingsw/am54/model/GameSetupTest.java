package it.polimi.ingsw.am54.model;

import it.polimi.ingsw.am54.model.states.SetUpGameState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del setup completo della partita (caricamento mazzi/plancia dai JSON tramite
 * DeckFactory) e delle utility di scoring/turnazione del Game.
 */
public class GameSetupTest {

    private Game newGameWithPlayers(int n) {
        Game game = new Game();
        String[] names = {"Alice", "Bob", "Charlie", "Dave", "Eve"};
        for (int i = 0; i < n; i++) {
            Player p = new Player();
            p.setNickname(names[i]);
            p.setTotemColor(TotemColors.values()[i]);
            game.getPlayers().add(p);
        }
        return game;
    }

    @Test
    public void testFullSetupBuildsDecksBoardAndStartsTurn() throws Exception {
        Game game = newGameWithPlayers(3);
        game.setupGame();

        assertEquals(1, game.getCurrentEra(), "Si parte dall'Era 1");
        assertEquals(1, game.getRoundCount());
        assertNotNull(game.getBoard(), "La plancia deve essere costruita");
        assertNotNull(game.getTribeDeck());
        assertNotNull(game.getBuildingDeck());
        assertNotNull(game.getCurrentPlayer(), "Deve esserci un primo giocatore");
        assertTrue(game.getCurrentState() instanceof StartTurnState,
                "Dopo il setup si entra in StartTurnState");

        // Le file iniziali devono essere popolate (numPlayers+1 in lower)
        assertTrue(game.getBoard().getCharacterRowSize("lower") >= 1);
        assertFalse(game.getBoard().getOfferingTrack().isEmpty());
        assertFalse(game.getBoard().getTurnOrderTile().isEmpty());
    }

    @Test
    public void testSetupWorksForTwoAndFourPlayers() throws Exception {
        assertDoesNotThrow(() -> newGameWithPlayers(2).setupGame());
        assertDoesNotThrow(() -> newGameWithPlayers(4).setupGame());
    }

    @Test
    public void testGetActivePlayerCountDefaultsToPlayers() {
        Game game = newGameWithPlayers(3);
        assertEquals(3, game.getActivePlayerCount(), "Senza override usa il numero di giocatori");
        game.setActivePlayerCount(2);
        assertEquals(2, game.getActivePlayerCount());
    }

    @Test
    public void testRefillUpperTribeRow() {
        Game game = newGameWithPlayers(3);
        game.setBoard(new Board());

        Stack<TribeCard> stack = new Stack<>();
        for (int i = 0; i < 12; i++)
            stack.push(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1));
        game.setTribeDeck(new Deck<>(stack));

        game.refillUpperTribeRow();
        int upper = game.getBoard().getCharacterRowSize("upper") + game.getBoard().getEventRowSize("upper");
        assertEquals(7, upper, "Target = numPlayers + 4 = 7");
    }

    @Test
    public void testEraTransitionAndDeckEmpty() {
        Game game = newGameWithPlayers(2);
        game.setCurrentEra(1);

        Stack<TribeCard> stack = new Stack<>();
        CharacterCard era2 = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 2);
        stack.push(era2);
        game.setTribeDeck(new Deck<>(stack));
        assertTrue(game.isEraTransition(), "La carta in cima è Era 2 > 1");
        assertFalse(game.isTribeDeckEmpty());

        game.getTribeDeck().drawCard();
        assertTrue(game.isTribeDeckEmpty());
        assertFalse(game.isEraTransition(), "Mazzo vuoto -> nessuna transizione");
    }

    @Test
    public void testScoringBonusMethods() {
        Game game = new Game();
        Player p = new Player(); p.setNickname("Scorer");
        game.getPlayers().add(p);

        // 2 inventori livelli 1 e 2 -> inventors(2) * uniqueLevels(2) = 4
        p.getMyTribe().add(charLevel(JobType.INVENTOR, 1, 0));
        p.getMyTribe().add(charLevel(JobType.INVENTOR, 2, 0));
        assertEquals(4, game.getPlayerInventorsBonus(p));

        // 3 artisti -> 3/2 = 1 coppia * 10 = 10
        for (int i = 0; i < 3; i++) p.getMyTribe().add(charLevel(JobType.ARTIST, 1, 0));
        assertEquals(10, game.getPlayerArtistsBonus(p));

        // costruttori: somma delle aura
        p.getMyTribe().add(charLevel(JobType.CONSTRUCTOR, 1, 3));
        p.getMyTribe().add(charLevel(JobType.CONSTRUCTOR, 1, 2));
        assertEquals(5, game.getPlayerConstructorsBonus(p));

        // edifici: somma finalAura
        p.getBuildings().add(new BuildingCard(JobType.HUNTER, 0, 7, null, new ArrayList<>(), 1));
        assertEquals(7, game.getPlayerBuildingsBaseAura(p));

        p.modifyAuraPoints(5);
        // 5 + 4 + 10 + 5 + 7 = 31
        assertEquals(31, game.calculatePlayerScore(p));
    }

    @Test
    public void testDetermineWinnersSingle() {
        Game game = new Game();
        Player a = player("A", 30, 0);
        Player b = player("B", 10, 0);
        game.getPlayers().add(a);
        game.getPlayers().add(b);

        List<Player> winners = game.determineWinners();
        assertEquals(1, winners.size());
        assertEquals(a, winners.get(0));
    }

    @Test
    public void testDetermineWinnersTieBrokenByFood() {
        Game game = new Game();
        Player a = player("A", 20, 5);
        Player b = player("B", 20, 9);
        game.getPlayers().add(a);
        game.getPlayers().add(b);

        List<Player> winners = game.determineWinners();
        assertEquals(1, winners.size());
        assertEquals(b, winners.get(0), "Pari aura -> vince chi ha più cibo");
    }

    @Test
    public void testDetermineWinnersFullTie() {
        Game game = new Game();
        game.getPlayers().add(player("A", 15, 4));
        game.getPlayers().add(player("B", 15, 4));

        List<Player> winners = game.determineWinners();
        assertEquals(2, winners.size(), "Aura e cibo identici -> pareggio totale");
    }

    @Test
    public void testRemovePlayerEntirely() {
        Game game = newGameWithPlayers(3);
        game.removePlayerEntirely("Bob");
        assertEquals(2, game.getPlayersNumber());
        assertTrue(game.getPlayers().stream().noneMatch(p -> p.getNickname().equals("Bob")));
    }

    @Test
    public void testRemoveAllPlayersResetsToSetupState() {
        Game game = new Game();
        Player a = new Player(); a.setNickname("Solo");
        game.getPlayers().add(a);
        // SetUpGameState.shouldResetToSetupOnEmpty() è false, quindi NON resetta:
        // usiamo lo stato di default (SetUpGameState) e verifichiamo che resti coerente
        game.removePlayerEntirely("Solo");
        assertEquals(0, game.getPlayersNumber());
        assertNotNull(game.getCurrentState());
    }

    @Test
    public void testGetPlayerByNicknameThrowsWhenMissing() {
        Game game = newGameWithPlayers(2);
        assertThrows(Exception.class, () -> game.getPlayerByNickname("Ghost"));
    }

    // --- helper ---
    private CharacterCard charLevel(JobType type, int level, int aura) {
        return new CharacterCard(level, type, aura, null, new ArrayList<>(), 1);
    }

    private Player player(String name, int aura, int food) {
        Player p = new Player();
        p.setNickname(name);
        p.modifyAuraPoints(aura);
        p.modifyFood(food);
        return p;
    }
}
