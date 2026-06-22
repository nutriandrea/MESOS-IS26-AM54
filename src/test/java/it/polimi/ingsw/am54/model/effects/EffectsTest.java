package it.polimi.ingsw.am54.model.effects;

import com.google.gson.JsonObject;
import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.effects.buildings.*;
import it.polimi.ingsw.am54.model.effects.characters.*;
import it.polimi.ingsw.am54.model.effects.events.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di tutti gli effetti del gioco (personaggi, edifici, eventi).
 * Ogni effetto è verificato in isolamento con input controllati.
 */
public class EffectsTest {

    private Game game;
    private Player player;

    @BeforeEach
    public void setUp() {
        game = new Game();
        game.setBoard(new Board());
        player = new Player();
        player.setNickname("Tester");
        game.getPlayers().add(player);
    }

    private CharacterCard ch(JobType type, int level, int aura) {
        return new CharacterCard(level, type, aura, null, new ArrayList<>(), 1);
    }

    // ==========================================
    // EFFETTI PERSONAGGIO
    // ==========================================
    @Test
    public void testCharacterHunterGivesFoodPerHunter() {
        player.getMyTribe().add(ch(JobType.HUNTER, 1, 0));
        player.getMyTribe().add(ch(JobType.HUNTER, 1, 0));
        CharacterCard newest = ch(JobType.HUNTER, 1, 0);
        player.getMyTribe().add(newest);

        new CharacterHunter().applyOnPurchase(player, game, newest);
        assertEquals(3, player.getFood(), "3 cacciatori in tribù -> +3 cibo");
    }

    @Test
    public void testCharacterHunterLevelZeroNoEffect() {
        CharacterCard lvl0 = ch(JobType.HUNTER, 0, 0);
        player.getMyTribe().add(lvl0);
        new CharacterHunter().applyOnPurchase(player, game, lvl0);
        assertEquals(0, player.getFood(), "Livello 0 -> nessun cibo");
    }

    @Test
    public void testCharacterConstructorGivesDiscount() {
        CharacterCard c = ch(JobType.CONSTRUCTOR, 2, 0);
        new CharacterConstructor().applyOnPurchase(player, game, c);
        assertEquals(2, player.getMyBuildingDiscount());
    }

    @Test
    public void testCharacterShamanGivesMagic() {
        CharacterCard c = ch(JobType.SHAMAN, 3, 0);
        new CharacterShaman().applyOnPurchase(player, game, c);
        assertEquals(3, player.getMagicPoints());
    }

    @Test
    public void testCharacterPassiveDoesNothing() {
        CharacterCard c = ch(JobType.ARTIST, 1, 0);
        new CharacterPassive().applyOnPurchase(player, game, c);
        assertEquals(0, player.getFood());
        assertEquals(0, player.getAuraPoints());
        assertEquals(0, player.getMagicPoints());
    }

    // ==========================================
    // EFFETTI EDIFICIO
    // ==========================================
    @Test
    public void testBuildingImmediateResource() {
        JsonObject params = new JsonObject();
        params.addProperty("aura", 4);
        params.addProperty("magic", 2);
        new BuildingImmediateResource(params).applyOnPurchase(player, game, null);
        assertEquals(4, player.getAuraPoints());
        assertEquals(2, player.getMagicPoints());
    }

    @Test
    public void testBuildingEndgameAura() {
        JsonObject params = new JsonObject();
        params.addProperty("aura", 6);
        new BuildingEndgameAura(params).applyOnEndGame(player, game, null);
        assertEquals(6, player.getAuraPoints());
    }

    @Test
    public void testBuildingExtraPick() {
        new BuildingExtraPick(null).applyOnTurnEnd(player, game, null);
        assertTrue(player.hasExtraPick());
    }

    @Test
    public void testBuildingDoubleInventorClosesPair() {
        // 2 inventori dello stesso livello = coppia chiusa -> +3 cibo
        player.getMyTribe().add(ch(JobType.INVENTOR, 1, 0));
        CharacterCard second = ch(JobType.INVENTOR, 1, 0);
        player.getMyTribe().add(second);

        new BuildingDoubleInventor().applyOnCharacterPicked(player, game, null, second);
        assertEquals(3, player.getFood(), "Coppia di inventori dello stesso livello -> +3 cibo");
    }

