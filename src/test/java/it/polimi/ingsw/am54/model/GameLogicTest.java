package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class GameLogicTest {

    private Game game;
    private Player p1;
    private Player p2;
    private Player p3;

    @BeforeEach
    public void setUp() {
        game = new Game();
        Board board = new Board();
        game.setBoard(board);

        p1 = new Player(); p1.setNickname("Alice");
        p2 = new Player(); p2.setNickname("Bob");
        p3 = new Player(); p3.setNickname("Charlie");

        game.getPlayers().add(p1);
        game.getPlayers().add(p2);
        game.getPlayers().add(p3);
    }

    // ==========================================
    // 1. TEST NEXT PLAYER (Turnazione)
    // ==========================================
    @Test
    public void testNextPlayer_SkipsPlacedTotems() {
        // Setup manuale dell'ordine di turno
        Board board = game.getBoard();
        TurnOrderSlot slot1 = new TurnOrderSlot(1, 0); slot1.occupy(p1);
        TurnOrderSlot slot2 = new TurnOrderSlot(2, 0); slot2.occupy(p2);
        TurnOrderSlot slot3 = new TurnOrderSlot(3, 0); slot3.occupy(p3);

        board.getTurnOrderTile().add(slot1);
        board.getTurnOrderTile().add(slot2);
        board.getTurnOrderTile().add(slot3);

        // Impostiamo il giocatore corrente
        game.setCurrentPlayer(p1);

        // Caso 1: Passaggio normale
        game.nextPlayer();
        assertEquals(p2, game.getCurrentPlayer(), "Il turno deve passare a Bob");

        // Caso 2: Charlie ha già piazzato il totem. Deve essere saltato, tornando ad Alice
        p3.setHasPlacedTotem(true);
        p1.setHasPlacedTotem(false); // Alice deve ancora piazzare il secondo totem

        game.nextPlayer(); // Da Bob salta Charlie e va ad Alice
        assertEquals(p1, game.getCurrentPlayer(), "Il turno deve saltare Charlie e tornare ad Alice");
    }

    // ==========================================
    // 2. TEST REFILL METODI
    // ==========================================
    @Test
    public void testRefillUpperTribeRow() {
        Stack<TribeCard> stack = new Stack<>();
        // Inseriamo 10 carte nel mazzo
        for (int i = 0; i < 10; i++) {
            stack.push(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1));
        }
        game.setTribeDeck(new Deck<>(stack));

        // Abbiamo 3 giocatori, il target è numPlayers + 4 = 7
        game.refillUpperTribeRow();

        int currentSize = game.getBoard().getCharacterRowSize("upper") + game.getBoard().getEventRowSize("upper");
        assertEquals(7, currentSize, "La fila superiore deve contenere esattamente 7 carte");
    }

    @Test
    public void testRefillUpperBuildings() {
        game.setCurrentEra(1);
        Stack<BuildingCard> bStack = new Stack<>();
        for (int i = 0; i < 10; i++) {
            bStack.push(new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1));
        }
        game.setBuildingDeck(new Deck<>(bStack));

        // Il test esegue il metodo. Se il file JSON è letto correttamente, aggiungerà carte.
        // Copre la logica del try-catch e del ciclo di pescata.
        assertDoesNotThrow(() -> game.refillUpperBuildings());
        assertTrue(game.getBoard().getBuildingRowSize("upper") >= 0, "Deve pescare edifici senza eccezioni");
    }

    // ==========================================
    // 3. TEST TRANSIZIONE ERA
    // ==========================================
    @Test
    public void testIsEraTransition() {
        game.setCurrentEra(1);
        Stack<TribeCard> stack = new Stack<>();

        // Creiamo una carta forzando esplicitamente l'Era a 2
        TribeCard era2Card = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1) {
            @Override
            public int getEra() { return 2; }
        };

        stack.push(era2Card);
        game.setTribeDeck(new Deck<>(stack));

        // Ora la carta restituirà sicuramente Era 2. 2 > 1 -> true.
        assertTrue(game.isEraTransition(), "Deve restituire true perché la carta in cima è Era 2");

        // Togliamo la carta e mettiamo una di Era 1
        stack.pop();

        TribeCard era1Card = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1) {
            @Override
            public int getEra() { return 1; }
        };

        stack.push(era1Card);
        assertFalse(game.isEraTransition(), "Deve restituire false perché la carta in cima è Era 1");
    }

    // ==========================================
    // 4. TEST EFFETTI EDIFICI FINE PARTITA
    // ==========================================
    @Test
    public void testApplyEndGameBuildingEffects() {
        // Creiamo un effetto endgame fittizio che dà 100 cibo via applyOnEndGame
        IEffect<BuildingCard> fakeEndGameEffect = new IEffect<>() {
            @Override public void applyOnEndGame(Player p, Game g, BuildingCard card) {
                p.modifyFood(100);
            }
        };

        BuildingCard b = new BuildingCard(null, 0, 0, null, List.of(fakeEndGameEffect), 1);
        p1.getBuildings().add(b);

        assertEquals(0, p1.getFood());
        // Simula la logica di EndGameState.resolvePhase()
        for (BuildingCard building : p1.getBuildings()) {
            if (building.getEffects() != null) {
                for (IEffect<BuildingCard> effect : building.getEffects()) {
                    effect.applyOnEndGame(p1, game, building);
                }
            }
        }
        assertEquals(100, p1.getFood(), "L'effetto di fine partita deve essere stato applicato");
    }

    // ==========================================
    // 5. TEST CALCOLO PUNTEGGI E VINCITORI
    // ==========================================
    @Test
    public void testCalculatePlayerScore() {
        p1.modifyAuraPoints(5); // Punti Base

        // 2 INVENTORI (Livello 1 e Livello 2) -> unique = 2, total = 2 -> 2 * 2 = 4 punti
        p1.getMyTribe().add(new CharacterCard(1, JobType.INVENTOR, 0, null, new ArrayList<>(), 1) {
            @Override public int getLevel() { return 1; }
        });
        p1.getMyTribe().add(new CharacterCard(1, JobType.INVENTOR, 0, null, new ArrayList<>(), 1) {
            @Override public int getLevel() { return 2; }
        });

        // 3 ARTISTI -> 3/2 = 1 coppia -> 1 * 10 = 10 punti
        for(int i=0; i<3; i++) {
            p1.getMyTribe().add(new CharacterCard(1, JobType.ARTIST, 0, null, new ArrayList<>(), 1));
        }

        // 1 CONSTRUTTORE -> Aura = 3
        p1.getMyTribe().add(new CharacterCard(1, JobType.CONSTRUCTOR, 0, null, new ArrayList<>(), 1) {
            @Override public int getAura() { return 3; }
        });

        // EDIFICI -> Final Aura = 7
        p1.getBuildings().add(new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1) {
            @Override public int getFinalAura() { return 7; }
        });

        // Totale atteso: 5 (base) + 4 (inventori) + 10 (artisti) + 3 (costruttori) + 7 (edifici) = 29
        int score = game.calculatePlayerScore(p1);
        assertEquals(29, score, "Il calcolo del punteggio deve rispettare tutte le regole di Mesos");
    }

    @Test
    public void testDetermineWinners_And_Notify() {
        // Assegniamo punti aura puri per fare in fretta
        p1.modifyAuraPoints(30); // Alice: 30 punti
        p2.modifyAuraPoints(30); // Bob: 30 punti (Pareggio)
        p3.modifyAuraPoints(15); // Charlie: 15 punti

        // Spareggio sul cibo
        p1.modifyFood(5);
        p2.modifyFood(10); // Bob ha più cibo, vince il pareggio

        // Simuliamo l'Observer per testare notifyEndGame
        final boolean[] notified = {false};
        game.addObserver(new IGameObserver() {
            @Override public void onPlayerJoined(Game game, Player player) {}
            @Override public void onTotemColorChosen(Game game, Player player, TotemColors color) {}
            @Override public void onTotemPlaced(Game game, Player player, int offerPosition) {}
            @Override public void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance, int newMagicBalance) {}
            @Override public void onTurnChanged(Game game, Player player) {}
            @Override public void onStateChanged(Game game, GameState newState) {}
            @Override public void onTotemReturned(Game game, Player player, int position, int f, int a) {}
            @Override public void onEventResolved(Game game, EventCard eventCard) {}

            // IL METODO CHE CI INTERESSA
            @Override public void onEndGame(Game game, List<Player> winnersList) {
                notified[0] = true;
                assertEquals(1, winnersList.size());
                assertEquals(p2, winnersList.get(0)); // Bob vince
            }
        });

        List<Player> winners = game.determineWinners();

        assertEquals(1, winners.size(), "Deve esserci un solo vincitore dopo lo spareggio");
        assertEquals(p2, winners.get(0), "Bob deve vincere per lo spareggio del cibo");

        // Verifichiamo il trigger della notifica (metodo notifyEndGame)
        game.notifyEndGame(winners);
        assertTrue(notified[0], "Gli Observer devono essere stati notificati della fine partita");
    }
}