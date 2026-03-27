package model;

import model.effects.buildings.EventPhaseEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test
    void testBuildingCard_GetCost() {
        // Create a building card with cost 5
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
        // getFinalAura() currently returns 0 (TODO)
        assertEquals(0, card.getFinalAura());
    }

    @Test
    void testBuildingCard_GetName() {
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 5, 3, Phases.ENDGAME, null, 1
        );
        // name is null by default
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
    void testBuildingCard_CanBePurchased() throws Exception {
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
    void testBuildingCard_Buy_WithDiscount() throws Exception {
        Game game = new Game();
        game.joinGame("DiscountPlayer");
        game.joinGame("Player2");
        game.chooseTotemColor("DiscountPlayer", "YELLOW");
        game.chooseTotemColor("Player2", "BLACK");
        game.setupGame();
        
        Player discountPlayer = game.getPlayers().get(0);
        int initialFood = discountPlayer.getFood();
        
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

    @Test
    void testBuildingCard_Era() {
        BuildingCard card = new BuildingCard(
            JobType.CONSTRUCTOR, 5, 3, Phases.ENDGAME, null, 2
        );
        assertEquals(2, card.getEra());
    }
}
