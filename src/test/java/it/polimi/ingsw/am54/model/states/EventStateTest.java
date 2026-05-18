package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Stack;

public class EventStateTest {

    private Game game;
    private Board board;
    private Player p1;
    private EventState eventState;

    @BeforeEach
    public void setUp() {
        game = new Game();
        board = new Board();
        game.setBoard(board);
        eventState = new EventState();
        game.setState(eventState);

        p1 = new Player();
        p1.setNickname("Alice");
        game.getPlayers().add(p1);

        // Setup base di plancia per evitare NullPointer
        board.initializeLayout(2);

        // Assegniamo un TurnOrderSlot fittizio ad Alice per non far fallire la fine del turno
        TurnOrderSlot slot = board.getTurnOrderTile().get(0);
        slot.occupy(p1);
    }

    @Test
    public void testResolvePhase_StandardRoundFlow() {
        // 1. SETUP: Creiamo un Evento Sostentamento "Finto" che toglie 2 cibo
        EventCard mockSostentamento = new EventCard(1, false, new ArrayList<>(), 0, 0, 0, 0, 0, 0, 0) {
            @Override
            public void resolveEvent(Game g) {
                // Simuliamo la carestia/sostentamento
                p1.modifyFood(-2);
            }
            @Override public boolean shouldResolveLast() { return true; }
        };

        board.getEventLowerRow().add(mockSostentamento);

        // Prepariamo il mazzo Tribù per evitare la transizione di Era e i NullPointer
        Stack<TribeCard> deckStack = new Stack<>();
        deckStack.push(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1) {
            @Override public int getEra() { return 1; }
        });
        game.setTribeDeck(new Deck<>(deckStack));

        // Mettiamo un personaggio fittizio in alto per testare lo scivolamento
        board.addCharacterCard(new CharacterCard(1, JobType.FARMER, 0, null, new ArrayList<>(), 1), "upper");

        p1.modifyFood(5);
        p1.setHasPlacedTotem(true); // Alice ha piazzato il totem durante il round

        int initialRound = game.getRoundCount();

        // 2. ESECUZIONE
        eventState.resolvePhase(game);

        // 3. VERIFICHE
        // A. L'evento è stato risolto? (Il cibo deve essere 5 - 2 = 3)
        assertEquals(3, p1.getFood(), "L'evento Sostentamento doveva essere risolto sottraendo 2 cibo");

        // B. Pulizia della Plancia
        assertEquals(0, board.getEventLowerRow().size(), "La fila eventi in basso deve essere stata svuotata");
        assertEquals(1, board.getCharacterRowSize("lower"), "Il personaggio in alto deve essere scivolato in basso");

        // C. Transizione di Round
        assertEquals(initialRound + 1, game.getRoundCount(), "Il round deve essere avanzato di 1");
        assertFalse(p1.hasPlacedTotem(), "Il totem di Alice deve essere stato resettato a false");

        // D. Transizione di Stato
        assertTrue(game.getCurrentState() instanceof StartTurnState, "Lo stato finale deve essere StartTurnState");
    }

    @Test
    public void testResolvePhase_EraTransition_ToEra3() {
        // 1. SETUP: Simuliamo il passaggio all'Era 3
        game.setCurrentEra(2);

        Stack<TribeCard> deckStack = new Stack<>();

        // A. Inseriamo PRIMA la carta di Era 3 (andrà sul fondo dello Stack)
        deckStack.push(new CharacterCard(3, JobType.FARMER, 0, null, new ArrayList<>(), 1) {
            @Override public int getEra() { return 3; }
        });

        // B. Inseriamo SOPRA 5 carte di Era 2.
        // Così il refillUpperTribeRow pescherà queste 5 e si fermerà,
        // lasciando la carta di Era 3 scoperta in cima al mazzo!
        int cardsToDraw = game.getPlayersNumber() + 4; // 1 giocatore + 4 = 5
        for (int i = 0; i < cardsToDraw; i++) {
            deckStack.push(new CharacterCard(2, JobType.FARMER, 0, null, new ArrayList<>(), 1) {
                @Override public int getEra() { return 2; }
            });
        }

        game.setTribeDeck(new Deck<>(deckStack));
        game.setBuildingDeck(new Deck<>(new Stack<>())); // Evita NullPointer

        // Inseriamo edifici fittizi per testare lo scivolamento e la distruzione (Regola Era III)
        BuildingCard oldLowerBuilding = new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1);
        BuildingCard upperBuilding = new BuildingCard(null, 0, 0, null, new ArrayList<>(), 1);

        board.addBuildingCard(oldLowerBuilding, "lower");
        board.addBuildingCard(upperBuilding, "upper");

        // 2. ESECUZIONE
        eventState.resolvePhase(game);

        // 3. VERIFICHE
        assertEquals(3, game.getCurrentEra(), "L'Era deve essere avanzata a 3");

        // Regola critica: gli edifici che erano in basso devono essere distrutti.
        assertEquals(1, board.getBuildingRowSize("lower"), "La fila inferiore deve contenere solo l'edificio sceso dall'alto");
        assertEquals(upperBuilding, board.getBuildingCardsLowerRow().get(0), "L'edificio in basso deve essere quello che prima era in alto");
    }
}