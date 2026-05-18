package it.polimi.ingsw.am54.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DeckFactoryTest {

    // I percorsi standard definiti nel tuo progetto
    private final String BUILDINGS_PATH = "data/BuildingCardsSetup.json";
    private final String CHARACTERS_PATH = "data/CharacterCardsSetup.json";
    private final String EVENTS_PATH = "data/EventCardsSetup.json";
    private final String ERA_COUNTS_PATH = "data/EraCounts.json";

    @BeforeEach
    public void setUp() {
        // PRE-CONDIZIONE: Prima ancora di testare il codice, ci assicuriamo che i file esistano fisicamente.
        // Se uno di questi fallisce, significa che la cartella "data" non si trova dove dovrebbe!
        assertTrue(new File(ERA_COUNTS_PATH).exists(), "File mancante: " + ERA_COUNTS_PATH);
        assertTrue(new File(BUILDINGS_PATH).exists(), "File mancante: " + BUILDINGS_PATH);
        assertTrue(new File(CHARACTERS_PATH).exists(), "File mancante: " + CHARACTERS_PATH);
        assertTrue(new File(EVENTS_PATH).exists(), "File mancante: " + EVENTS_PATH);
    }

    // =========================================================================
    // 1. TEST GESTIONE ERRORI (ECCEZIONI)
    // =========================================================================

    @Test
    public void testInvalidPlayerCount_ThrowsException() {
        // Mesos si gioca da 2 a 5 giocatori. Testiamo i limiti (1 e 6).
        assertThrows(IllegalArgumentException.class, () -> {
            DeckFactory.createBuildingDeck(BUILDINGS_PATH, 1);
        }, "Con 1 giocatore deve lanciare eccezione");

        assertThrows(IllegalArgumentException.class, () -> {
            DeckFactory.createTribeDeck(CHARACTERS_PATH, EVENTS_PATH, 6);
        }, "Con 6 giocatori deve lanciare eccezione");
    }

    // =========================================================================
    // 2. TEST MAZZO EDIFICI (Integrità e Ordinamento)
    // =========================================================================

    @Test
    public void testCreateBuildingDeck_OrderingAndIntegrity() {
        int numPlayers = 3;
        Deck<BuildingCard> deck = DeckFactory.createBuildingDeck(BUILDINGS_PATH, numPlayers);

        assertNotNull(deck, "Il mazzo non deve essere nullo");
        assertFalse(deck.isEmpty(), "Il mazzo non deve essere vuoto dopo la creazione");

        // Verifichiamo l'ordinamento delle Ere (LIFO: Era 1 in cima, Era 3 in fondo)
        int currentEra = 1;
        while (!deck.isEmpty()) {
            BuildingCard card = deck.drawCard();

            // L'era della carta appena pescata deve essere MAGGIORE O UGUALE a quella di prima.
            // Es: Se pesco Era 2, non posso più pescare un'Era 1!
            assertTrue(card.getEra() >= currentEra,
                    "Errore di ordinamento: trovata Era " + card.getEra() + " sotto a un'Era " + currentEra);

            currentEra = card.getEra();
        }

        // Se il ciclo finisce, significa che l'ultimo currentEra è 3.
        assertEquals(3, currentEra, "Il mazzo deve finire con le carte di Era 3");
    }

    // =========================================================================
    // 3. TEST MAZZO TRIBU' (Personaggi, Eventi e Ordinamento)
    // =========================================================================

    @Test
    public void testCreateTribeDeck_OrderingAndFinalEvents() {
        int numPlayers = 4;
        Deck<TribeCard> deck = DeckFactory.createTribeDeck(CHARACTERS_PATH, EVENTS_PATH, numPlayers);

        assertNotNull(deck, "Il mazzo Tribù non deve essere nullo");
        assertFalse(deck.isEmpty(), "Il mazzo Tribù non deve essere vuoto");

        int currentEra = 1;
        boolean finalEventFound = false;

        while (!deck.isEmpty()) {
            TribeCard card = deck.drawCard();

            // Riconosciamo se la carta pescata è un Evento
            if (card instanceof EventCard) {
                EventCard event = (EventCard) card;

                if (event.isFinalEvent()) { // NOTA: assicurati di avere il getter isFinalEvent() o simile
                    finalEventFound = true;
                } else {
                    // Regola d'oro: non puoi mai pescare un evento normale DOPO aver trovato un evento finale!
                    assertFalse(finalEventFound, "Trovato un evento normale SOTTO un Evento Finale!");
                }
            }

            // Per le carte normali, l'era deve essere strettamente crescente o uguale
            if (!finalEventFound) {
                assertTrue(card.getEra() >= currentEra,
                        "Errore di ordinamento: trovata Era " + card.getEra() + " sotto a un'Era " + currentEra);
                currentEra = card.getEra();
            }
        }

        // Il manuale dice che sul fondo devono esserci i 2 Eventi Finali.
        // Se il ciclo svuota il mazzo, l'ultima o le ultime due carte dovevano essere finali.
        assertTrue(finalEventFound, "Non sono stati trovati gli Eventi Finali in fondo al mazzo!");
    }

    // =========================================================================
    // 4. TEST ECCEZIONI: FILE MANCANTI (CATCH BLOCKS)
    // =========================================================================

    @Test
    public void testFileNonTrovato_LanciaIllegalStateException() {
        // Forza l'ingresso nel catch: "Errore critico durante la generazione delle carte dal file..."
        Exception e1 = assertThrows(IllegalStateException.class, () -> {
            DeckFactory.createBuildingDeck("percorso/finto/edifici.json", 3);
        });
        assertTrue(e1.getMessage().contains("percorso/finto/edifici.json"), "Il messaggio deve contenere il percorso errato");

        // Forza l'ingresso nel catch: "Errore critico durante la lettura del file dei Personaggi..."
        Exception e2 = assertThrows(IllegalStateException.class, () -> {
            DeckFactory.createTribeDeck("percorso/finto/personaggi.json", EVENTS_PATH, 3);
        });
        assertTrue(e2.getMessage().contains("percorso/finto/personaggi.json"), "Il messaggio deve contenere il percorso errato");
    }

    // =========================================================================
    // 5. TEST ECCEZIONI: JSON CORROTTO (SWITCH DEFAULT)
    // =========================================================================

    @Test
    public void testEffettiSconosciuti_LancianoEccezione() throws IOException {
        // Creiamo file temporanei con JSON malevolo per far scattare i "default" degli switch

        // --- 1. Edificio con effetto inesistente ---
        File badBuildingFile = File.createTempFile("badBuilding", ".json");
        badBuildingFile.deleteOnExit(); // Si cancella da solo a fine test
        try (FileWriter w = new FileWriter(badBuildingFile)) {
            w.write("[{\"era\": 1, \"type\": \"FARM\", \"cost\": 1, \"finalAura\": 1, \"phase\": \"BUILDING\", \"effects\": [{\"name\": \"EFFETTO_INVENTATO\"}]}]");
        }

        // Lo switch lancia IllegalArgumentException, ma il catch esterno della Factory lo cattura e rilancia come IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            DeckFactory.createBuildingDeck(badBuildingFile.getAbsolutePath(), 3);
        }, "Un effetto edificio inesistente deve far fallire la generazione");

        // --- 2. Personaggio con effetto inesistente ---
        File badCharFile = File.createTempFile("badChar", ".json");
        badCharFile.deleteOnExit();
        try (FileWriter w = new FileWriter(badCharFile)) {
            w.write("[{\"era\": 1, \"type\": \"FARMER\", \"level\": 1, \"aura\": 0, \"phase\": \"TRIBE\", \"effects\": [{\"name\": \"EFFETTO_INVENTATO\"}]}]");
        }

        assertThrows(IllegalStateException.class, () -> {
            DeckFactory.createTribeDeck(badCharFile.getAbsolutePath(), EVENTS_PATH, 3);
        }, "Un effetto personaggio inesistente deve far fallire la generazione");

        // --- 3. Evento con effetto inesistente ---
        File badEventFile = File.createTempFile("badEvent", ".json");
        badEventFile.deleteOnExit();
        try (FileWriter w = new FileWriter(badEventFile)) {
            w.write("[{\"era\": 1, \"finalEvent\": false, \"baseFood\": 0, \"auraPerCharacter\": 0, \"threshold\": 0, \"bonusMultiplier\": 0, \"penalty\": 0, \"bonus\": 0, \"malus\": 0, \"effects\": [{\"name\": \"EFFETTO_INVENTATO\"}]}]");
        }

        assertThrows(IllegalStateException.class, () -> {
            DeckFactory.createTribeDeck(CHARACTERS_PATH, badEventFile.getAbsolutePath(), 3);
        }, "Un effetto evento inesistente deve far fallire la generazione");
    }
}