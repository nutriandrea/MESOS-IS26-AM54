package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test delle entità di base del model: Player, OfferingTile, TurnOrderSlot,
 * Deck e la gerarchia delle carte (Card / CharacterCard / BuildingCard / EventCard).
 * Copre getter/setter, modificatori di risorse, pick() e rappresentazioni client.
 */
public class ModelEntitiesTest {

    // ==========================================
    // PLAYER
    // ==========================================
    @Test
    public void testPlayerResourceModifiers() {
        Player p = new Player();
        p.setNickname("Tester");

        assertEquals("Tester", p.getNickname());
        assertEquals(0, p.getFood());
        assertEquals(0, p.getAuraPoints());
        assertEquals(0, p.getMagicPoints());

        p.modifyFood(5);
        assertEquals(5, p.getFood());
        assertEquals(5, p.getFoodTokens(), "getFoodTokens deve essere un alias di getFood");

        // Il cibo non può scendere sotto zero
        p.modifyFood(-10);
        assertEquals(0, p.getFood(), "Il cibo non deve mai diventare negativo");

        p.modifyAuraPoints(7);
        p.modifyAuraPoints(-3);
        assertEquals(4, p.getAuraPoints(), "L'aura può variare liberamente");

        p.modifyMagicPoints(2);
        assertEquals(2, p.getMagicPoints());
    }

    @Test
    public void testPlayerTotemAndFlags() {
        Player p = new Player();
        p.setNickname("Bob");
        p.setTotemColor(TotemColors.BLUE);
        assertEquals(TotemColors.BLUE, p.getTotemColors());

        assertFalse(p.hasPlacedTotem());
        p.setHasPlacedTotem(true);
        assertTrue(p.hasPlacedTotem());

        assertFalse(p.hasExtraPick());
        p.setExtraPick(true);
        assertTrue(p.hasExtraPick());

        OfferingTile tile = new OfferingTile('A', 1, 1, 0);
        p.setCurrentTile(tile);
        assertEquals(tile, p.getCurrentTile());
    }

    @Test
    public void testPlayerDiscountsAndEventModifiers() {
        Player p = new Player();
        p.setNickname("Eve");

        p.modifyMyBuildingDiscount(3);
        assertEquals(3, p.getMyBuildingDiscount());

        p.setEventCostFood(-2);
        p.setEventCostAura(1);
        p.setShamanNoLoss(true);
        p.setShamanDoubleAura(true);
        assertEquals(-2, p.getEventCostFood());
        assertEquals(1, p.getEventCostAura());
        assertTrue(p.hasShamanNoLoss());
        assertTrue(p.hasShamanDoubleAura());

        p.resetEventAttributes();
        assertEquals(0, p.getEventCostFood());
        assertEquals(0, p.getEventCostAura());
        assertFalse(p.hasShamanNoLoss());
        assertFalse(p.hasShamanDoubleAura());
    }

    @Test
    public void testPlayerEndGameAura() {
        Player p = new Player();
        p.setNickname("Ann");
        p.modifyAuraPoints(20);
        p.setEndGameBuildingsAura(5);

        assertEquals(5, p.getEndGameBuildingsAura());
        assertEquals(15, p.getBaseAuraBeforeEndGame(),
                "L'aura base esclude il delta degli edifici di fine partita");
    }

