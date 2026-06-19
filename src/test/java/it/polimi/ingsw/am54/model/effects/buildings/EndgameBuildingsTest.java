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

    // Metodo helper per creare velocemente personaggi
    private CharacterCard createCharacter(JobType type, int aura) {
        return new CharacterCard(1, type, aura, Phases.ENDGAME, new ArrayList<>(), 1);
    }

    // ==========================================
    // TEST 1: Punti Fissi a fine partita (Es. +25 PP)
    // ==========================================
    @Test
    public void testBuildingEndgameAura() {
        // Creiamo l'effetto (supponiamo dia 25 punti)
        // Nel JSON, questo verrebbe passato tramite JsonNode, ma qui lo simuliamo o usiamo il campo finalAura della carta
        // Per testare il tuo BuildingEndgameAura, che presumibilmente aggiunge punti:
        BuildingEndgameAura effetto = new BuildingEndgameAura(null); // Passa il JsonNode corretto se il tuo costruttore lo richiede, oppure modificalo per il test

        BuildingCard card = new BuildingCard(JobType.NULL, 0, 25, Phases.ENDGAME, List.of(effetto), 1);
        player.getBuildings().add(card);

        // Simuliamo il trigger di fine partita
        effetto.applyOnEndGame(player, game, card);

        // Il punteggio finale dovrebbe includere i 25 punti (che siano stati aggiunti tramite effetto o tramite getFinalAura() di BuildingCard)
        int score = game.calculatePlayerScore(player);

        // Attenzione: game.calculatePlayerScore somma già card.getFinalAura().
        // Se il tuo BuildingEndgameAura aggiunge ANCHE punti al player.modifyAuraPoints(), avrai il doppio!
        // Questo test ti dirà se hai fatto le cose nel modo giusto.
        assertEquals(25, score, "L'edificio base dovrebbe dare esattamente 25 punti");
    }

    // ==========================================
    // TEST 2: Set di 6 personaggi diversi (SixDifferentAura)
    // ==========================================
    @Test
    public void testBuildingSixDifferentAura() {
        // 1. Diamo al giocatore un set completo di 6 lavori diversi
        player.modifyMyTribe(createCharacter(JobType.HUNTER, 0));
        player.modifyMyTribe(createCharacter(JobType.CONSTRUCTOR, 0));
        player.modifyMyTribe(createCharacter(JobType.SHAMAN, 0));
        player.modifyMyTribe(createCharacter(JobType.FARMER, 0));
        player.modifyMyTribe(createCharacter(JobType.ARTIST, 0));
        player.modifyMyTribe(createCharacter(JobType.INVENTOR, 0));

        // Aggiungiamo un doppione per verificare che conti solo i set completi
        player.modifyMyTribe(createCharacter(JobType.HUNTER, 0));

        // 2. CALCOLIAMO IL PUNTEGGIO BASE (Prima dell'effetto)
        // Questo catturerà eventuali punti intrinseci (es. quel famoso 1 punto dell'Artista)
        int scoreBase = game.calculatePlayerScore(player);

        // 3. Creiamo e inneschiamo l'edificio
        BuildingSixDifferentAura effetto = new BuildingSixDifferentAura();
        BuildingCard card = new BuildingCard(JobType.NULL, 0, 0, Phases.ENDGAME, List.of(effetto), 1);
        player.getBuildings().add(card);

        effetto.applyOnEndGame(player, game, card);

        // 4. CALCOLIAMO IL PUNTEGGIO FINALE (Dopo l'effetto)
        int scoreFinale = game.calculatePlayerScore(player);

        // MATEMATICA: Indipendentemente da quanti punti base avesse la tribù,
        // l'edificio DEVE averne aggiunti esattamente 6.
        assertEquals(scoreBase + 6, scoreFinale, "Un set completo dovrebbe aggiungere esattamente 6 punti extra al punteggio");
    }

    // ==========================================
    // TEST 3: Doppio dei punti per un Job (es. Costruttori)
    // ==========================================
    @Test
    public void testBuildingJobAuraBonus() {
        player.modifyMyTribe(createCharacter(JobType.CONSTRUCTOR, 5));
        player.modifyMyTribe(createCharacter(JobType.CONSTRUCTOR, 5));
        player.modifyMyTribe(createCharacter(JobType.HUNTER, 2));

        int scoreBase = game.calculatePlayerScore(player); // Dovrebbe essere 10 (se calcola solo CONSTRUCTOR) + eventuali altri bonus

        // 🛠️ IL FIX: Creiamo i parametri JSON "finti" come se li stessimo leggendo dal file
        JsonObject params = new JsonObject();
        params.addProperty("jobType", "CONSTRUCTOR");
        params.addProperty("useCardBaseAura", true); // Questo attiva il raddoppio dell'Aura base

        // Ora passiamo i parametri veri invece di 'null'!
        BuildingJobAuraBonus effetto = new BuildingJobAuraBonus(params);
        BuildingCard card = new BuildingCard(JobType.CONSTRUCTOR, 0, 0, Phases.ENDGAME, List.of(effetto), 1);
        player.getBuildings().add(card);

        effetto.applyOnEndGame(player, game, card);

        int scoreFinale = game.calculatePlayerScore(player);

        // MATEMATICA: L'edificio deve aver aggiunto ESATTAMENTE l'aura base dei costruttori (5 + 5 = +10)
        assertEquals(scoreBase + 10, scoreFinale, "I punti bonus dell'edificio dovrebbero raddoppiare esattamente i 10 punti base dei Costruttori");
    }
}