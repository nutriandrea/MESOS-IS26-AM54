package model;

import it.polimi.ingsw.am54.model.BuildingCard;
import it.polimi.ingsw.am54.model.Deck;
import it.polimi.ingsw.am54.model.DeckFactory;
import it.polimi.ingsw.am54.model.TribeCard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeckFactoryTest {

    @Test
    void testCreateBuildingDeck_InvalidPlayerCount_LessThanTwo() {

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", 1);
        });


        assertTrue(ex.getMessage().contains("1"));
    }

    @Test
    void testCreateBuildingDeck_InvalidPlayerCount_MoreThanFive() {

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", 6);
        });

        assertTrue(ex.getMessage().contains("6"));
    }

    @Test
    void testCreateTribeDeck_InvalidPlayerCount_LessThanTwo() {

        // prende l'eccezione
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {

            // Eseguiamo il metodo passandogli '1' come numero di giocatori (che non è valido)
            DeckFactory.createTribeDeck(
                    "data/CharacterCardsSetup.json",
                    "data/EventCardsSetup.json",
                    1
            );

        });

        // Ora verifichiamo che il messaggio di errore contenga il numero errato passato ("1").
        assertTrue(ex.getMessage().contains("1"));
    }

    @Test
    void testCreateTribeDeck_InvalidPlayerCount_MoreThanFive() {
        // prende l'eccezione
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {

            // Eseguiamo il metodo passandogli '6' come numero di giocatori (che non è valido)
            DeckFactory.createTribeDeck(
                    "data/CharacterCardsSetup.json",
                    "data/EventCardsSetup.json",
                    6
            );

        });

        // Ora verifichiamo che il messaggio di errore contenga il numero errato passato ("1").
        assertTrue(ex.getMessage().contains("6"));
    }

    @Test
    void testCreateBuildingDeck_ValidInput() {
        Deck<BuildingCard> deck = DeckFactory.createBuildingDeck("data/BuildingCardsSetup.json", 2);
        assertNotNull(deck);
    }

    @Test
    void testCreateTribeDeck_ValidInput() {
        Deck<TribeCard> deck = DeckFactory.createTribeDeck(
            "data/CharacterCardsSetup.json", 
            "data/EventCardsSetup.json", 
            2
        );
        assertNotNull(deck);
    }

    @Test
    void testCreateBuildingDeck_InvalidFilePath() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            DeckFactory.createBuildingDeck("invalid/path.json", 2);
        });

        //verifichiamo
        assertTrue(ex.getMessage().contains("invalid/path.json"));
    }

    @Test
    void testCreateTribeDeck_InvalidFilePath() {
        // aspettiamo l'eccezione del file mancante
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            DeckFactory.createTribeDeck(
                    "invalid/char.json",
                    "invalid/event.json",
                    2
            );
        });

        //verifica che il messaggio sia corretto
        assertTrue(ex.getMessage().contains("Errore critico durante la lettura"));
    }
}
