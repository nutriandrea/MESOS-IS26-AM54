package it.polimi.ingsw.am54.model.effects.buildings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am54.model.*;

import java.util.ArrayList;

public class BuildingEffectsTest {

    private Player player;
    private Game game;
    private BuildingCard dummyCard;
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        // ARRANGE GENERALE: Prima di ogni test, creiamo un ambiente pulito
        player = new Player();
        game = new Game(); // Nota: per alcuni test potrebbe servire inizializzare la Board
        // FIX: Diamo una plancia al Game prima di avviare i test!
        game.setBoard(new Board());
        dummyCard = new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1);// Passa i parametri fittizi richiesti dal tuo costruttore
        mapper = new ObjectMapper();
    }

    // --- METODI DI SUPPORTO ---

    private JsonNode creaJson(String jsonString) {
        try {
            return mapper.readTree(jsonString);
        } catch (Exception e) {
            fail("Errore nella sintassi del JSON di test");
            return null;
        }
    }

    private void aggiungiPersonaggio(JobType type, int level, int aura) {
        // Modifica questa riga in base al VERO costruttore della tua CharacterCard
        CharacterCard card = new CharacterCard(level, type, aura, null, new ArrayList<>(), 1);
        player.getMyTribe().add(card);
    }

    // =========================================================================
    // 1. TEST DELLA CLASSE: BuildingFlatBonus (Copre 3 Edifici)
    // =========================================================================

    @Test
    public void testFlatBonus_25AuraEndgame() {
        JsonNode params = creaJson("{\"aura\": 25, \"isEndgame\": true}");
        BuildingFlatBonus effect = new BuildingFlatBonus(params);

        assertTrue(effect.isEndgameEffect(), "L'effetto deve essere riconosciuto come Endgame");

        effect.apply(player, game, dummyCard);
        assertEquals(25, player.getAuraPoints(), "L'aura deve essere aumentata di 25");
    }

    @Test
    public void testFlatBonus_ThreeMagic() {
        JsonNode params = creaJson("{\"magic\": 3}");
        BuildingFlatBonus effect = new BuildingFlatBonus(params);

        effect.apply(player, game, dummyCard);
        assertEquals(3, player.getMagicPoints(), "La magia deve essere aumentata di 3");
    }

    @Test
    public void testFlatBonus_ExtraPick() {
        JsonNode params = creaJson("{\"extraPick\": true}");
        BuildingFlatBonus effect = new BuildingFlatBonus(params);

        effect.apply(player, game, dummyCard);
        assertTrue(player.hasExtraPick(), "Il flag ExtraPick deve essere true");
    }

    // =========================================================================
    // 2. TEST DELLA CLASSE: BuildingJobAuraBonus (Copre 7 Edifici)
    // =========================================================================

    @Test
    public void testJobAuraBonus_Multiplier() {
        // Simulo l'edificio che dà 4 Aura per ogni Contadino
        JsonNode params = creaJson("{\"jobType\": \"FARMER\", \"multiplier\": 4}");
        BuildingJobAuraBonus effect = new BuildingJobAuraBonus(params);

        aggiungiPersonaggio(JobType.FARMER, 1, 0);
        aggiungiPersonaggio(JobType.FARMER, 1, 0);
        aggiungiPersonaggio(JobType.HUNTER, 1, 0); // Non deve essere contato

        effect.apply(player, game, dummyCard);
        assertEquals(8, player.getAuraPoints(), "2 Contadini x 4 = 8 Aura");
    }

    @Test
    public void testJobAuraBonus_ConstructorBaseAura() {
        // Simulo l'edificio che somma l'aura base stampata sulle carte Costruttore
        JsonNode params = creaJson("{\"jobType\": \"CONSTRUCTOR\", \"useCardBaseAura\": true}");
        BuildingJobAuraBonus effect = new BuildingJobAuraBonus(params);

        aggiungiPersonaggio(JobType.CONSTRUCTOR, 1, 3); // Costruttore con 3 Aura
        aggiungiPersonaggio(JobType.CONSTRUCTOR, 2, 5); // Costruttore con 5 Aura

        effect.apply(player, game, dummyCard);
        assertEquals(8, player.getAuraPoints(), "Deve sommare l'Aura nativa: 3 + 5 = 8 Aura");
    }

    // =========================================================================
    // 3. TEST DELLA CLASSE: BuildingEventCostModifier (Copre 5 Edifici)
    // =========================================================================

    @Test
    public void testEventCostModifier_FoodDiscount() {
        // Edificio Sconto Evento: -1 Cibo per ogni Inventore
        JsonNode params = creaJson("{\"jobType\": \"INVENTOR\", \"foodDiscountMultiplier\": -1}");
        BuildingEventCostModifier effect = new BuildingEventCostModifier(params);

        aggiungiPersonaggio(JobType.INVENTOR, 1, 0);
        aggiungiPersonaggio(JobType.INVENTOR, 2, 0);

        effect.apply(player, game, dummyCard);
        assertEquals(-2, player.getEventCostFood(), "Lo sconto cibo per l'evento deve essere -2");
    }

    @Test
    public void testEventCostModifier_HunterBonusOverride() {
        // Edificio Caccia: L'evento considera sia override Cibo che Aura
        JsonNode params = creaJson("{\"jobType\": \"HUNTER\", \"overrideFoodCost\": true, \"overrideAuraCost\": true}");
        BuildingEventCostModifier effect = new BuildingEventCostModifier(params);

        aggiungiPersonaggio(JobType.HUNTER, 1, 0);
        aggiungiPersonaggio(JobType.HUNTER, 1, 0);

        effect.apply(player, game, dummyCard);
        assertEquals(2, player.getEventCostFood(), "Bonus cibo impostato a 2");
        assertEquals(2, player.getEventCostAura(), "Bonus aura impostato a 2");
    }

    // =========================================================================
    // 4. TEST DELLE CLASSI COMPLESSE (Le "Intoccabili")
    // =========================================================================

    @Test
    public void testSixOfType_IlBugCorretto() {
        BuildingSixOfType effect = new BuildingSixOfType();

        // Test 1: Tribù incompleta
        aggiungiPersonaggio(JobType.HUNTER, 1, 0);
        aggiungiPersonaggio(JobType.FARMER, 1, 0);
        effect.apply(player, game, dummyCard);
        assertEquals(0, player.getFood(), "Set non completo, cibo = 0");

        // Test 2: Set completo di 6 lavori diversi
        aggiungiPersonaggio(JobType.CONSTRUCTOR, 1, 0);
        aggiungiPersonaggio(JobType.SHAMAN, 1, 0);
        aggiungiPersonaggio(JobType.ARTIST, 1, 0);
        aggiungiPersonaggio(JobType.INVENTOR, 1, 0); // L'ultimo aggiunto triggera il controllo

        effect.apply(player, game, dummyCard);
        assertEquals(5, player.getFood(), "Set completo, deve dare esattamente 5 Cibo (il bug che abbiamo risolto!)");
    }

    @Test
    public void testDoubleInventor_SoloStessoLivello() {
        BuildingDoubleInventor effect = new BuildingDoubleInventor();

        // Primo inventore di Livello 1 (Non deve dare cibo)
        aggiungiPersonaggio(JobType.INVENTOR, 1, 0);
        effect.apply(player, game, dummyCard);
        assertEquals(0, player.getFood());

        // Inventore di Livello 2 (Non fa coppia, non deve dare cibo)
        aggiungiPersonaggio(JobType.INVENTOR, 2, 0);
        effect.apply(player, game, dummyCard);
        assertEquals(0, player.getFood());

        // Secondo inventore di Livello 2 (Fa coppia, deve dare +3 cibo)
        aggiungiPersonaggio(JobType.INVENTOR, 2, 0);
        effect.apply(player, game, dummyCard);
        assertEquals(3, player.getFood(), "Coppia dello stesso livello trovata, +3 Cibo");
    }

    @Test
    public void testSixDifferentAura_Endgame() {
        BuildingSixDifferentAura effect = new BuildingSixDifferentAura();

        // Mettiamo 2 set completi
        for (int i = 0; i < 2; i++) {
            aggiungiPersonaggio(JobType.HUNTER, 1, 0);
            aggiungiPersonaggio(JobType.FARMER, 1, 0);
            aggiungiPersonaggio(JobType.CONSTRUCTOR, 1, 0);
            aggiungiPersonaggio(JobType.SHAMAN, 1, 0);
            aggiungiPersonaggio(JobType.ARTIST, 1, 0);
            aggiungiPersonaggio(JobType.INVENTOR, 1, 0);
        }

        effect.apply(player, game, dummyCard);
        assertEquals(12, player.getAuraPoints(), "2 set completi * 6 punti = 12 Aura");
    }

    @Test
    public void testShamanPassive_Marcatorix() {
        JsonNode params = creaJson("{\"noLoss\": true, \"doubleAura\": false}");
        BuildingShamanPassive effect = new BuildingShamanPassive(params);

        assertTrue(effect.isNoLossEffect());
        assertFalse(effect.isDoubleAuraShamanEffect());
    }

    @Test
    public void testSixDifferentAura_MetodiBase() {
        BuildingSixDifferentAura effect = new BuildingSixDifferentAura();
        assertTrue(effect.isEndgameEffect(), "Deve essere un effetto di fine partita");
    }

    @Test
    public void testJobAuraBonus_EdgeCases() {
        // Testiamo l'isEndgame e il caso in cui il JSON sia vuoto/nullo
        BuildingJobAuraBonus effect = new BuildingJobAuraBonus(null);
        assertTrue(effect.isEndgameEffect());
        effect.apply(player, game, dummyCard); // jobType è nullo, deve fare return senza crashare
    }

    @Test
    public void testShamanPassive_Marcatori() {
        JsonNode params = creaJson("{\"noLoss\": true, \"doubleAura\": false}");
        BuildingShamanPassive effect = new BuildingShamanPassive(params);

        assertTrue(effect.isNoLossEffect());
        assertFalse(effect.isDoubleAuraShamanEffect());

        // Aggiungi questa riga per coprire il metodo vuoto
        effect.apply(player, game, dummyCard);
    }

    // =========================================================================
    // 5. TEST DELLA CLASSE: BuildingTurnOrderBonus (Il Boss Finale!)
    // =========================================================================

    @Test
    public void testTurnOrderBonus_GiocatoreSuSlotConBonus() {
        BuildingTurnOrderBonus effect = new BuildingTurnOrderBonus();

        // Creiamo uno slot in posizione 1, con un bonus di 1 cibo, e lo assegniamo al player
        TurnOrderSlot slot = new TurnOrderSlot(1, 1);
        slot.occupy(player); // Usa il metodo corretto se si chiama diversamente nel tuo codice

        // Aggiungiamo lo slot alla Board fittizia del nostro Game
        game.getBoard().getTurnOrderTile().add(slot);

        effect.apply(player, game, dummyCard);

        assertEquals(1, player.getFood(), "Il giocatore è su uno slot bonus, deve ricevere 1 cibo");
    }

    @Test
    public void testTurnOrderBonus_GiocatoreSuSlotSenzaBonus() {
        BuildingTurnOrderBonus effect = new BuildingTurnOrderBonus();

        // Creiamo uno slot in posizione 2, con bonus 0
        TurnOrderSlot slot = new TurnOrderSlot(2, 0);
        slot.occupy(player);

        game.getBoard().getTurnOrderTile().add(slot);

        effect.apply(player, game, dummyCard);

        assertEquals(0, player.getFood(), "Lo slot non ha bonus cibo, non deve ricevere nulla");
    }

    @Test
    public void testTurnOrderBonus_GiocatoreNonTrovato() {
        BuildingTurnOrderBonus effect = new BuildingTurnOrderBonus();

        // Lasciamo la Board vuota, il giocatore non ha ancora piazzato il Totem
        effect.apply(player, game, dummyCard);

        // Il codice deve usare l'orElse(null) e non deve crashare
        assertEquals(0, player.getFood(), "Il giocatore non è sul tracciato, nulla cambia");
    }
}