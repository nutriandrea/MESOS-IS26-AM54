package model;

import model.effects.buildings.EndgamePhaseEffect;
import model.effects.buildings.EventPhaseEffect;
import model.effects.buildings.PostactionPhaseEffect;
import model.effects.buildings.PostpickPhaseEffect;
import model.effects.characters.CharacterEffect;
import model.effects.events.EventEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EffectsTest {

    private Game game;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();
        game.joinGame("Alice");
        game.joinGame("Bob");
        game.chooseTotemColor("Alice", "RED");
        game.chooseTotemColor("Bob", "YELLOW");
        game.setupGame();
        
        alice = game.getPlayers().get(0);
        bob = game.getPlayers().get(1);
    }

    // ==================== EventEffect Tests ====================

    @Test
    void testEventEffect_GetEffectName() {
        EventEffect effect = new EventEffect("HUNTER");
        assertEquals("HUNTER", effect.getEffectName());
    }

    @Test
    void testEventEffect_IsFarmerEffect_True() {
        EventEffect effect = new EventEffect("FARMER");
        assertTrue(effect.isFarmerEffect());
    }

    @Test
    void testEventEffect_IsFarmerEffect_False() {
        EventEffect effect = new EventEffect("HUNTER");
        assertFalse(effect.isFarmerEffect());
    }

    @Test
    void testEventEffect_ShouldResolveLast_True() {
        EventEffect effect = new EventEffect("FARMER");
        assertTrue(effect.shouldResolveLast());
    }

    @Test
    void testEventEffect_ShouldResolveLast_False() {
        EventEffect effect = new EventEffect("HUNTER");
        assertFalse(effect.shouldResolveLast());
    }

    @Test
    void testEventEffect_IsNoLossEffect_DefaultFalse() {
        EventEffect effect = new EventEffect("HUNTER");
        assertFalse(effect.isNoLossEffect());
    }

    @Test
    void testEventEffect_IsDoubleAuraShamanEffect_DefaultFalse() {
        EventEffect effect = new EventEffect("HUNTER");
        assertFalse(effect.isDoubleAuraShamanEffect());
    }

    @Test
    void testEventEffect_Apply_HUNTER() {
        EventEffect effect = new EventEffect("HUNTER");
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(effect);
        EventCard eventCard = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 0, 0);
        
        assertDoesNotThrow(() -> effect.apply(null, game, eventCard));
    }

    @Test
    void testEventEffect_Apply_FARMER() {
        EventEffect effect = new EventEffect("FARMER");
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(effect);
        EventCard eventCard = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 0, 0);
        
        assertDoesNotThrow(() -> effect.apply(null, game, eventCard));
    }

    @Test
    void testEventEffect_Apply_ARTIST() {
        EventEffect effect = new EventEffect("ARTIST");
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(effect);
        EventCard eventCard = new EventCard(1, false, effects, 2, 1, 1, 2, 3, 0, 0);
        
        assertDoesNotThrow(() -> effect.apply(null, game, eventCard));
    }

    @Test
    void testEventEffect_Apply_SHAMAN() {
        EventEffect effect = new EventEffect("SHAMAN");
        List<IEffect<EventCard>> effects = new ArrayList<>();
        effects.add(effect);
        EventCard eventCard = new EventCard(1, false, effects, 2, 1, 0, 0, 0, 5, 3);
        
        assertDoesNotThrow(() -> effect.apply(null, game, eventCard));
    }

    @Test
    void testEventEffect_IsEndgameEffect_ReturnsFalse() {
        EventEffect effect = new EventEffect("HUNTER");
        assertFalse(effect.isEndgameEffect());
    }

    // ==================== EndgamePhaseEffect Tests ====================

    @Test
    void testEndgamePhaseEffect_GetEffectName() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("AURA_PER_JOB", null);
        assertEquals("AURA_PER_JOB", effect.getEffectName());
    }

    @Test
    void testEndgamePhaseEffect_IsEndgameEffect_ReturnsTrue() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("AURA_PER_JOB", null);
        assertTrue(effect.isEndgameEffect());
    }

    @Test
    void testEndgamePhaseEffect_Apply_AURA_PER_JOB() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("AURA_PER_JOB", null);
        BuildingCard building = new BuildingCard(JobType.HUNTER, 3, 2, Phases.ENDGAME, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    @Test
    void testEndgamePhaseEffect_Apply_DOUBLE_CONSTRUCTOR_AURA() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("DOUBLE_CONSTRUCTOR_AURA", null);
        BuildingCard building = new BuildingCard(JobType.CONSTRUCTOR, 5, 2, Phases.ENDGAME, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    @Test
    void testEndgamePhaseEffect_Apply_SIX_DIFFERENT_AURA() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("SIX_DIFFERENT_AURA", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 5, 2, Phases.ENDGAME, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    @Test
    void testEndgamePhaseEffect_Apply_FLAT_AURA_BONUS() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("FLAT_AURA_BONUS", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 10, 0, Phases.ENDGAME, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    // ==================== EventPhaseEffect Tests ====================

    @Test
    void testEventPhaseEffect_GetEffectName() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        assertEquals("EVENT_FOOD_DISCOUNT", effect.getEffectName());
    }

    @Test
    void testEventPhaseEffect_Apply_EVENT_FOOD_DISCOUNT() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        BuildingCard building = new BuildingCard(JobType.FARMER, 5, 2, Phases.EVENT, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    @Test
    void testEventPhaseEffect_Apply_EVENT_BONUS() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_BONUS", null);
        BuildingCard building = new BuildingCard(JobType.HUNTER, 3, 2, Phases.EVENT, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    @Test
    void testEventPhaseEffect_IsNoLossEffect_ReturnsFalse() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        assertFalse(effect.isNoLossEffect());
    }

    @Test
    void testEventPhaseEffect_IsDoubleAuraShamanEffect_ReturnsFalse() {
        EventPhaseEffect effect = new EventPhaseEffect("EVENT_FOOD_DISCOUNT", null);
        assertFalse(effect.isDoubleAuraShamanEffect());
    }

    @Test
    void testEventPhaseEffect_IsNoLossEffect_ReturnsTrue() {
        EventPhaseEffect effect = new EventPhaseEffect("NO_LOSS", null);
        assertTrue(effect.isNoLossEffect());
    }

    @Test
    void testEventPhaseEffect_IsDoubleAuraShamanEffect_ReturnsTrue() {
        EventPhaseEffect effect = new EventPhaseEffect("DOUBLE_AURA_SHAMAN", null);
        assertTrue(effect.isDoubleAuraShamanEffect());
    }

    // ==================== PostpickPhaseEffect Tests ====================

    @Test
    void testPostpickPhaseEffect_Apply_SIX_OF_TYPE() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("SIX_OF_TYPE", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 4, 3, Phases.POSTPICK, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    @Test
    void testPostpickPhaseEffect_Apply_DOUBLE_INVENTOR() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("DOUBLE_INVENTOR", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 6, 4, Phases.POSTPICK, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    @Test
    void testPostpickPhaseEffect_Apply_THREE_MAGIC() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("THREE_MAGIC", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 6, 4, Phases.POSTPICK, null, 1);
        
        int initialMagic = alice.getMagicPoints();
        effect.apply(alice, game, building);
        assertEquals(initialMagic + 3, alice.getMagicPoints());
    }

    // ==================== SIX_OF_TYPE Tests ====================

    @Test
    void testPostpick_SixOfType_EmptyTribe() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("SIX_OF_TYPE", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 4, 3, Phases.POSTPICK, null, 1);
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood, alice.getFood());
    }

    @Test
    void testPostpick_SixOfType_SingleCard() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("SIX_OF_TYPE", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 4, 3, Phases.POSTPICK, null, 1);
        
        CharacterCard card = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        alice.modifyMyTribe(card);
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood, alice.getFood());
    }

    @Test
    void testPostpick_SixOfType_IncompleteSet() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("SIX_OF_TYPE", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 4, 3, Phases.POSTPICK, null, 1);
        
        alice.modifyMyTribe(new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(2, JobType.HUNTER, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(3, JobType.SHAMAN, 1, Phases.EVENT, null, 1));
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood, alice.getFood());
    }

    @Test
    void testPostpick_SixOfType_CompleteSet_SameCount() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("SIX_OF_TYPE", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 4, 3, Phases.POSTPICK, null, 1);
        
        alice.modifyMyTribe(new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(2, JobType.SHAMAN, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(3, JobType.CONSTRUCTOR, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(4, JobType.FARMER, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(5, JobType.INVENTOR, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(6, JobType.ARTIST, 1, Phases.EVENT, null, 1));
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood + 1, alice.getFood());
    }

    @Test
    void testPostpick_SixOfType_CompleteSet_DifferentCount() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("SIX_OF_TYPE", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 4, 3, Phases.POSTPICK, null, 1);
        
        alice.modifyMyTribe(new CharacterCard(1, JobType.HUNTER, 3, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(2, JobType.HUNTER, 3, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(3, JobType.HUNTER, 3, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(4, JobType.SHAMAN, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(5, JobType.CONSTRUCTOR, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(6, JobType.FARMER, 1, Phases.EVENT, null, 1));
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood, alice.getFood());
    }

    // ==================== DOUBLE_INVENTOR Tests ====================

    @Test
    void testPostpick_DoubleInventor_EmptyTribe() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("DOUBLE_INVENTOR", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 6, 4, Phases.POSTPICK, null, 1);
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood, alice.getFood());
    }

    @Test
    void testPostpick_DoubleInventor_LastNotInventor() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("DOUBLE_INVENTOR", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 6, 4, Phases.POSTPICK, null, 1);
        
        alice.modifyMyTribe(new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1));
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood, alice.getFood());
    }

    @Test
    void testPostpick_DoubleInventor_OddCount() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("DOUBLE_INVENTOR", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 6, 4, Phases.POSTPICK, null, 1);
        
        alice.modifyMyTribe(new CharacterCard(2, JobType.INVENTOR, 1, Phases.EVENT, null, 1));
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood, alice.getFood());
    }

    @Test
    void testPostpick_DoubleInventor_EvenCount_SameLevel() {
        PostpickPhaseEffect effect = new PostpickPhaseEffect("DOUBLE_INVENTOR", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 6, 4, Phases.POSTPICK, null, 1);
        
        alice.modifyMyTribe(new CharacterCard(2, JobType.INVENTOR, 1, Phases.EVENT, null, 1));
        alice.modifyMyTribe(new CharacterCard(2, JobType.INVENTOR, 1, Phases.EVENT, null, 1));
        
        assertEquals(2, alice.getMyTribe().size());
        assertEquals(JobType.INVENTOR, alice.getMyTribe().get(1).getJobType());
        
        int initialFood = alice.getFood();
        effect.apply(alice, game, building);
        assertEquals(initialFood + 3, alice.getFood());
    }

    // ==================== PostactionPhaseEffect Tests ====================

    @Test
    void testPostactionPhaseEffect_Apply_EXTRA_PICK() {
        PostactionPhaseEffect effect = new PostactionPhaseEffect("EXTRA_PICK", null);
        BuildingCard building = new BuildingCard(JobType.NULL, 9, 3, Phases.POSTACTION, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, building));
    }

    // ==================== CharacterEffect Tests ====================

    @Test
    void testCharacterEffect_Apply_HUNTER() {
        CharacterEffect effect = new CharacterEffect("HUNTER");
        CharacterCard hunter = new CharacterCard(1, JobType.HUNTER, 1, Phases.EVENT, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, hunter));
    }

    @Test
    void testCharacterEffect_Apply_CONSTRUCTOR() {
        CharacterEffect effect = new CharacterEffect("CONSTRUCTOR");
        CharacterCard builder = new CharacterCard(1, JobType.CONSTRUCTOR, 2, Phases.EVENT, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, builder));
    }

    @Test
    void testCharacterEffect_Apply_SHAMAN() {
        CharacterEffect effect = new CharacterEffect("SHAMAN");
        CharacterCard shaman = new CharacterCard(1, JobType.SHAMAN, 1, Phases.EVENT, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, shaman));
    }

    @Test
    void testCharacterEffect_Apply_FARMER() {
        CharacterEffect effect = new CharacterEffect("FARMER");
        CharacterCard farmer = new CharacterCard(1, JobType.FARMER, 1, Phases.EVENT, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, farmer));
    }

    @Test
    void testCharacterEffect_Apply_INVENTOR() {
        CharacterEffect effect = new CharacterEffect("INVENTOR");
        CharacterCard inventor = new CharacterCard(1, JobType.INVENTOR, 0, Phases.EVENT, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, inventor));
    }

    @Test
    void testCharacterEffect_Apply_ARTIST() {
        CharacterEffect effect = new CharacterEffect("ARTIST");
        CharacterCard artist = new CharacterCard(1, JobType.ARTIST, 0, Phases.EVENT, null, 1);
        
        assertDoesNotThrow(() -> effect.apply(alice, game, artist));
    }
}
