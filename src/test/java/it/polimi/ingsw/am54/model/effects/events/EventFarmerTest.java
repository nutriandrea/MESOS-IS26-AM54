package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventFarmerTest {

    private Game game;
    private Player player;
    private EventFarmer eventFarmer;
    private EventCard mockEventCard;

    @BeforeEach
    public void setUp() {
        game = new Game();
        player = new Player();
        player.setNickname("TestPlayer");

        // Give the player some initial resources
        player.modifyFood(10);
        player.modifyAuraPoints(20);

        game.getPlayers().add(player);

        eventFarmer = new EventFarmer();

        // Create a fake EventCard.
        // The 5th parameter (2) is auraPerCharacter, which EventFarmer uses as multiplier for the penalty.
        mockEventCard = new EventCard(1, false, new ArrayList<>(), 0, 2, 0, 0, 0, 0, 0);
    }

    // Helper method to quickly create characters fittizi
    private CharacterCard createCharacter(JobType type) {
        return new CharacterCard(1, type, 0, Phases.ENDGAME, new ArrayList<>(), 1);
    }

    @Test
    public void testTribuMista_ContaTuttoScontaSoloFarmer() {
        // Add 4 different characters: 1 Farmer, 1 Hunter, 1 Artist, 1 Constructor
        player.modifyMyTribe(createCharacter(JobType.FARMER));
        player.modifyMyTribe(createCharacter(JobType.HUNTER));
        player.modifyMyTribe(createCharacter(JobType.ARTIST));
        player.modifyMyTribe(createCharacter(JobType.CONSTRUCTOR));

        /* MATEMATICA ATTESA:
           - Cibo iniziale: 10
           - Costo base: 4 personaggi = 4 cibo
           - Sconto Farmer: 1 Farmer * 3 = 3 cibo
           - Costo Finale: 4 - 3 = 1 cibo da pagare
           - Cibo rimanente: 10 - 1 = 9
        */

        eventFarmer.applyOnEvent(player, game, mockEventCard, mockEventCard);

        assertEquals(9, player.getFood(), "Il cibo dovrebbe essere sceso a 9 (Costo 4 - Sconto 3 = 1 pagato)");
        assertEquals(20, player.getAuraPoints(), "I punti Aura non dovrebbero essere intaccati");
    }

    @Test
    public void testSoloContadini_NessunCosto() {
        // Add 2 Farmer
        player.modifyMyTribe(createCharacter(JobType.FARMER));
        player.modifyMyTribe(createCharacter(JobType.FARMER));

        /* MATEMATICA ATTESA:
           - Costo base: 2 personaggi = 2 cibo
           - Sconto Farmer: 2 Farmer * 3 = 6 cibo
           - Costo Finale: 2 - 6 = -4 -> arrotondato a 0
           - Cibo rimanente: 10 (nessun pagamento)
        */

        eventFarmer.applyOnEvent(player, game, mockEventCard, mockEventCard);

        assertEquals(10, player.getFood(), "Il cibo dovrebbe restare 10 perché lo sconto copre l'intero costo");
    }

    @Test
    public void testMalusAura_CiboInsufficiente() {
        // Reset il cibo a 1
        player.modifyFood(-9); // Ora ha 1 cibo

        // Add 3 characters (No Farmer)
        player.modifyMyTribe(createCharacter(JobType.HUNTER));
        player.modifyMyTribe(createCharacter(JobType.ARTIST));
        player.modifyMyTribe(createCharacter(JobType.CONSTRUCTOR));

        /* MATEMATICA ATTESA:
           - Costo base: 3 cibo
           - Sconto Farmer: 0
           - Costo Finale: 3 cibo
           - Risorse: Ha solo 1 cibo. Ne mancano 2.
           - Cibo rimanente: 0.
           - Malus Aura: 2 cibo mancante * 2 (auraPenalty) = -4 Aura.
           - Aura rimanente: 20 - 4 = 16.
        */

        eventFarmer.applyOnEvent(player, game, mockEventCard, mockEventCard);

        assertEquals(0, player.getFood(), "Il giocatore dovrebbe aver speso tutto il suo cibo (1)");
        assertEquals(16, player.getAuraPoints(), "Dovrebbe aver perso 4 punti Aura per i 2 cibi mancanti");
    }

    @Test
    public void testScontoEdifici() {
        // Add 2 characters (No Farmer) -> Cost = 2
        player.modifyMyTribe(createCharacter(JobType.HUNTER));
        player.modifyMyTribe(createCharacter(JobType.ARTIST));

        // Simulate a building that reduces event cost by 1
        // L'edificio ha impostato eventCostFood a -1
        player.setEventCostFood(-1);

        /* MATEMATICA ATTESA:
           - Costo base: 2 cibo
           - Sconto Farmer: 0
           - Sconto Edificio: |-1| = 1
           - Costo Finale: 2 - 1 = 1 cibo da pagare.
           - Cibo rimanente: 10 - 1 = 9.
        */

        eventFarmer.applyOnEvent(player, game, mockEventCard, mockEventCard);

        assertEquals(9, player.getFood(), "Il costo dovrebbe essere scontato di 1 grazie all'edificio");
        assertEquals(0, player.getEventCostFood(), "I modificatori temporanei devono essere resettati a fine evento");
    }
}