    @Test
    public void testPlayerEqualsAndHashCode() {
        Player a = new Player(); a.setNickname("Alice");
        Player b = new Player(); b.setNickname("ALICE"); // case-insensitive
        Player c = new Player(); c.setNickname("Bob");

        assertEquals(a, b, "Il confronto sul nickname è case-insensitive");
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "stringa");
        assertEquals(a, a);
    }

    // ==========================================
    // OFFERING TILE
    // ==========================================
    @Test
    public void testOfferingTilePicksLifecycle() {
        OfferingTile tile = new OfferingTile('B', 2, 1, 3);

        assertEquals('B', tile.getId());
        assertEquals(2, tile.getCardsFromUpper());
        assertEquals(1, tile.getCardsFromLower());
        assertEquals(3, tile.getFoodBonus());
        assertTrue(tile.isAvailable());

        Player p = new Player(); p.setNickname("Picker");
        tile.occupy(p);
        assertFalse(tile.isAvailable());
        assertEquals(p, tile.getOccupiedBy());
        assertEquals(2, tile.getRemainingUpperPicks());
        assertEquals(1, tile.getRemainingLowerPicks());

        assertTrue(tile.useUpperPick());
        assertEquals(1, tile.getRemainingUpperPicks());
        assertTrue(tile.useLowerPick());
        assertFalse(tile.useLowerPick(), "Niente più pick lower disponibili");

        tile.freeSlot();
        assertTrue(tile.isAvailable());
    }

    @Test
    public void testOfferingTileRestoreVsReset() {
        OfferingTile tile = new OfferingTile('C', 2, 2, 0);
        Player p = new Player(); p.setNickname("X");
        tile.occupy(p);
        tile.useUpperPick();
        tile.useLowerPick();
        assertEquals(1, tile.getRemainingUpperPicks());

        // restoreOccupant NON deve resettare i contatori (usato nel recovery JSON)
        tile.freeSlot();
        tile.restoreOccupant(p);
        assertEquals(p, tile.getOccupiedBy());
        assertEquals(1, tile.getRemainingUpperPicks(), "restoreOccupant preserva i pick usati");

        // resetPicks invece riporta tutto a pieno
        tile.resetPicks();
        assertEquals(2, tile.getRemainingUpperPicks());
        assertEquals(2, tile.getRemainingLowerPicks());
    }

    // ==========================================
    // TURN ORDER SLOT
    // ==========================================
    @Test
    public void testTurnOrderSlot() {
        TurnOrderSlot slot = new TurnOrderSlot(2, -1);
        assertEquals(2, slot.getPosition());
        assertEquals(-1, slot.getBonusFood());
        assertNull(slot.getOccupiedBy());

        Player p = new Player(); p.setNickname("Z");
        slot.occupy(p);
        assertEquals(p, slot.getOccupiedBy());
        slot.freeSlot();
        assertNull(slot.getOccupiedBy());
    }

    // ==========================================
    // DECK
    // ==========================================
    @Test
    public void testDeckDrawPeekAndEmpty() {
        Deck<TribeCard> empty = new Deck<>();
        assertTrue(empty.isEmpty());
        assertNull(empty.drawCard(), "drawCard su mazzo vuoto deve restituire null");
        assertNull(empty.peek());

        Stack<TribeCard> stack = new Stack<>();
        CharacterCard c1 = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1);
        CharacterCard c2 = new CharacterCard(2, JobType.HUNTER, 0, null, new ArrayList<>(), 1);
        stack.push(c1);
        stack.push(c2);

        Deck<TribeCard> deck = new Deck<>(stack);
        assertFalse(deck.isEmpty());
        assertEquals(c2, deck.peek(), "peek non rimuove la carta in cima");
        assertEquals(2, deck.getAllCards().size());
        assertEquals(c2, deck.drawCard(), "drawCard rimuove dalla cima (LIFO)");
        assertEquals(c1, deck.drawCard());
        assertTrue(deck.isEmpty());
    }

    // ==========================================
    // CARD HIERARCHY
    // ==========================================
    @Test
    public void testCardBaseDefaults() {
        Card card = new Card(2);
        assertEquals(2, card.getEra());
        card.setEra(3);
        assertEquals(3, card.getEra());

        assertEquals("0", card.getImageId());
        card.setImageId("42");
        assertEquals("42", card.getImageId());

        assertTrue(card.canBePurchased(new Player()), "Card base è sempre acquistabile");
        assertThrows(UnsupportedOperationException.class, () -> card.pick(new Player(), new Game()));
        assertThrows(UnsupportedOperationException.class, () -> card.reveal(new Board(), "upper"));
        assertThrows(UnsupportedOperationException.class, card::toClientString);
    }

    @Test
    public void testCharacterCardGettersAndClientString() {
        CharacterCard c = new CharacterCard(2, JobType.INVENTOR, 5, Phases.ENDGAME, new ArrayList<>(), 1);
        c.setMinPlayers(3);
        c.setImageId("7");

        assertEquals(2, c.getLevel());
        assertEquals(JobType.INVENTOR, c.getJobType());
        assertEquals(5, c.getAura());
        assertEquals(0, c.getCost(), "I personaggi non hanno costo in cibo");
        assertEquals(3, c.getMinPlayers());
        assertEquals("INVENTOR", c.getName());

        String cs = c.toClientString();
        assertTrue(cs.startsWith("7|1|3|INVENTOR|2|5"), "Formato client del personaggio: " + cs);
    }

    @Test
    public void testCharacterCardNameUnknownWhenNullType() {
        CharacterCard c = new CharacterCard(1, null, 0, null, new ArrayList<>(), 1);
        assertEquals("Unknown", c.getName());
    }

    @Test
    public void testCharacterCardPickAddsToTribe() throws Exception {
        Game g = new Game();
        Player p = new Player(); p.setNickname("Hunter1");
        g.getPlayers().add(p);

        // Un HUNTER di livello 1: l'effetto CharacterHunter dà 1 cibo (1 hunter in tribù)
        List<IEffect<CharacterCard>> effects = new ArrayList<>();
        effects.add(new it.polimi.ingsw.am54.model.effects.characters.CharacterHunter());
        CharacterCard hunter = new CharacterCard(1, JobType.HUNTER, 0, null, effects, 1);

        hunter.pick(p, g);
        assertEquals(1, p.getMyTribe().size(), "La carta deve essere aggiunta alla tribù");
        assertEquals(1, p.getFood(), "L'effetto del cacciatore deve dare 1 cibo");
    }

    @Test
    public void testCharacterCardRevealGoesToCharacterRow() {
        Board board = new Board();
        CharacterCard c = new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1);
        c.reveal(board, "upper");
        assertEquals(1, board.getCharacterRowSize("upper"));
    }

    @Test
    public void testBuildingCardGettersAndPurchase() throws Exception {
        Game g = new Game();
        Player p = new Player(); p.setNickname("Builder");
        g.getPlayers().add(p);
        p.modifyFood(10);

        BuildingCard b = new BuildingCard(JobType.CONSTRUCTOR, 4, 6, Phases.ENDGAME, new ArrayList<>(), 2);
        b.setName("Torre");
        b.setImageId("99");

        assertEquals(4, b.getCost());
        assertEquals(6, b.getFinalAura());
        assertEquals(JobType.CONSTRUCTOR, b.getBuildingType());
        assertEquals("Torre", b.getName());
        assertNotNull(b.getEffects());
        assertTrue(b.canBePurchased(p));

        b.pick(p, g);
        assertEquals(6, p.getFood(), "Deve scalare il costo (10 - 4)");
        assertEquals(1, p.getBuildings().size());

        String cs = b.toClientString();
        assertTrue(cs.contains("BUILDING") && cs.contains("Torre"), "Formato client edificio: " + cs);
    }

    @Test
    public void testBuildingCardDiscountAndInsufficientFood() throws Exception {
        Game g = new Game();
        Player p = new Player(); p.setNickname("Poor");
        g.getPlayers().add(p);
        p.modifyFood(2);

        BuildingCard expensive = new BuildingCard(JobType.CONSTRUCTOR, 5, 0, null, new ArrayList<>(), 1);
        assertFalse(expensive.canBePurchased(p), "Cibo insufficiente");
        Exception ex = assertThrows(Exception.class, () -> expensive.pick(p, g));
        assertTrue(ex.getMessage().toLowerCase().contains("cibo"));

        // Con sconto pari al costo, diventa gratis
        p.modifyMyBuildingDiscount(5);
        assertTrue(expensive.canBePurchased(p), "Lo sconto azzera il costo");
        expensive.pick(p, g);
        assertEquals(2, p.getFood(), "Costo azzerato dallo sconto: nessuna spesa");
    }

    @Test
    public void testBuildingCardRevealGoesToBuildingRow() {
        Board board = new Board();
        BuildingCard b = new BuildingCard(JobType.HUNTER, 0, 0, null, new ArrayList<>(), 1);
        b.reveal(board, "lower");
        assertEquals(1, board.getBuildingRowSize("lower"));
    }

    @Test
    public void testEventCardGettersAndClientString() {
        EventCard e = new EventCard(2, true, new ArrayList<>(), 1, 2, 3, 4, 5, 6, 7);
        e.setName("HUNTER");
        e.setImageId("3");

        assertEquals(1, e.getBaseFood());
        assertEquals(2, e.getAuraPerCharacter());
        assertEquals(3, e.getThreshold());
        assertEquals(4, e.getBonusMultiplier());
        assertEquals(5, e.getPenalty());
        assertEquals(6, e.getBonus());
        assertEquals(7, e.getMalus());
        assertTrue(e.isFinalEvent());
        assertTrue(e.matchesJobType(JobType.HUNTER));
        assertFalse(e.matchesJobType(JobType.FARMER));
        assertFalse(e.isSustenanceEvent(), "Il nome è HUNTER, non FARMER");

        EventCard farmer = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0);
        farmer.setName("FARMER");
        assertTrue(farmer.isSustenanceEvent());
        assertTrue(farmer.getName().contains("FARMER"));

        String cs = e.toClientString();
        assertTrue(cs.contains("EVENT"), "Formato client evento: " + cs);
    }

    @Test
    public void testEventCardRevealGoesToEventRow() {
        Board board = new Board();
        EventCard e = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0);
        e.reveal(board, "upper");
        assertEquals(1, board.getEventRowSize("upper"));
    }
}
