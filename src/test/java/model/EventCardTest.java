package model;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.effects.buildings.EventPhaseEffect;
import it.polimi.ingsw.am54.model.effects.events.EventEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventCardTest {

    private Game game;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("TestPlayer");
        game.joinGame("Player2");
        game.chooseTotemColor("TestPlayer", "RED");
        game.chooseTotemColor("Player2", "YELLOW");
        game.setupGame();
        
        player = game.getPlayers().get(0);
    }

    // ==================== Constructor Tests ====================

    @Test
    void testEventCard_Constructor() {
        EventCard card = new EventCard(1, true, null, 2, 1, 0, 0, 0, 0, 0);
        assertNotNull(card);
    }

    @Test
    void testEventCard_IsFinalEvent() {
        EventCard card = new EventCard(1, true, null, 2, 1, 0, 0, 0, 0, 0);
        assertTrue(card.isFinalEvent());
    }

    @Test
    void testEventCard_IsNotFinalEvent() {
        EventCard card = new EventCard(1, false, null, 2, 1, 0, 0, 0, 0, 0);
        assertFalse(card.isFinalEvent());
    }

    // ==================== Reveal Tests ====================

    @Test
    void testEventCard_Reveal() {
        EventCard card = new EventCard(1, false, null, 2, 1, 0, 0, 0, 0, 0);
        Board board = game.getBoard();
        
        assertDoesNotThrow(() -> card.reveal(board, "lower"));
    }

    // ==================== ResolveEvent Tests ====================

    @Test
    void testEventCard_ResolveEvent_WithEmptyEffects() {
        EventCard card = new EventCard(1, false, new java.util.ArrayList<>(), 2, 1, 0, 0, 0, 0, 0);
        
        assertDoesNotThrow(() -> card.resolveEvent(game));
    }

    @Test
    void testEventCard_ResolveEvent_WithEffects() {
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(new EventEffect("HUNTER"));
        
        EventCard card = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 0, 0);
        
        assertDoesNotThrow(() -> card.resolveEvent(game));
    }

    // ==================== Getters Tests ====================

    @Test
    void testEventCard_GetBaseFood() {
        EventCard card = new EventCard(1, false, null, 5, 1, 0, 0, 0, 0, 0);
        assertEquals(5, card.getBaseFood());
    }

    @Test
    void testEventCard_GetAuraPerCharacter() {
        EventCard card = new EventCard(1, false, null, 2, 3, 0, 0, 0, 0, 0);
        assertEquals(3, card.getAuraPerCharacter());
    }

    @Test
    void testEventCard_GetThreshold() {
        EventCard card = new EventCard(1, false, null, 2, 1, 5, 0, 0, 0, 0);
        assertEquals(5, card.getThreshold());
    }

    @Test
    void testEventCard_GetBonusMultiplier() {
        EventCard card = new EventCard(1, false, null, 2, 1, 0, 3, 0, 0, 0);
        assertEquals(3, card.getBonusMultiplier());
    }

    @Test
    void testEventCard_GetPenalty() {
        EventCard card = new EventCard(1, false, null, 2, 1, 0, 0, 5, 0, 0);
        assertEquals(5, card.getPenalty());
    }

    @Test
    void testEventCard_GetBonus() {
        EventCard card = new EventCard(1, false, null, 2, 1, 0, 0, 0, 4, 0);
        assertEquals(4, card.getBonus());
    }

    @Test
    void testEventCard_GetMalus() {
        EventCard card = new EventCard(1, false, null, 2, 1, 0, 0, 0, 0, 3);
        assertEquals(3, card.getMalus());
    }

    // ==================== Farmer Event Tests ====================

    @Test
    void testEventCard_IsFarmerEvent_True() {
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(new EventEffect("FARMER"));
        
        EventCard card = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 0, 0);
        
        assertTrue(card.isFarmerEvent());
    }

    @Test
    void testEventCard_IsFarmerEvent_False() {
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(new EventEffect("HUNTER"));
        
        EventCard card = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 0, 0);
        
        assertFalse(card.isFarmerEvent());
    }

    @Test
    void testEventCard_ShouldResolveLast_True() {
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(new EventEffect("FARMER"));
        
        EventCard card = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 0, 0);
        
        assertTrue(card.shouldResolveLast());
    }

    @Test
    void testEventCard_ShouldResolveLast_False() {
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(new EventEffect("HUNTER"));
        
        EventCard card = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 0, 0);
        
        assertFalse(card.shouldResolveLast());
    }

    @Test
    void testEventEffect_Apply_SHAMAN_DoubleAura() throws Exception {
        Game testGame = new Game();
        testGame.joinGame("Player1");
        testGame.joinGame("Player2");
        testGame.chooseTotemColor("Player1", "RED");
        testGame.chooseTotemColor("Player2", "YELLOW");
        testGame.setupGame();
        
        Player player1 = testGame.getPlayers().get(0);
        Player player2 = testGame.getPlayers().get(1);
        
        player1.modifyAuraPoints(5);
        player2.modifyAuraPoints(2);
        
        List<IEffect<BuildingCard>> buildingEffects = new ArrayList<>();
        buildingEffects.add(new EventPhaseEffect("DOUBLE_AURA_SHAMAN", null));
        
        BuildingCard building = new BuildingCard(JobType.SHAMAN, 5, 2, Phases.EVENT, buildingEffects, 1);
        player1.getBuildings().add(building);
        
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(new EventEffect("SHAMAN"));
        
        EventCard eventCard = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 10, 5);
        
        int initialAura = player1.getAuraPoints();
        
        eventCard.resolveEvent(testGame);
        
        assertTrue(player1.getAuraPoints() >= initialAura);
    }
}
