package it.polimi.ingsw.am54.model.effects.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.effects.buildings.BuildingShamanPassive;

import java.util.ArrayList;
import java.util.List;

public class EventEffectsTest {

    private Game game;
    private Player p1, p2, p3;
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        game = new Game();
        game.setBoard(new Board()); // Essenziale per evitare NullPointer

        p1 = new Player();
        p2 = new Player();
        p3 = new Player();

        // Aggiungiamo 3 giocatori per testare bene il Rituale Sciamanico (max/min)
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);
        game.getPlayers().add(p3);

        mapper = new ObjectMapper();
    }

    // =========================================================================
    // METODI DI SUPPORTO
    // =========================================================================

    private JsonNode creaJson(String jsonString) {
        try {
            return mapper.readTree(jsonString);
        } catch (Exception e) {
            fail("Errore JSON");
            return null;
        }
    }

    private void aggiungiPersonaggio(Player p, JobType type, int level) {
        CharacterCard card = new CharacterCard(level, type, 0, null, new ArrayList<>(), 1);
        p.getMyTribe().add(card);
    }

    /**
     * Usa il VERO costruttore di EventCard passandogli i parametri necessari.
     * I campi non rilevanti per la logica matematica (era, finalEvent, effects)
     * vengono inizializzati con valori di default.
     */
    private EventCard creaEvento(int threshold, int penalty, int bonusMult, int baseFood, int auraPerChar, int bonus, int malus) {
        return new EventCard(
                1,                  // era
                false,              // finalEvent
                new ArrayList<>(),  // effects
                baseFood,           // baseFood
                auraPerChar,        // auraPerCharacter
                threshold,          // threshold
                bonusMult,          // bonusMultiplier
                penalty,            // penalty
                bonus,              // bonus
                malus               // malus
        );
    }

    // =========================================================================
    // 1. TEST EVENTO ARTISTA
    // =========================================================================

    @Test
    public void testArtist_SottoSoglia() {
        // Soglia = 1, Penalità = 3, BonusMultiplier = 0
        EventCard evento = creaEvento(1, 3, 0, 0, 0, 0, 0);
        EventArtist effect = new EventArtist();

        // p1 ha 0 artisti (sotto la soglia di 1)
        effect.apply(p1, game, evento);
        assertEquals(-3, p1.getAuraPoints(), "Senza artisti sotto la soglia deve perdere 3 Aura");
    }

    @Test
    public void testArtist_SopraSoglia() {
        // Soglia = 1, Penalità = 3, BonusMultiplier = 2
        EventCard evento = creaEvento(1, 3, 2, 0, 0, 0, 0);
        EventArtist effect = new EventArtist();

        aggiungiPersonaggio(p1, JobType.ARTIST, 1);
        aggiungiPersonaggio(p1, JobType.ARTIST, 1); // 2 Artisti (sopra la soglia di 1)

        effect.apply(p1, game, evento);
        assertEquals(4, p1.getAuraPoints(), "2 Artisti * 2 (moltiplicatore) = 4 Aura");
    }

    // =========================================================================
    // 2. TEST EVENTO CACCIATORE
    // =========================================================================

    @Test
    public void testHunter_NormaleConBuildingBonus() {
        // BaseFood = 1, AuraPerChar = 2
        EventCard evento = creaEvento(0, 0, 0, 1, 2, 0, 0);
        EventHunter effect = new EventHunter();

        aggiungiPersonaggio(p1, JobType.HUNTER, 1);
        aggiungiPersonaggio(p1, JobType.HUNTER, 1); // 2 Cacciatori

        // Simuliamo che il giocatore abbia un Edificio che dà +3 Aura durante la caccia
        p1.setEventCostAura(3);

        effect.apply(p1, game, evento);

        assertEquals(2, p1.getFood(), "2 Cacciatori * 1 BaseFood = 2 Cibo");
        assertEquals(7, p1.getAuraPoints(), "(2 Cacciatori * 2 Aura) + 3 Bonus Edificio = 7 Aura");
    }

    // =========================================================================
    // 3. TEST EVENTO SCIAMANO
    // =========================================================================

    @Test
    public void testShaman_NormaleMaxMin() {
        // Bonus = 10, Malus = 5
        EventCard evento = creaEvento(0, 0, 0, 0, 0, 10, 5);
        EventShaman effect = new EventShaman();

        p1.modifyMagicPoints(5); // MAX
        p2.modifyMagicPoints(2); // Mezzo
        p3.modifyMagicPoints(0); // MIN

        effect.apply(p1, game, evento); // Apply cicla su tutti i giocatori in automatico

        assertEquals(10, p1.getAuraPoints(), "Il MAX guadagna il bonus");
        assertEquals(0, p2.getAuraPoints(), "Chi sta in mezzo non prende nulla");
        assertEquals(-5, p3.getAuraPoints(), "Il MIN subisce il malus");
    }

    @Test
    public void testShaman_ugualiValori() {
        // Bonus = 10, Malus = 5
        EventCard evento = creaEvento(0, 0, 0, 0, 0, 10, 5);
        EventShaman effect = new EventShaman();

        p1.modifyMagicPoints(5); // MAX
        p2.modifyMagicPoints(5); // MAX
        p3.modifyMagicPoints(5); // MAX

        effect.apply(p1, game, evento); // Apply cicla su tutti i giocatori in automatico

        assertEquals(5, p1.getAuraPoints(), "Pareggio assoluto: P1 deve guadagnare 10 e perdere 5 (Totale netto 5)");
        assertEquals(5, p2.getAuraPoints(), "Pareggio assoluto: P2 deve guadagnare 10 e perdere 5 (Totale netto 5)");
        assertEquals(5, p3.getAuraPoints(), "Pareggio assoluto: P3 deve guadagnare 10 e perdere 5 (Totale netto 5)");
    }

    @Test
    public void testShaman_ConEdificiPassivi() {
        EventCard evento = creaEvento(0, 0, 0, 0, 0, 10, 5);
        EventShaman effect = new EventShaman();

        p1.modifyMagicPoints(5); // MAX
        p3.modifyMagicPoints(0); // MIN

        // Diamo a P1 l'Edificio "DoubleAura"
        BuildingShamanPassive doubleAuraEffect = new BuildingShamanPassive(creaJson("{\"doubleAura\": true, \"noLoss\": false}"));
        BuildingCard cardP1 = new BuildingCard(null, 0, 0, null, List.of(doubleAuraEffect), 1);
        p1.getBuildings().add(cardP1);

        // Diamo a P3 l'Edificio "NoLoss"
        BuildingShamanPassive noLossEffect = new BuildingShamanPassive(creaJson("{\"doubleAura\": false, \"noLoss\": true}"));
        BuildingCard cardP3 = new BuildingCard(null, 0, 0, null, List.of(noLossEffect), 1);
        p3.getBuildings().add(cardP3);

        effect.apply(p1, game, evento);

        assertEquals(20, p1.getAuraPoints(), "Il bonus di 10 deve essere raddoppiato a 20");
        assertEquals(0, p3.getAuraPoints(), "Il malus di -5 deve essere annullato dall'edificio");
    }

    // =========================================================================
    // 4. TEST EVENTO RACCOGLITORE (FARMER - SOSTENTAMENTO)
    // =========================================================================

    @Test
    public void testFarmer_MetodiBase() {
        EventFarmer effect = new EventFarmer();
        assertTrue(effect.isFarmerEffect(), "Deve essere riconosciuto come Farmer Effect");
        assertTrue(effect.shouldResolveLast(), "Il Sostentamento si risolve per ultimo");
    }

    @Test
    public void testFarmer_ScontoMaggioreDelCosto() {
        EventCard evento = creaEvento(0, 0, 0, 0, 0, 0, 0);
        EventFarmer effect = new EventFarmer();

        aggiungiPersonaggio(p1, JobType.FARMER, 1); // Costo 1, Sconto 3. Costo Netto: -2.

        effect.apply(p1, game, evento);
        assertEquals(0, p1.getFood(), "Non deve guadagnare cibo, lo sconto assorbe solo il costo");
    }

    @Test
    public void testFarmer_PagaTuttoRegolarmente() {
        EventCard evento = creaEvento(0, 0, 0, 0, 0, 0, 0);
        EventFarmer effect = new EventFarmer();

        p1.modifyFood(5); // Ha 5 cibo
        aggiungiPersonaggio(p1, JobType.ARTIST, 1);
        aggiungiPersonaggio(p1, JobType.ARTIST, 1); // Tribù di 2. Costo 2. Sconto 0.

        effect.apply(p1, game, evento);
        assertEquals(3, p1.getFood(), "5 Cibo - 2 Costo = 3 Cibo rimanente");
    }

    @Test
    public void testFarmer_CiboInsufficienteConPenalita() {
        // AuraPerChar (Malus) = 3
        EventCard evento = creaEvento(0, 0, 0, 0, 3, 0, 0);
        EventFarmer effect = new EventFarmer();

        p1.modifyFood(1); // Ha solo 1 cibo
        aggiungiPersonaggio(p1, JobType.ARTIST, 1);
        aggiungiPersonaggio(p1, JobType.ARTIST, 1);
        aggiungiPersonaggio(p1, JobType.ARTIST, 1); // Costo = 3. Cibo mancante = 2.

        effect.apply(p1, game, evento);
        assertEquals(0, p1.getFood(), "Il cibo viene azzerato");
        assertEquals(-6, p1.getAuraPoints(), "2 Cibo mancante * 3 Aura di malus = -6 Aura");
    }
}