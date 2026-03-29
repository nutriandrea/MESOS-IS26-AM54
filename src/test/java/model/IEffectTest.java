package model;

import model.effects.buildings.EndgamePhaseEffect;
import model.effects.buildings.EventPhaseEffect;
import model.effects.buildings.PostactionPhaseEffect;
import model.effects.buildings.PostpickPhaseEffect;
import model.effects.characters.CharacterEffect;
import model.effects.events.EventEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IEffectTest {

    private Game game;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "YELLOW");
        game.setupGame();
        player = game.getPlayers().get(0);
    }

    // ==================== Default Methods Tests ====================

    @Test
    void testIEffect_Apply_MethodExists() {
        IEffect<Card> effect = (p, g, c) -> {};
        assertDoesNotThrow(() -> effect.apply(player, game, null));
    }

    // ==================== isFarmerEffect Tests ====================

    @Test
    void testIEffect_Default_isFarmerEffect_ReturnsFalse() {
        IEffect<Card> effect = (p, g, c) -> {};
        assertFalse(effect.isFarmerEffect());
    }

    @Test
    void testEventEffect_isFarmerEffect_ReturnsTrue_ForFARMER() {
        EventEffect effect = new EventEffect("FARMER");
        assertTrue(effect.isFarmerEffect());
    }

    @Test
    void testEventEffect_isFarmerEffect_ReturnsFalse_ForHUNTER() {
        EventEffect effect = new EventEffect("HUNTER");
        assertFalse(effect.isFarmerEffect());
    }

    @Test
    void testEventEffect_isFarmerEffect_ReturnsFalse_ForARTIST() {
        EventEffect effect = new EventEffect("ARTIST");
        assertFalse(effect.isFarmerEffect());
    }

    @Test
    void testEventEffect_isFarmerEffect_ReturnsFalse_ForSHAMAN() {
        EventEffect effect = new EventEffect("SHAMAN");
        assertFalse(effect.isFarmerEffect());
    }

    @Test
    void testEndgamePhaseEffect_isFarmerEffect_ReturnsFalse() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("AURA_PER_JOB", null);
        assertFalse(effect.isFarmerEffect());
    }

    @Test
    void testEventPhaseEffect_isFarmerEffect_ReturnsFalse() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        assertFalse(effect.isFarmerEffect());
    }

    @Test
    void testCharacterEffect_isFarmerEffect_ReturnsFalse() {
        CharacterEffect effect = new CharacterEffect("HUNTER");
        assertFalse(effect.isFarmerEffect());
    }

    // ==================== shouldResolveLast Tests ====================

    @Test
    void testIEffect_Default_shouldResolveLast_ReturnsFalse() {
        IEffect<Card> effect = (p, g, c) -> {};
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testEventEffect_shouldResolveLast_ReturnsTrue_ForFARMER() {
        EventEffect effect = new EventEffect("FARMER");
        assertTrue(effect.shouldResolveLast());
    }

    @Test
    void testEventEffect_shouldResolveLast_ReturnsFalse_ForHUNTER() {
        EventEffect effect = new EventEffect("HUNTER");
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testEventEffect_shouldResolveLast_ReturnsFalse_ForARTIST() {
        EventEffect effect = new EventEffect("ARTIST");
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testEventEffect_shouldResolveLast_ReturnsFalse_ForSHAMAN() {
        EventEffect effect = new EventEffect("SHAMAN");
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testEndgamePhaseEffect_shouldResolveLast_ReturnsFalse() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("AURA_PER_JOB", null);
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testEventPhaseEffect_shouldResolveLast_ReturnsFalse() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testCharacterEffect_shouldResolveLast_ReturnsFalse() {
        CharacterEffect effect = new CharacterEffect("HUNTER");
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testPostpickPhaseEffect_shouldResolveLast_ReturnsFalse() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("SIX_OF_TYPE", null);
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testPostactionPhaseEffect_shouldResolveLast_ReturnsFalse() {
        PostactionPhaseEffect effect = new PostactionPhaseEffect("EXTRA_PICK", null);
        assertFalse(effect.shouldResolveLast());
    }

    // ==================== isNoLossEffect Tests ====================

    @Test
    void testIEffect_Default_isNoLossEffect_ReturnsFalse() {
        IEffect<Card> effect = (p, g, c) -> {};
        assertFalse(effect.isNoLossEffect());
    }

    @Test
    void testEventPhaseEffect_isNoLossEffect_ReturnsTrue_ForNO_LOSS() {
        EventPhaseEffect effect = new EventPhaseEffect("NO_LOSS", null);
        assertTrue(effect.isNoLossEffect());
    }

    @Test
    void testEventPhaseEffect_isNoLossEffect_ReturnsFalse_ForOther() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        assertFalse(effect.isNoLossEffect());
    }

    // ==================== isDoubleAuraShamanEffect Tests ====================

    @Test
    void testIEffect_Default_isDoubleAuraShamanEffect_ReturnsFalse() {
        IEffect<Card> effect = (p, g, c) -> {};
        assertFalse(effect.isDoubleAuraShamanEffect());
    }

    @Test
    void testEventPhaseEffect_isDoubleAuraShamanEffect_ReturnsTrue_ForDOUBLE_AURA_SHAMAN() {
        EventPhaseEffect effect = new EventPhaseEffect("DOUBLE_AURA_SHAMAN", null);
        assertTrue(effect.isDoubleAuraShamanEffect());
    }

    @Test
    void testEventPhaseEffect_isDoubleAuraShamanEffect_ReturnsFalse_ForOther() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        assertFalse(effect.isDoubleAuraShamanEffect());
    }

    // ==================== isEndgameEffect Tests ====================

    @Test
    void testIEffect_Default_isEndgameEffect_ReturnsFalse() {
        IEffect<Card> effect = (p, g, c) -> {};
        assertFalse(effect.isEndgameEffect());
    }

    @Test
    void testEndgamePhaseEffect_isEndgameEffect_ReturnsTrue() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("AURA_PER_JOB", null);
        assertTrue(effect.isEndgameEffect());
    }

    @Test
    void testEndgamePhaseEffect_isEndgameEffect_ReturnsTrue_ForAllTypes() {
        assertTrue(new EndgamePhaseEffect("AURA_PER_JOB", null).isEndgameEffect());
        assertTrue(new EndgamePhaseEffect("DOUBLE_CONSTRUCTOR_AURA", null).isEndgameEffect());
        assertTrue(new EndgamePhaseEffect("SIX_DIFFERENT_AURA", null).isEndgameEffect());
        assertTrue(new EndgamePhaseEffect("FLAT_AURA_BONUS", null).isEndgameEffect());
    }

    @Test
    void testEventEffect_isEndgameEffect_ReturnsFalse() {
        EventEffect effect = new EventEffect("FARMER");
        assertFalse(effect.isEndgameEffect());
    }

    @Test
    void testEventPhaseEffect_isEndgameEffect_ReturnsFalse() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        assertFalse(effect.isEndgameEffect());
    }

    @Test
    void testCharacterEffect_isEndgameEffect_ReturnsFalse() {
        CharacterEffect effect = new CharacterEffect("HUNTER");
        assertFalse(effect.isEndgameEffect());
    }

    @Test
    void testPostpickPhaseEffect_isEndgameEffect_ReturnsFalse() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("SIX_OF_TYPE", null);
        assertFalse(effect.isEndgameEffect());
    }

    @Test
    void testPostactionPhaseEffect_isEndgameEffect_ReturnsFalse() {
        PostactionPhaseEffect effect = new PostactionPhaseEffect("EXTRA_PICK", null);
        assertFalse(effect.isEndgameEffect());
    }
}
