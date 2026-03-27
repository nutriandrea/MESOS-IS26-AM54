package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import model.effects.events.EventEffect;
import model.effects.buildings.EndgamePhaseEffect;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EffectsTest {

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game();
        
        try {
            game.joinGame("carbicane");
            game.joinGame("frabbo");
            game.chooseTotemColor("carbicane", "RED");
            game.chooseTotemColor("frabbo", "YELLOW");
            game.setupGame();
        } catch (Exception e) {
            fail("Setup should not throw: " + e.getMessage());
        }
    }

    @Test
    void testEventEffect_IsFarmerEffect() {
        EventEffect farmerEffect = new EventEffect("FARMER");
        assertTrue(farmerEffect.isFarmerEffect());
        assertTrue(farmerEffect.shouldResolveLast());
    }

    @Test
    void testEventEffect_IsNotFarmerEffect() {
        EventEffect hunterEffect = new EventEffect("HUNTER");
        assertFalse(hunterEffect.isFarmerEffect());
        assertFalse(hunterEffect.shouldResolveLast());
        
        EventEffect artistEffect = new EventEffect("ARTIST");
        assertFalse(artistEffect.isFarmerEffect());
        
        EventEffect shamanEffect = new EventEffect("SHAMAN");
        assertFalse(shamanEffect.isFarmerEffect());
    }

    @Test
    void testEventEffect_IsEndgameEffect() {
        // EventEffect should not be endgame effect
        EventEffect hunterEffect = new EventEffect("HUNTER");
        assertFalse(hunterEffect.isEndgameEffect());
    }

    @Test
    void testEndgamePhaseEffect_IsEndgameEffect() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("AURA_PER_JOB", null);
        assertTrue(effect.isEndgameEffect());
    }

    @Test
    void testEndgamePhaseEffect_GetEffectName() {
        EndgamePhaseEffect effect = new EndgamePhaseEffect("FLAT_AURA_BONUS", null);
        assertEquals("FLAT_AURA_BONUS", effect.getEffectName());
    }

    @Test
    void testFarmerEffectName() {
        EventEffect farmerEffect = new EventEffect("FARMER");
        assertEquals("FARMER", farmerEffect.getEffectName());
    }

    @Test
    void testNoLossEffect_DefaultFalse() {
        EventEffect hunterEffect = new EventEffect("HUNTER");
        assertFalse(hunterEffect.isNoLossEffect());
    }

    @Test
    void testDoubleAuraShamanEffect_DefaultFalse() {
        EventEffect hunterEffect = new EventEffect("HUNTER");
        assertFalse(hunterEffect.isDoubleAuraShamanEffect());
    }
}
