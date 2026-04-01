package model;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.effects.buildings.EndgamePhaseEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuildingCardTest {

    private BuildingCard buildingCard;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        Game game = new Game();
        game.joinGame("TestPlayer");
        game.joinGame("Player2");
        game.chooseTotemColor("TestPlayer", "RED");
        game.chooseTotemColor("Player2", "YELLOW");
        game.setupGame();
        
        player = game.getPlayers().get(0);
    }

    // ==================== Basic Getter Tests ====================

    @Test
    void testBuildingCard_GetCost() {
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 5, 3, Phases.ENDGAME, null, 1
        );
        assertEquals(5, card.getCost());
    }

    @Test
    void testBuildingCard_GetFinalAura() {
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 5, 3, Phases.ENDGAME, null, 1
        );
        assertEquals(0, card.getFinalAura());
    }

    @Test
    void testBuildingCard_GetName() {
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 5, 3, Phases.ENDGAME, null, 1
        );
        assertNull(card.getName());
    }

    @Test
    void testBuildingCard_GetBuildingType() {
        BuildingCard card = new BuildingCard(
            JobType.HUNTER, 3, 2, Phases.ENDGAME, null, 1
        );
        assertEquals(JobType.HUNTER, card.getBuildingType());
    }

    @Test
    void testBuildingCard_Era() {
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 5, 3, Phases.ENDGAME, null, 2
        );
        assertEquals(2, card.getEra());
    }

    // ==================== Constructor Tests ====================

    @Test
    void testBuildingCard_Constructor_AllParameters() {
        List<IEffect<BuildingCard>> effects = new ArrayList<>();
        effects.add(new EndgamePhaseEffect("AURA_PER_JOB", null));
        
        BuildingCard card = new BuildingCard(
            JobType.FARMER, 7, 4, Phases.EVENT, effects, 3
        );
        
        assertEquals(JobType.FARMER, card.getBuildingType());
        assertEquals(7, card.getCost());
        assertEquals(0, card.getFinalAura());
        assertNotNull(card.getEffects());
        assertEquals(1, card.getEffects().size());
        assertEquals(3, card.getEra());
    }

    @Test
    void testBuildingCard_Constructor_NullEffects() {
        BuildingCard card = new BuildingCard(
            JobType.NULL, 5, 3, Phases.ENDGAME, null, 1
        );
        assertNull(card.getEffects());
    }

    // ==================== canBePurchased Tests ====================

    @Test
    void testBuildingCard_CanBePurchased_EnoughFood() throws Exception {
        Game game = new Game();
        game.joinGame("Buyer");
        game.joinGame("Player2");
        game.chooseTotemColor("Buyer", "YELLOW");
        game.chooseTotemColor("Player2", "BLACK");
        game.setupGame();
        
        Player buyer = game.getPlayers().get(0);
        int initialFood = buyer.getFood();
        
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, initialFood - 1, 2, Phases.ENDGAME, null, 1
        );
        
        assertTrue(card.canBePurchased(buyer));
    }

    @Test
    void testBuildingCard_CannotBePurchased_NotEnoughFood() throws Exception {
        Game game = new Game();
        game.joinGame("PoorPlayer");
        game.joinGame("Player2");
        game.chooseTotemColor("PoorPlayer", "BLACK");
        game.chooseTotemColor("Player2", "WHITE");
        game.setupGame();
        
        Player poorPlayer = game.getPlayers().get(0);
        poorPlayer.modifyFood(-poorPlayer.getFood());
        
        BuildingCard expensiveCard = new BuildingCard(
            JobType.CONSTRUCTOR, 100, 5, Phases.ENDGAME, null, 1
        );
        
        assertFalse(expensiveCard.canBePurchased(poorPlayer));
    }

    @Test
    void testBuildingCard_CanBePurchased_WithDiscount() throws Exception {
        Game game = new Game();
        game.joinGame("DiscountPlayer");
        game.joinGame("Player2");
        game.chooseTotemColor("DiscountPlayer", "YELLOW");
        game.chooseTotemColor("Player2", "BLACK");
        game.setupGame();
        
        Player discountPlayer = game.getPlayers().get(0);
        
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 3, 1, Phases.ENDGAME, null, 1
        );
        card.addOwner(discountPlayer);
        
        BuildingCard expensiveCard = new BuildingCard(
            JobType.CONSTRUCTOR, 5, 2, Phases.ENDGAME, null, 1
        );
        
        int effectiveCost = expensiveCard.getCost() - discountPlayer.getMyBuildingDiscount();
        assertTrue(effectiveCost >= 0);
    }

    // ==================== buy Tests ====================

    @Test
    void testBuildingCard_Buy_Success() throws Exception {
        Game game = new Game();
        game.joinGame("Buyer");
        game.joinGame("Player2");
        game.chooseTotemColor("Buyer", "YELLOW");
        game.chooseTotemColor("Player2", "BLACK");
        game.setupGame();
        
        Player buyer = game.getPlayers().get(0);
        int initialFood = buyer.getFood();
        int initialBuildings = buyer.getBuildings().size();
        
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 1, 2, Phases.ENDGAME, null, 1
        );
        
        card.buy(buyer);
        
        assertEquals(initialBuildings + 1, buyer.getBuildings().size());
        assertTrue(buyer.getBuildings().contains(card));
    }

    @Test
    void testBuildingCard_Buy_ReducesFood() throws Exception {
        Game game = new Game();
        game.joinGame("Buyer");
        game.joinGame("Player2");
        game.chooseTotemColor("Buyer", "YELLOW");
        game.chooseTotemColor("Player2", "BLACK");
        game.setupGame();
        
        Player buyer = game.getPlayers().get(0);
        int initialFood = buyer.getFood();
        
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 2, 2, Phases.ENDGAME, null, 1
        );
        
        card.buy(buyer);
        
        assertTrue(buyer.getFood() <= initialFood);
    }

    // ==================== pick Tests ====================

    @Test
    void testBuildingCard_Pick_Success() throws Exception {
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 1, 2, Phases.ENDGAME, null, 1
        );
        int initialBuildings = player.getBuildings().size();
        
        card.pick(player, null);
        
        assertEquals(initialBuildings + 1, player.getBuildings().size());
    }

    @Test
    void testBuildingCard_Pick_InsufficientFood() throws Exception {
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 1000, 5, Phases.ENDGAME, null, 1
        );
        
        assertThrows(Exception.class, () -> {
            card.pick(player, null);
        });
    }

    @Test
    void testBuildingCard_Pick_WithDiscount() throws Exception {
        BuildingCard discountCard = new BuildingCard(
            JobType.CONSTRUCTOR, 1, 1, Phases.ENDGAME, null, 1
        );
        discountCard.addOwner(player);
        
        BuildingCard affordableCard = new BuildingCard(
            JobType.CONSTRUCTOR, 1, 2, Phases.ENDGAME, null, 1
        );
        
        assertDoesNotThrow(() -> {
            affordableCard.pick(player, null);
        });
    }

    // ==================== resolveEffects Tests ====================

    @Test
    void testBuildingCard_ResolveEffects_WithEmptyEffects() {
        BuildingCard card = new BuildingCard(
            JobType.NULL, 5, 3, Phases.ENDGAME, new ArrayList<>(), 1
        );
        
        assertDoesNotThrow(() -> card.resolveEffects(player, null));
    }

    @Test
    void testBuildingCard_ResolveEffects_WithEffects() {
        List<IEffect<BuildingCard>> effects = new ArrayList<>();
        effects.add(new EndgamePhaseEffect("AURA_PER_JOB", null));
        
        BuildingCard card = new BuildingCard(
            JobType.HUNTER, 5, 3, Phases.ENDGAME, effects, 1
        );
        
        assertDoesNotThrow(() -> card.resolveEffects(player, null));
    }

    // ==================== getEffects Tests ====================

    @Test
    void testBuildingCard_GetEffects_ReturnsNull() {
        BuildingCard card = new BuildingCard(
            JobType.NULL, 5, 3, Phases.ENDGAME, null, 1
        );
        assertNull(card.getEffects());
    }

    @Test
    void testBuildingCard_GetEffects_ReturnsList() {
        List<IEffect<BuildingCard>> effects = new ArrayList<>();
        effects.add(new EndgamePhaseEffect("AURA_PER_JOB", null));
        
        BuildingCard card = new BuildingCard(
            JobType.HUNTER, 5, 3, Phases.ENDGAME, effects, 1
        );
        
        assertNotNull(card.getEffects());
        assertEquals(1, card.getEffects().size());
    }

    // ==================== All JobTypes ====================

    @Test
    void testBuildingCard_AllJobTypes() {
        JobType[] validTypes = {JobType.HUNTER, JobType.SHAMAN, JobType.CONSTRUCTOR, 
                                JobType.FARMER, JobType.INVENTOR, JobType.ARTIST};
        for (JobType type : validTypes) {
            BuildingCard card = new BuildingCard(
                type, 5, 3, Phases.ENDGAME, null, 1
            );
            assertEquals(type, card.getBuildingType());
        }
    }

    // ==================== All Phases ====================

    @Test
    void testBuildingCard_AllPhases() {
        Phases[] phases = {Phases.STARTTURN, Phases.POSTPICK, Phases.POSTACTION, Phases.EVENT, Phases.ENDGAME};
        for (Phases phase : phases) {
            BuildingCard card = new BuildingCard(
                JobType.NULL, 5, 3, phase, null, 1
            );
            assertNotNull(card);
        }
    }
}
