package it.polimi.ingsw.am54.model.effects.buildings;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EndgameBuildingsTest {

    private Game game;
    private Player player;

    @BeforeEach
    public void setUp() {
        game = new Game();
        player = new Player();
        player.setNickname("TestPlayer");
        game.getPlayers().add(player);
    }

    // Helper method to quickly create characters
    private CharacterCard createCharacter(JobType type, int aura) {
        return new CharacterCard(1, type, aura, Phases.ENDGAME, new ArrayList<>(), 1);
    }

    // ==========================================
    // TEST 1: Fixed Endgame Points (e.g. +25 PP)
    // ==========================================
    @Test
    public void testBuildingEndgameAura() {
        // Create l'effetto (supponiamo dia 25 punti)
        // In JSON, this would be passed via JsonNode, but here we simulate it or use the card's finalAura field
        // To test your BuildingEndgameAura, which presumably adds points:
        BuildingEndgameAura effetto = new BuildingEndgameAura(null); // Passes il JsonNode corretto se il tuo costruttore lo richiede, oppure modificalo per il test

        BuildingCard card = new BuildingCard(JobType.NULL, 0, 25, Phases.ENDGAME, List.of(effetto), 1);
        player.getBuildings().add(card);

        // Simulate the endgame trigger
        effetto.applyOnEndGame(player, game, card);

        // The final score should include the 25 points (whether added via effect or via getFinalAura() di BuildingCard)
        int score = game.calculatePlayerScore(player);

        // Note: game.calculatePlayerScore already sums card.getFinalAura().
        // If your BuildingEndgameAura ALSO adds points to player.modifyAuraPoints(), you'll get double!
        // This test will tell you if you did things the right way.
        assertEquals(25, score, "L'edificio base dovrebbe dare esattamente 25 punti");
    }

    // ==========================================
    // TEST 2: Set of 6 different characters (SixDifferentAura)
    // ==========================================
    @Test
    public void testBuildingSixDifferentAura() {
        // 1. Give the player a complete set of 6 different jobs
        player.modifyMyTribe(createCharacter(JobType.HUNTER, 0));
        player.modifyMyTribe(createCharacter(JobType.CONSTRUCTOR, 0));
        player.modifyMyTribe(createCharacter(JobType.SHAMAN, 0));
        player.modifyMyTribe(createCharacter(JobType.FARMER, 0));
        player.modifyMyTribe(createCharacter(JobType.ARTIST, 0));
        player.modifyMyTribe(createCharacter(JobType.INVENTOR, 0));

        // Add a duplicate to verify it only counts complete sets
        player.modifyMyTribe(createCharacter(JobType.HUNTER, 0));

        // 2. CALCULATE THE BASE SCORE (Before the effect)
        // This will capture any intrinsic points (e.g. that famous 1 point of the Artist)
        int scoreBase = game.calculatePlayerScore(player);

        // 3. Creiamo e inneschiamo l'edificio
        BuildingSixDifferentAura effetto = new BuildingSixDifferentAura();
        BuildingCard card = new BuildingCard(JobType.NULL, 0, 0, Phases.ENDGAME, List.of(effetto), 1);
        player.getBuildings().add(card);

        effetto.applyOnEndGame(player, game, card);

        // 4. CALCOLIAMO IL PUNTEGGIO FINALE (Dopo l'effetto)
        int scoreFinale = game.calculatePlayerScore(player);

        // MATH: Regardless of how many base points the tribe had,
        // l'edificio DEVE averne aggiunti esattamente 6.
        assertEquals(scoreBase + 6, scoreFinale, "Un set completo dovrebbe aggiungere esattamente 6 punti extra al punteggio");
    }

    // ==========================================
    // TEST 3: Double points for a Job (e.g. Builders)
    // ==========================================
    @Test
    public void testBuildingJobAuraBonus() {
        player.modifyMyTribe(createCharacter(JobType.CONSTRUCTOR, 5));
        player.modifyMyTribe(createCharacter(JobType.CONSTRUCTOR, 5));
        player.modifyMyTribe(createCharacter(JobType.HUNTER, 2));

        int scoreBase = game.calculatePlayerScore(player); // Dovrebbe essere 10 (se calcola solo CONSTRUCTOR) + eventuali altri bonus

        // 🛠️ THE FIX: Create "fake" JSON parameters as if reading from the file
        JsonObject params = new JsonObject();
        params.addProperty("jobType", "CONSTRUCTOR");
        params.addProperty("useCardBaseAura", true); // Questo attiva il raddoppio dell'Aura base

        // Ora passiamo i parametri veri invece di 'null'!
        BuildingJobAuraBonus effetto = new BuildingJobAuraBonus(params);
        BuildingCard card = new BuildingCard(JobType.CONSTRUCTOR, 0, 0, Phases.ENDGAME, List.of(effetto), 1);
        player.getBuildings().add(card);

        effetto.applyOnEndGame(player, game, card);

        int scoreFinale = game.calculatePlayerScore(player);

        // MATH: The building must have added EXACTLY the builders' base aura (5 + 5 = +10)
        assertEquals(scoreBase + 10, scoreFinale, "I punti bonus dell'edificio dovrebbero raddoppiare esattamente i 10 punti base dei Costruttori");
    }
}