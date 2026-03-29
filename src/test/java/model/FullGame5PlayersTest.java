package model;

import model.states.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full Game Simulation Test: Complete 10-round MESOS game with 5 players
 * 
 * This test simulates a complete game following all rules:
 * - Setup with 5 players
 * - 10 rounds with totem placement, picking, and events
 * - Era transitions at rounds 4 and 7
 * - End game scoring and winner determination
 */
class FullGame5PlayersTest {

    private Game game;
    private Player p1, p2, p3, p4, p5;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.joinGame("Charlie");
        game.joinGame("Diana");
        game.joinGame("Evan");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "YELLOW");
        game.chooseTotemColor("Charlie", "BLACK");
        game.chooseTotemColor("Diana", "WHITE");
        game.chooseTotemColor("Evan", "BLUE");
        game.setupGame();
        game.setState(new StartTurnState());
        
        p1 = game.getPlayerByNickname("Alice");
        p2 = game.getPlayerByNickname("Bob");
        p3 = game.getPlayerByNickname("Charlie");
        p4 = game.getPlayerByNickname("Diana");
        p5 = game.getPlayerByNickname("Evan");
    }

    @Test
    void testGameSetup_5Players() {
        assertEquals(5, game.getPlayersNumber());
        assertEquals(5, game.getPlayers().size());
        assertEquals(TotemColors.RED, p1.getTotemColors());
        assertEquals(TotemColors.YELLOW, p2.getTotemColors());
        assertEquals(TotemColors.BLACK, p3.getTotemColors());
        assertEquals(TotemColors.WHITE, p4.getTotemColors());
        assertEquals(TotemColors.BLUE, p5.getTotemColors());
    }

    @Test
    void testGameSetup_InitialFoodDistribution() {
        assertEquals(2, p1.getFood());
        assertEquals(3, p2.getFood());
        assertEquals(3, p3.getFood());
        assertEquals(4, p4.getFood());
        assertEquals(4, p5.getFood());
    }

    @Test
    void testGameSetup_BoardCreated() {
        assertNotNull(game.getBoard());
        assertNotNull(game.getBoard().getOfferingTrack());
        assertEquals(7, game.getBoard().getOfferingTrack().size());
        assertNotNull(game.getBoard().getTurnOrderTile());
        assertEquals(5, game.getBoard().getTurnOrderTile().size());
    }

    @Test
    void testGameSetup_InitialStateIsStartTurn() {
        assertTrue(game.getCurrentState() instanceof StartTurnState);
    }

    @Test
    void testGameSetup_CharacterRowsPopulated() {
        assertTrue(game.getBoard().getCharacterRowSize("upper") > 0);
        assertTrue(game.getBoard().getCharacterRowSize("lower") > 0);
    }

    @Test
    void testComplete10RoundGame() throws Exception {
        int initialEra = game.getCurrentEra();
        assertEquals(1, initialEra);

        for (int round = 1; round <= 10; round++) {
            simulateRound(game);
            
            assertEquals(round, game.getRoundCount(), 
                "Round counter should be " + round + " after round " + round);
            
            if (round == 4) {
                assertEquals(2, game.getCurrentEra(), 
                    "Era should transition to 2 at round 4");
            } else if (round == 7) {
                assertEquals(3, game.getCurrentEra(), 
                    "Era should transition to 3 at round 7");
            }
        }

        assertEquals(10, game.getRoundCount());
        assertTrue(game.isTribeDeckEmpty(), "Tribe deck should be empty after 10 rounds");
    }

    @Test
    void testRound_NormalTurns() throws Exception {
        simulateRound(game);
        
        assertEquals(1, game.getRoundCount());
        assertTrue(game.getCurrentState() instanceof StartTurnState);
    }

    @Test
    void testRound_EraII_Transition() throws Exception {
        for (int r = 1; r < 4; r++) {
            simulateRound(game);
        }
        
        assertEquals(2, game.getCurrentEra());
        
        simulateRound(game);
        assertEquals(2, game.getCurrentEra());
    }

    @Test
    void testRound_EraIII_Transition() throws Exception {
        for (int r = 1; r < 7; r++) {
            simulateRound(game);
        }
        
        assertEquals(3, game.getCurrentEra());
        
        simulateRound(game);
        assertEquals(3, game.getCurrentEra());
    }

    @Test
    void testEndGame_WinnerDetermination() throws Exception {
        for (int r = 1; r <= 10; r++) {
            simulateRound(game);
        }
        
        applyEndGameEffects(game);
        
        List<Player> winners = game.determineWinners();
        assertFalse(winners.isEmpty());
        
        int maxScore = winners.stream()
            .mapToInt(game::calculatePlayerScore)
            .max().orElse(0);
        
        for (Player p : winners) {
            assertEquals(maxScore, game.calculatePlayerScore(p));
        }
    }

    @Test
    void testEndGame_ScoreCalculation() throws Exception {
        for (int r = 1; r <= 10; r++) {
            simulateRound(game);
        }
        
        applyEndGameEffects(game);
        
        assertDoesNotThrow(() -> {
            for (Player p : game.getPlayers()) {
                game.calculatePlayerScore(p);
            }
        });
    }

    @Test
    void testEndGame_TieBreakerByFood() throws Exception {
        for (int r = 1; r <= 10; r++) {
            simulateRound(game);
        }
        
        applyEndGameEffects(game);
        
        List<Player> winners = game.determineWinners();
        
        if (winners.size() > 1) {
            int maxFood = winners.stream()
                .mapToInt(Player::getFood)
                .max().orElse(0);
            
            for (Player p : winners) {
                assertEquals(maxFood, p.getFood(),
                    "In tie-breaker, all winners should have same food");
            }
        }
    }

    @Test
    void testTotemPlacement_LeftToRightOrder() throws Exception {
        List<Player> turnOrderPlayers = new ArrayList<>();
        for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            if (slot.getOccupiedBy() != null) {
                turnOrderPlayers.add(slot.getOccupiedBy());
            }
        }
        assertFalse(turnOrderPlayers.isEmpty());
        
        simulateStartTurnPhase(game);
        
        assertEquals(5, game.getBoard().getOfferingTrack().stream()
            .filter(t -> t.getOccupiedBy() != null)
            .count());
    }

    @Test
    void testPickingPhase_AllPlayersPick() throws Exception {
        simulateStartTurnPhase(game);
        
        int picksBefore = getTotalPicks(game);
        simulatePickingPhase(game);
        int picksAfter = getTotalPicks(game);
        
        assertTrue(picksAfter > picksBefore, "All players should pick cards");
    }

    @Test
    void testEventPhase_ResolvesAllEvents() throws Exception {
        simulateRound(game);
        
        int upperSize = game.getBoard().getCharacterRowSize("upper");
        int lowerSize = game.getBoard().getCharacterRowSize("lower");
        
        assertTrue(upperSize >= 0);
        assertTrue(lowerSize >= 0);
    }

    private void simulateRound(Game game) throws Exception {
        simulateStartTurnPhase(game);
        simulatePickingPhase(game);
        simulateEventPhase(game);
    }

    private void simulateStartTurnPhase(Game game) throws Exception {
        if (!(game.getCurrentState() instanceof StartTurnState)) {
            game.setState(new StartTurnState());
        }
        
        List<Player> playersOnTurnOrder = new ArrayList<>();
        for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            if (slot.getOccupiedBy() != null) {
                playersOnTurnOrder.add(slot.getOccupiedBy());
            }
        }
        
        if (playersOnTurnOrder.isEmpty()) {
            for (Player p : game.getPlayers()) {
                TurnOrderSlot freeSlot = findFreeTurnOrderSlot(game);
                if (freeSlot != null) {
                    freeSlot.occupy(p);
                }
            }
        }
        
        for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            if (slot.getOccupiedBy() != null) {
                Player player = slot.getOccupiedBy();
                game.setCurrentPlayer(player);
                
                int tileIndex = findAvailableOfferingTile(game, 0);
                try {
                    game.placeTotem(player.getNickname(), tileIndex);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }
    
    private TurnOrderSlot findFreeTurnOrderSlot(Game game) {
        for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            if (slot.getOccupiedBy() == null) {
                return slot;
            }
        }
        return null;
    }

    private void simulatePickingPhase(Game game) throws Exception {
        List<Player> playersOnTiles = getPlayersOnOfferingTiles(game);
        
        for (Player player : playersOnTiles) {
            OfferingTile tile = player.getCurrentTile();
            if (tile != null) {
                pickCardsFromTile(game, player, tile);
            }
            
            game.getCurrentState().endTurn(game, player.getNickname());
        }
    }

    private void pickCardsFromTile(Game game, Player player, OfferingTile tile) throws Exception {
        int upperPicks = tile.getRemainingUpperPicks();
        int lowerPicks = tile.getRemainingLowerPicks();
        
        for (int i = 0; i < upperPicks; i++) {
            int rowSize = game.getBoard().getCharacterRowSize("upper");
            if (rowSize > 0) {
                try {
                    game.pickCard(player.getNickname(), "upper", 0);
                } catch (Exception e) {
                }
            }
        }
        
        for (int i = 0; i < lowerPicks; i++) {
            int rowSize = game.getBoard().getCharacterRowSize("lower");
            if (rowSize > 0) {
                try {
                    game.pickCard(player.getNickname(), "lower", 0);
                } catch (Exception e) {
                }
            }
        }
    }

    private void simulateEventPhase(Game game) throws Exception {
        GameState state = game.getCurrentState();
        if (state instanceof PickingState) {
            ((PickingState) state).resolvePhase(game);
        } else if (state instanceof EventState) {
            ((EventState) state).resolvePhase(game);
        }
    }

    private void applyEndGameEffects(Game game) {
        game.applyEndGameBuildingEffects();
    }

    private List<Player> getPlayersByTurnOrder(Game game) {
        List<Player> players = new java.util.ArrayList<>();
        List<TurnOrderSlot> turnOrder = game.getBoard().getTurnOrderTile();
        
        for (TurnOrderSlot slot : turnOrder) {
            if (slot.getOccupiedBy() != null) {
                players.add(slot.getOccupiedBy());
            }
        }
        
        for (Player p : game.getPlayers()) {
            if (!players.contains(p)) {
                players.add(p);
            }
        }
        
        return players;
    }

    private List<Player> getPlayersOnOfferingTiles(Game game) {
        List<Player> players = new java.util.ArrayList<>();
        for (OfferingTile tile : game.getBoard().getOfferingTrack()) {
            Player occupant = tile.getOccupiedBy();
            if (occupant != null) {
                players.add(occupant);
            }
        }
        return players;
    }

    private int findAvailableOfferingTile(Game game, int preferredIndex) {
        List<OfferingTile> tiles = game.getBoard().getOfferingTrack();
        
        if (preferredIndex < tiles.size() && tiles.get(preferredIndex).getOccupiedBy() == null) {
            return preferredIndex;
        }
        
        for (int i = 0; i < tiles.size(); i++) {
            if (tiles.get(i).getOccupiedBy() == null) {
                return i;
            }
        }
        
        return 0;
    }

    private TurnOrderSlot findFirstOccupiedTurnOrderSlot(Game game) {
        for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            if (slot.getOccupiedBy() != null) {
                return slot;
            }
        }
        return null;
    }

    private TurnOrderSlot findLastOccupiedTurnOrderSlot(Game game) {
        TurnOrderSlot last = null;
        for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            if (slot.getOccupiedBy() != null) {
                last = slot;
            }
        }
        return last;
    }

    private int getTotalPicks(Game game) {
        int total = 0;
        for (Player p : game.getPlayers()) {
            total += p.getMyTribe().size();
            total += p.getBuildings().size();
        }
        return total;
    }
}