    @Test
    public void testBuildingDoubleInventorIgnoresNonInventor() {
        CharacterCard hunter = ch(JobType.HUNTER, 1, 0);
        player.getMyTribe().add(hunter);
        new BuildingDoubleInventor().applyOnCharacterPicked(player, game, null, hunter);
        assertEquals(0, player.getFood());
    }

    @Test
    public void testBuildingJobAuraBonusByMultiplier() {
        JsonObject params = new JsonObject();
        params.addProperty("jobType", "FARMER");
        params.addProperty("multiplier", 4);
        player.getMyTribe().add(ch(JobType.FARMER, 1, 0));
        player.getMyTribe().add(ch(JobType.FARMER, 1, 0));

        new BuildingJobAuraBonus(params).applyOnEndGame(player, game, null);
        assertEquals(8, player.getAuraPoints(), "2 farmer * 4 = 8 aura");
    }

    @Test
    public void testBuildingJobAuraBonusUseCardBaseAura() {
        JsonObject params = new JsonObject();
        params.addProperty("jobType", "CONSTRUCTOR");
        params.addProperty("useCardBaseAura", true);
        player.getMyTribe().add(ch(JobType.CONSTRUCTOR, 1, 3));
        player.getMyTribe().add(ch(JobType.CONSTRUCTOR, 1, 2));

        new BuildingJobAuraBonus(params).applyOnEndGame(player, game, null);
        assertEquals(5, player.getAuraPoints(), "Somma delle aura base dei costruttori");
    }

    @Test
    public void testBuildingSixDifferentAura() {
        // Un set completo dei 6 mestieri -> minCount = 1 -> 6 aura
        for (JobType j : new JobType[]{JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN,
                JobType.FARMER, JobType.ARTIST, JobType.INVENTOR}) {
            player.getMyTribe().add(ch(j, 1, 0));
        }
        new BuildingSixDifferentAura().applyOnEndGame(player, game, null);
        assertEquals(6, player.getAuraPoints());
    }

    @Test
    public void testBuildingSixDifferentAuraIncompleteSet() {
        player.getMyTribe().add(ch(JobType.HUNTER, 1, 0)); // manca il resto
        new BuildingSixDifferentAura().applyOnEndGame(player, game, null);
        assertEquals(0, player.getAuraPoints(), "Set incompleto -> nessuna aura");
    }

    @Test
    public void testBuildingSixOfTypeCompleteSetGivesFood() {
        // Un esemplare per ogni mestiere: il set è "completo" al livello 1 -> +5 cibo
        for (JobType j : new JobType[]{JobType.HUNTER, JobType.CONSTRUCTOR, JobType.SHAMAN,
                JobType.FARMER, JobType.ARTIST, JobType.INVENTOR}) {
            player.getMyTribe().add(ch(j, 1, 0));
        }
        CharacterCard last = player.getMyTribe().get(player.getMyTribe().size() - 1);
        new BuildingSixOfType().applyOnCharacterPicked(player, game, null, last);
        assertEquals(5, player.getFood());
    }

    @Test
    public void testBuildingSixOfTypeIncompleteNoFood() {
        player.getMyTribe().add(ch(JobType.HUNTER, 1, 0));
        CharacterCard h = player.getMyTribe().get(0);
        new BuildingSixOfType().applyOnCharacterPicked(player, game, null, h);
        assertEquals(0, player.getFood());
    }

    @Test
    public void testBuildingTurnOrderBonus() {
        TurnOrderSlot slot = new TurnOrderSlot(1, 2); // bonusFood positivo
        slot.occupy(player);
        game.getBoard().getTurnOrderTile().add(slot);

        new BuildingTurnOrderBonus().applyOnTurnEnd(player, game, null);
        assertEquals(1, player.getFood(), "Slot con bonus positivo -> +1 cibo");
    }

    @Test
    public void testBuildingShamanPassiveSetsFlags() {
        JsonObject params = new JsonObject();
        params.addProperty("noLoss", true);
        params.addProperty("doubleAura", true);
        EventCard ev = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0);

