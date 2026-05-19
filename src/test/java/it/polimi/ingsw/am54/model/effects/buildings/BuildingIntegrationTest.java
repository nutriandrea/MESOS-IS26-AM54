package it.polimi.ingsw.am54.model.effects.buildings;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.effects.buildings.*;
import it.polimi.ingsw.am54.model.states.PickingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BuildingIntegrationTest {

    private Game game;
    private Board board;
    private Player p1;
    private PickingState pickingState;

    @BeforeEach
    public void setUp() {
        // 1. CREIAMO UN MONDO VUOTO
        game = new Game();
        board = new Board();
        game.setBoard(board);

        p1 = new Player();
        p1.setNickname("Tester");
        game.getPlayers().add(p1);
        game.setCurrentPlayer(p1);
        game.setActivePlayerCount(1);

        pickingState = new PickingState();
        game.setState(pickingState);
    }

    @Test
    public void testExtraPick_TurnFlowExecution() throws Exception {
        // ========================================================
        // FASE 1: SETUP DEL TAVOLO (2 Giocatori)
        // ========================================================
        Player p2 = new Player();
        p2.setNickname("Bob");
        game.getPlayers().add(p2);
        game.setActivePlayerCount(2);
        // Diamo al gioco un mazzo vuoto fittizio per non farlo esplodere quando controlla la fine del round
        game.setTribeDeck(new Deck<>(new Stack<>()));

        board.initializeLayout(2);
        p1.modifyFood(10);
        p2.modifyFood(10);

        // Mettiamo le carte fittizie nella plancia per non farla svuotare
        BuildingCard dummy1 = new BuildingCard(JobType.FARMER, 0, 0, null, new ArrayList<>(), 1);
        BuildingCard dummy2 = new BuildingCard(JobType.FARMER, 0, 0, null, new ArrayList<>(), 1);
        BuildingCard dummy3 = new BuildingCard(JobType.FARMER, 0, 0, null, new ArrayList<>(), 1);

        board.getBuildingCardsUpperRow().add(dummy1);
        board.getBuildingCardsUpperRow().add(dummy2);
        board.getBuildingCardsUpperRow().add(dummy3);

        // IL TRUCCO DEFINITIVO SULLE TESSERE:
        // Creiamo due tessere sicure al 100% che hanno "1 pick upper"
        OfferingTile safeTile1 = new OfferingTile('A', 1, 0, 0);
        OfferingTile safeTile2 = new OfferingTile('B', 1, 0, 1);

        // Le andiamo a sovrascrivere FISICAMENTE sulla plancia di gioco
        List<OfferingTile> track = board.getOfferingTrack();
        track.set(0, safeTile1);
        track.set(1, safeTile2);

        // Ora le occupiamo con i giocatori
        safeTile1.occupy(p1);
        p1.setCurrentTile(safeTile1);

        safeTile2.occupy(p2);
        p2.setCurrentTile(safeTile2);

        game.setCurrentPlayer(p1);
        p1.setExtraPick(true);

        // ========================================================
        // FASE 2: IL GIRO NORMALE
        // ========================================================

        // 1. Tester (p1) fa il suo turno NORMALE.
        // Ora ha sicuramente il pick upper grazie alla safeTile1!
        pickingState.pickCard(game, "Tester", "upper", 0);

        // VERIFICA CHIAVE 1
        assertEquals(p2, game.getCurrentPlayer(), "Il turno deve passare a Bob, l'Extra Pick si usa alla fine!");
        assertNull(p1.getCurrentTile(), "Tester deve aver restituito il totem dopo il suo turno normale");

        // 2. Bob (p2) fa il suo turno normale. Facciamo saltare il suo turno.
        pickingState.skipTurn(game, "Bob");

        // ========================================================
        // FASE 3: IL TURNO BONUS
        // ========================================================

        // VERIFICA CHIAVE 2
        assertEquals(p1, game.getCurrentPlayer(), "Il gioco deve aver assegnato il Turno Bonus a Tester");
        assertTrue(p1.hasExtraPick(), "Tester ha ancora il gettone del bonus intatto");

        // 3. Tester (p1) consuma il suo Extra Pick sulla terza carta finta rimasta.
        pickingState.pickCard(game, "Tester", "upper", 0);

        // VERIFICA CHIAVE 3
        assertFalse(p1.hasExtraPick(), "Il bonus deve essere stato consumato");
        assertNotEquals(pickingState, game.getCurrentState(), "Finito il bonus, il gioco deve passare agli Eventi o a Fine Partita");
    }

    @Test
    public void testBuilding_25AuraEndgame() throws Exception {
        // ========================================================
        // FASE 1: SETUP DEL TAVOLO (Configurato per 2 per non far esplodere la Board)
        // ========================================================
        Player p2 = new Player();
        p2.setNickname("Bob");
        game.getPlayers().add(p2);
        game.setActivePlayerCount(2);
        board.initializeLayout(2); // ORA ACCETTERA' IL SETUP!

        p1.modifyFood(10); // Diamo cibo per poter comprare la carta

        // Annotiamo quanti punti Aura ha all'inizio (dovrebbe essere 0)
        int initialAura = p1.getAuraPoints();

        // TRUCCO JSON: Creiamo al volo un finto file JSON per l'effetto
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "{\"aura\": 25, \"magic\": 0, \"extraPick\": false, \"isEndgame\": true}";
        JsonNode params = mapper.readTree(jsonString);

        // Istanziamo il VERO effetto
        IEffect<BuildingCard> endgameEffect = new BuildingFlatBonus(params);

        // Creiamo la carta e la infiliamo nella plancia
        BuildingCard card25Aura = new BuildingCard(JobType.FARMER, 5, 0, null, List.of(endgameEffect), 99);
        board.getBuildingCardsUpperRow().add(card25Aura);

        // Mettiamo il giocatore sulla tessera per farlo pescare
        OfferingTile safeTile = new OfferingTile('A', 1, 0, 0);
        safeTile.occupy(p1);
        p1.setCurrentTile(safeTile);
        game.setCurrentPlayer(p1);

        // ========================================================
        // FASE 2: L'ACQUISTO
        // ========================================================
        // Il giocatore compra l'edificio (indice 0 in upper)
        pickingState.pickCard(game, "Tester", "upper", 0);

        // VERIFICA CHIAVE 1: La carta è sua, MA i punti non devono essere ancora assegnati!
        assertTrue(p1.getBuildings().contains(card25Aura), "La carta deve essere nell'inventario del giocatore");
        assertEquals(initialAura, p1.getAuraPoints(), "ERRORE: I punti Aura sono stati assegnati subito, ma dovevano aspettare il fine partita!");

        // ========================================================
        // FASE 3: FINE PARTITA
        // ========================================================
        // Simuliamo la chiamata finale del gioco che conta i punti
        game.determineWinners();

        // VERIFICA CHIAVE 2: Ora i punti devono esserci!
        assertEquals(initialAura + 25, p1.getAuraPoints(), "A fine partita, il giocatore deve aver ricevuto i 25 punti Aura!");
    }
}