        new BuildingShamanPassive(params).applyOnEvent(player, game, null, ev);
        assertTrue(player.hasShamanNoLoss());
        assertTrue(player.hasShamanDoubleAura());
    }

    @Test
    public void testBuildingEventCostModifierSustenanceDiscount() {
        JsonObject params = new JsonObject();
        params.addProperty("jobType", "FARMER");
        params.addProperty("foodDiscountMultiplier", -1);

        player.getMyTribe().add(ch(JobType.FARMER, 1, 0));
        player.getMyTribe().add(ch(JobType.FARMER, 1, 0));

        EventCard sustenance = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0);
        sustenance.setName("FARMER");

        new BuildingEventCostModifier(params).applyOnEvent(player, game, null, sustenance);
        assertEquals(-2, player.getEventCostFood(), "2 farmer * -1 = -2 di sconto cibo evento");
    }

    @Test
    public void testBuildingEventCostModifierAuraOverride() {
        JsonObject params = new JsonObject();
        params.addProperty("jobType", "HUNTER");
        params.addProperty("overrideAuraCost", true);

        player.getMyTribe().add(ch(JobType.HUNTER, 1, 0));
        EventCard huntEvent = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0);
        huntEvent.setName("HUNTER");

        new BuildingEventCostModifier(params).applyOnEvent(player, game, null, huntEvent);
        assertEquals(1, player.getEventCostAura(), "1 cacciatore -> +1 aura evento");
    }

    @Test
    public void testBuildingEventCostModifierNullJobOrEventNoOp() {
        new BuildingEventCostModifier(null).applyOnEvent(player, game, null, null);
        assertEquals(0, player.getEventCostFood());
        assertEquals(0, player.getEventCostAura());
    }

    // ==========================================
    // EFFETTI EVENTO
    // ==========================================
    @Test
    public void testEventHunterRewardsHunters() {
        player.getMyTribe().add(ch(JobType.HUNTER, 1, 0));
        player.getMyTribe().add(ch(JobType.HUNTER, 1, 0));
        // baseFood=1, auraPerCharacter=2
        EventCard ev = new EventCard(1, false, new ArrayList<>(), 1, 2, 0, 0, 0, 0, 0);

        new EventHunter().applyOnEvent(null, game, ev, ev);
        assertEquals(2, player.getFood(), "2 cacciatori * baseFood 1 = 2 cibo");
        assertEquals(4, player.getAuraPoints(), "2 cacciatori * auraPerChar 2 = 4 aura");
    }

    @Test
    public void testEventArtistBonusAboveThreshold() {
        player.getMyTribe().add(ch(JobType.ARTIST, 1, 0));
        player.getMyTribe().add(ch(JobType.ARTIST, 1, 0));
        // threshold=2, bonusMultiplier=3, penalty=5
        EventCard ev = new EventCard(1, false, new ArrayList<>(), 0, 0, 2, 3, 5, 0, 0);

        new EventArtist().applyOnEvent(null, game, ev, ev);
        assertEquals(6, player.getAuraPoints(), "2 artisti >= soglia 2 -> 2*3 = 6 aura");
    }

    @Test
    public void testEventArtistPenaltyBelowThreshold() {
        player.getMyTribe().add(ch(JobType.ARTIST, 1, 0));
        player.modifyAuraPoints(10);
        // threshold=3, penalty=5
        EventCard ev = new EventCard(1, false, new ArrayList<>(), 0, 0, 3, 3, 5, 0, 0);

        new EventArtist().applyOnEvent(null, game, ev, ev);
        assertEquals(5, player.getAuraPoints(), "1 artista < soglia 3 -> -5 aura (10-5)");
    }

    @Test
    public void testEventShamanBonusToMaxMalusToMin() {
        Player other = new Player(); other.setNickname("Other");
        game.getPlayers().add(other);

        player.modifyMagicPoints(5);   // max
        other.modifyMagicPoints(0);    // min
        // bonus=4, malus=2
        EventCard ev = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 4, 2);

        new EventShaman().applyOnEvent(null, game, ev, ev);
        assertEquals(4, player.getAuraPoints(), "Chi ha più magia guadagna il bonus");
        assertEquals(-2, other.getAuraPoints(), "Chi ha meno magia subisce il malus");
    }

    @Test
    public void testEventShamanDoubleAuraWhenUniqueMax() {
        player.modifyMagicPoints(5);
        player.setShamanDoubleAura(true);
        Player other = new Player(); other.setNickname("Other");
        other.modifyMagicPoints(1);
        game.getPlayers().add(other);

        EventCard ev = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 3, 0);
        new EventShaman().applyOnEvent(null, game, ev, ev);
        assertEquals(6, player.getAuraPoints(), "Max unico + doubleAura -> 2*bonus = 6");
    }
}
